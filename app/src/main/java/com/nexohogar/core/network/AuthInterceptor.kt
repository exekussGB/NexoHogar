package com.nexohogar.core.network

import android.util.Log
import com.nexohogar.core.session.RefreshResult
import com.nexohogar.core.session.TokenRefreshCoordinator
import com.nexohogar.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * OkHttp interceptor that:
 * 1. Injects Supabase headers and the current JWT on every request.
 * 2. Proactively refreshes the token BEFORE sending the request when it is
 *    expired or about to expire.
 * 3. Reactively refreshes the token when the server responds with 401.
 *
 * All refresh logic is delegated to [TokenRefreshCoordinator] to prevent the
 * race condition between this interceptor and [com.nexohogar.core.session.SessionRefresher].
 *
 * Error semantics:
 * - Server rejected refresh → clears session, returns synthetic **401**
 *   with message "Unauthorized - session expired".
 * - Network error during refresh → returns synthetic **503** so that
 *   [com.nexohogar.presentation.household.HouseholdViewModel] does NOT
 *   interpret it as a session-expired event.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // ── 1. Proactive refresh ─────────────────────────────────────────────
        if (sessionManager.isTokenExpired()) {
            Log.d(TAG, "⏰ Token expired/expiring → proactive refresh")
            val result = TokenRefreshCoordinator.refresh(sessionManager)
            when (result) {
                is RefreshResult.Success ->
                    Log.d(TAG, "⏰ Proactive refresh: ✅ OK")
                is RefreshResult.AlreadyFresh ->
                    Log.d(TAG, "⏰ Proactive refresh: ✅ Already fresh")
                is RefreshResult.ServerRejected ->
                    Log.d(TAG, "⏰ Proactive refresh: ❌ Server rejected refresh_token")
                is RefreshResult.NetworkError ->
                    Log.d(TAG, "⏰ Proactive refresh: ⚠️ Network error (${result.message})")
            }
        }

        // ── 2. Send request with the current token ───────────────────────────
        val token = sessionManager.fetchAuthToken()
        val response = chain.proceed(originalRequest.withAuthHeaders(token))

        // ── 3. Reactive refresh on 401 ───────────────────────────────────────
        if (response.code == 401) {
            Log.d(TAG, "🔴 Server responded 401 for ${originalRequest.url.encodedPath}")
            response.close()

            val result = TokenRefreshCoordinator.refresh(sessionManager)

            return when {
                result.isSuccess -> {
                    val newToken = sessionManager.fetchAuthToken()
                    Log.d(TAG, "🟢 Retrying request with refreshed token")
                    chain.proceed(originalRequest.withAuthHeaders(newToken))
                }
                result is RefreshResult.ServerRejected -> {
                    Log.d(TAG, "🔴 Refresh rejected → clearSession → login")
                    sessionManager.clearSession()
                    syntheticResponse(originalRequest, 401, "Unauthorized - session expired")
                }
                else -> {
                    // Network error — do NOT clear session, return 503
                    Log.d(TAG, "⚠️ Network unavailable — returning 503 (session preserved)")
                    syntheticResponse(originalRequest, 503, "Service unavailable - token refresh failed")
                }
            }
        }

        return response
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun Request.withAuthHeaders(token: String?): Request {
        val builder = newBuilder()
            .header("apikey", SupabaseConfig.API_KEY)
            .header("Content-Type", "application/json")
        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }
        return builder.build()
    }

    private fun syntheticResponse(request: Request, code: Int, message: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body("".toResponseBody("application/json".toMediaType()))
            .build()
}
