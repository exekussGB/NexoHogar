package com.nexohogar.core.session

import android.util.Log
import com.google.gson.Gson
import com.nexohogar.core.network.SupabaseConfig
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.mapper.toDomain
import com.nexohogar.data.remote.dto.LoginResponse
import com.nexohogar.domain.model.UserSession
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Single source of truth for Supabase token refresh.
 *
 * Both [com.nexohogar.core.network.AuthInterceptor] (OkHttp thread) and
 * [SessionRefresher] (coroutine / lifecycle) MUST call [refresh] instead of
 * performing their own HTTP refresh requests. This eliminates the race
 * condition where two components consume the same refresh_token concurrently,
 * causing Supabase to revoke the entire token family.
 *
 * Thread-safety: all state is guarded by [lock].
 */
object TokenRefreshCoordinator {

    private const val TAG = "TokenRefreshCoordinator"

    /** Prevent concurrent refresh attempts. */
    private val lock = Any()

    /** Timestamp of the last successful refresh (epoch ms). */
    @Volatile
    private var lastSuccessMs = 0L

    /** Don't attempt a new refresh if one succeeded less than 5 s ago. */
    private const val DEBOUNCE_MS = 5_000L

    /** Maximum retry attempts for transient network errors. */
    private const val MAX_RETRIES = 3

    /** Delays between retries (index = attempt - 1). */
    private val RETRY_DELAYS = longArrayOf(0, 1_000, 2_000)

    /** Dedicated HTTP client WITHOUT AuthInterceptor to avoid infinite loops. */
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val gson: Gson by lazy { Gson() }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Ensures the access token stored in [sessionManager] is fresh.
     *
     * Safe to call from any thread. Concurrent callers will block on [lock]
     * and the second caller will see the already-refreshed token (double-check).
     */
    fun refresh(sessionManager: SessionManager): RefreshResult = synchronized(lock) {
        // Double-check: another thread may have refreshed while we waited.
        if (!sessionManager.isTokenExpired()) {
            return@synchronized RefreshResult.AlreadyFresh
        }

        // Debounce: avoid hammering Supabase if we just succeeded.
        val now = System.currentTimeMillis()
        if (now - lastSuccessMs < DEBOUNCE_MS && !sessionManager.isTokenExpired()) {
            Log.d(TAG, "⏭️ Debounce — refresh succeeded ${(now - lastSuccessMs) / 1000}s ago")
            return@synchronized RefreshResult.AlreadyFresh
        }

        // Retry loop
        var lastResult: RefreshResult = RefreshResult.NetworkError("No attempt made")

        for (attempt in 0 until MAX_RETRIES) {
            if (attempt > 0) {
                val delayMs = RETRY_DELAYS.getOrElse(attempt) { 2_000L }
                Log.d(TAG, "🔄 Retry #$attempt after ${delayMs}ms")
                try {
                    Thread.sleep(delayMs)
                } catch (_: InterruptedException) {
                    break
                }
            }

            lastResult = doSingleRefresh(sessionManager)
            when (lastResult) {
                is RefreshResult.Success -> {
                    lastSuccessMs = System.currentTimeMillis()
                    Log.d(TAG, "✅ Token refreshed on attempt #$attempt")
                    return@synchronized lastResult
                }
                is RefreshResult.ServerRejected -> {
                    Log.d(TAG, "❌ Server rejected refresh — not retrying")
                    return@synchronized lastResult
                }
                is RefreshResult.NetworkError -> {
                    Log.d(TAG, "⚠️ Network error on attempt #$attempt: ${lastResult.message}")
                    continue
                }
                is RefreshResult.AlreadyFresh -> {
                    return@synchronized lastResult
                }
            }
        }

        Log.d(TAG, "❌ Refresh failed after $MAX_RETRIES attempts: $lastResult")
        lastResult
    }

    // ── Internal ────────────────────────────────────────────────────────────

    /**
     * Single HTTP refresh attempt against Supabase auth endpoint.
     */
    private fun doSingleRefresh(sessionManager: SessionManager): RefreshResult {
        val refreshToken = sessionManager.fetchRefreshToken()
        if (refreshToken == null) {
            Log.d(TAG, "❌ No refresh_token stored")
            return RefreshResult.ServerRejected
        }

        Log.d(TAG, "🔑 Sending refresh_token (${refreshToken.take(8)}...)")

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
                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Log.d(TAG, "❌ Refresh response body is null")
                    return RefreshResult.ServerRejected
                }
                val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
                var newSession = loginResponse.toDomain()

                // Fallback: user object may not come in refresh response
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
                        Log.d(TAG, "🔄 Fallback with stored userId/email")
                    }
                }

                if (newSession == null) {
                    Log.d(TAG, "❌ toDomain() returned null")
                    return RefreshResult.ServerRejected
                }

                sessionManager.saveSession(newSession)
                Log.d(TAG, "✅ Token refreshed — expires in ${loginResponse.expiresIn}s")
                RefreshResult.Success(newSession.accessToken)
            } else {
                val errorBody = response.body?.string() ?: "no body"
                Log.d(TAG, "❌ Server rejected refresh: HTTP ${response.code} — $errorBody")
                RefreshResult.ServerRejected
            }
        } catch (e: Exception) {
            Log.d(TAG, "⚠️ Network error during refresh: ${e.message}")
            RefreshResult.NetworkError(e.message ?: "Unknown")
        }
    }
}
