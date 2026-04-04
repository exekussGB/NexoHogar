package com.nexohogar.core.session

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.gson.Gson
import com.nexohogar.core.network.SupabaseConfig
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.mapper.toDomain
import com.nexohogar.data.remote.dto.LoginResponse
import com.nexohogar.domain.model.UserSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Componente lifecycle-aware que mantiene la sesión activa automáticamente.
 *
 * Responsabilidades:
 * 1. Al volver al foreground: verificar y refrescar el token si es necesario
 * 2. Timer proactivo: programar refresh ~5 min antes de que expire el token
 * 3. Emitir eventos de sesión expirada para que la UI reaccione globalmente
 *
 * Se inicializa una sola vez desde NexoHogarApp.onCreate().
 */
class SessionRefresher(
    private val sessionManager: SessionManager
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "SessionRefresher"

        /** Refrescar 5 minutos antes de que expire. */
        private const val REFRESH_MARGIN_MS = 5 * 60 * 1000L

        /** Mínimo tiempo entre intentos de refresh (evitar spam). */
        private const val MIN_REFRESH_INTERVAL_MS = 30_000L

        /** Máximo de reintentos consecutivos por error de red. */
        private const val MAX_RETRIES = 3

        private val RETRY_DELAYS = longArrayOf(1000, 2000, 4000)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var refreshTimerJob: Job? = null
    private var lastRefreshAttemptMs = 0L

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    // ── Eventos de sesión expirada (observable por cualquier ViewModel) ───
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

    /**
     * Llamar desde Application.onCreate() para registrar el observer.
     */
    fun init() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Log.d(TAG, "✅ SessionRefresher inicializado con ProcessLifecycleOwner")
        scheduleRefreshTimer()
    }

    // ── Lifecycle callbacks ──────────────────────────────────────────────

    override fun onStart(owner: LifecycleOwner) {
        // App volvió al foreground
        Log.d(TAG, "📱 App en foreground — verificando sesión...")
        scope.launch {
            refreshIfNeeded()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        // App fue al background — cancelar timer (no sirve en background)
        refreshTimerJob?.cancel()
        Log.d(TAG, "📱 App en background — timer cancelado")
    }

    // ── Core refresh logic ──────────────────────────────────────────────

    /**
     * Verifica si el token necesita refresh y lo ejecuta si es necesario.
     * Incluye protección anti-spam y retry con backoff.
     */
    private suspend fun refreshIfNeeded() {
        val session = sessionManager.fetchSession() ?: return
        val now = System.currentTimeMillis()

        // Anti-spam: no intentar si ya intentamos hace poco
        if (now - lastRefreshAttemptMs < MIN_REFRESH_INTERVAL_MS) {
            Log.d(TAG, "⏭️ Refresh omitido — último intento hace ${(now - lastRefreshAttemptMs) / 1000}s")
            scheduleRefreshTimer()
            return
        }

        // Si el token aún es válido y le queda más del margen, solo programar timer
        val timeUntilExpiry = session.expiresAt - now
        if (timeUntilExpiry > REFRESH_MARGIN_MS) {
            Log.d(TAG, "✅ Token válido — expira en ${timeUntilExpiry / 1000}s, timer programado")
            scheduleRefreshTimer()
            return
        }

        // Token expirado o por expirar — refrescar
        Log.d(TAG, "🔄 Token por expirar (${timeUntilExpiry / 1000}s restantes) — refrescando...")
        lastRefreshAttemptMs = now

        val success = tryRefreshWithRetry()
        if (success) {
            Log.d(TAG, "✅ Sesión renovada desde SessionRefresher")
            scheduleRefreshTimer()
        } else {
            Log.d(TAG, "❌ No se pudo renovar la sesión")
            // Solo emitir evento si el refresh_token fue rechazado
            // (no en error de red, porque la sesión podría recuperarse)
            if (sessionManager.fetchRefreshToken() == null ||
                sessionManager.fetchAuthToken() == null
            ) {
                _sessionExpiredEvent.tryEmit(Unit)
            }
        }
    }

    /**
     * Programa un timer para refrescar ANTES de que el token expire.
     */
    private fun scheduleRefreshTimer() {
        refreshTimerJob?.cancel()

        val session = sessionManager.fetchSession() ?: return
        val now = System.currentTimeMillis()
        val timeUntilExpiry = session.expiresAt - now

        if (timeUntilExpiry <= 0) return // ya expirado, no programar

        // Refrescar 5 min antes de expirar, mínimo 30 segundos
        val delayMs = maxOf(timeUntilExpiry - REFRESH_MARGIN_MS, 30_000L)

        refreshTimerJob = scope.launch {
            Log.d(TAG, "⏰ Timer programado: refresh en ${delayMs / 1000}s")
            delay(delayMs)
            refreshIfNeeded()
        }
    }

    /**
     * Intenta refrescar con retry y exponential backoff.
     * Retorna true si el refresh fue exitoso.
     */
    private suspend fun tryRefreshWithRetry(): Boolean {
        for (attempt in 0 until MAX_RETRIES) {
            if (attempt > 0) {
                val delayMs = RETRY_DELAYS.getOrElse(attempt) { 4000L }
                Log.d(TAG, "🔄 Retry #$attempt tras ${delayMs}ms")
                delay(delayMs)
            }

            val result = withContext(Dispatchers.IO) { doRefresh() }
            when (result) {
                RefreshOutcome.SUCCESS -> return true
                RefreshOutcome.SERVER_REJECTED -> return false // no reintentar
                RefreshOutcome.NETWORK_ERROR -> continue       // reintentar
            }
        }
        return false
    }

    private enum class RefreshOutcome { SUCCESS, SERVER_REJECTED, NETWORK_ERROR }

    /**
     * Ejecuta la llamada HTTP de refresh. Mismo endpoint que AuthInterceptor
     * pero desde un contexto de coroutine (no desde un interceptor OkHttp).
     */
    private fun doRefresh(): RefreshOutcome {
        val refreshToken = sessionManager.fetchRefreshToken() ?: return RefreshOutcome.SERVER_REJECTED

        return try {
            val bodyJson = gson.toJson(mapOf("refresh_token" to refreshToken))
            val body = bodyJson.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}auth/v1/token?grant_type=refresh_token")
                .post(body)
                .header("apikey", SupabaseConfig.API_KEY)
                .header("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return RefreshOutcome.SERVER_REJECTED
                val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
                var newSession = loginResponse.toDomain()

                // Fallback: user object puede no venir en refresh response
                if (newSession == null &&
                    loginResponse.accessToken != null &&
                    loginResponse.refreshToken != null
                ) {
                    val stored = sessionManager.fetchSession()
                    if (stored != null) {
                        val expSec = loginResponse.expiresIn ?: 3600L
                        newSession = UserSession(
                            accessToken  = loginResponse.accessToken,
                            refreshToken = loginResponse.refreshToken,
                            userId       = loginResponse.user?.id   ?: stored.userId,
                            email        = loginResponse.user?.email ?: stored.email,
                            expiresAt    = System.currentTimeMillis() + (expSec * 1000L)
                        )
                    }
                }

                if (newSession == null) return RefreshOutcome.SERVER_REJECTED

                sessionManager.saveSession(newSession)
                Log.d(TAG, "✅ Token refrescado por SessionRefresher — expira en ${loginResponse.expiresIn}s")
                RefreshOutcome.SUCCESS
            } else {
                val errorBody = response.body?.string() ?: ""
                Log.d(TAG, "❌ Servidor rechazó refresh: HTTP ${response.code} — $errorBody")
                RefreshOutcome.SERVER_REJECTED
            }
        } catch (e: Exception) {
            Log.d(TAG, "⚠️ Error de red en refresh: ${e.message}")
            RefreshOutcome.NETWORK_ERROR
        }
    }
}
