package com.nexohogar.core.session

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.nexohogar.data.local.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lifecycle-aware component that keeps the session alive automatically.
 *
 * Responsibilities:
 * 1. On foreground resume: verify and refresh the token if necessary.
 * 2. Proactive timer: schedule refresh ~5 min before the token expires.
 * 3. Emit session-expired events so the UI can react globally.
 *
 * All refresh HTTP logic is delegated to [TokenRefreshCoordinator] to
 * prevent the race condition with [com.nexohogar.core.network.AuthInterceptor].
 *
 * Initialized once from NexoHogarApp.onCreate().
 */
class SessionRefresher(
    private val sessionManager: SessionManager
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "SessionRefresher"

        /** Refresh 5 minutes before expiry. */
        private const val REFRESH_MARGIN_MS = 5 * 60 * 1000L

        /** Minimum time between refresh attempts (spam protection). */
        private const val MIN_REFRESH_INTERVAL_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var refreshTimerJob: Job? = null
    private var lastRefreshAttemptMs = 0L

    // ── Session-expired events (observable by any ViewModel) ─────────────
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

    /**
     * Call from Application.onCreate() to register the observer.
     */
    fun init() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Log.d(TAG, "✅ SessionRefresher initialized with ProcessLifecycleOwner")
        scheduleRefreshTimer()
    }

    // ── Lifecycle callbacks ─────────────────────────────────────────────────

    override fun onStart(owner: LifecycleOwner) {
        // App came to foreground
        Log.d(TAG, "📱 App in foreground — checking session...")
        scope.launch {
            refreshIfNeeded()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        // App went to background — cancel timer (useless in background)
        refreshTimerJob?.cancel()
        Log.d(TAG, "📱 App in background — timer cancelled")
    }

    // ── Core refresh logic ──────────────────────────────────────────────────

    /**
     * Checks whether the token needs a refresh and delegates to
     * [TokenRefreshCoordinator] if it does.
     */
    private suspend fun refreshIfNeeded() {
        val session = sessionManager.fetchSession() ?: return
        val now = System.currentTimeMillis()

        // Anti-spam: skip if we already attempted recently
        if (now - lastRefreshAttemptMs < MIN_REFRESH_INTERVAL_MS) {
            Log.d(TAG, "⏭️ Refresh skipped — last attempt ${(now - lastRefreshAttemptMs) / 1000}s ago")
            scheduleRefreshTimer()
            return
        }

        // If the token is still well within its validity window, just schedule
        val timeUntilExpiry = session.expiresAt - now
        if (timeUntilExpiry > REFRESH_MARGIN_MS) {
            Log.d(TAG, "✅ Token valid — expires in ${timeUntilExpiry / 1000}s, timer scheduled")
            scheduleRefreshTimer()
            return
        }

        // Token expired or about to expire — delegate to coordinator
        Log.d(TAG, "🔄 Token expiring (${timeUntilExpiry / 1000}s remaining) — refreshing...")
        lastRefreshAttemptMs = now

        val result = withContext(Dispatchers.IO) {
            TokenRefreshCoordinator.refresh(sessionManager)
        }

        if (result.isSuccess) {
            Log.d(TAG, "✅ Session renewed via SessionRefresher")
            scheduleRefreshTimer()
        } else if (result is RefreshResult.ServerRejected) {
            Log.d(TAG, "❌ Server rejected refresh — emitting session-expired event")
            _sessionExpiredEvent.tryEmit(Unit)
        } else {
            // NetworkError: don't emit expired, will retry on next foreground
            Log.d(TAG, "⚠️ Network error during refresh — will retry later")
        }
    }

    /**
     * Schedules a timer to refresh BEFORE the token expires.
     */
    private fun scheduleRefreshTimer() {
        refreshTimerJob?.cancel()

        val session = sessionManager.fetchSession() ?: return
        val now = System.currentTimeMillis()
        val timeUntilExpiry = session.expiresAt - now

        if (timeUntilExpiry <= 0) return // already expired, don't schedule

        // Refresh 5 min before expiry, minimum 30 seconds
        val delayMs = maxOf(timeUntilExpiry - REFRESH_MARGIN_MS, 30_000L)

        refreshTimerJob = scope.launch {
            Log.d(TAG, "⏰ Timer scheduled: refresh in ${delayMs / 1000}s")
            delay(delayMs)
            refreshIfNeeded()
        }
    }
}
