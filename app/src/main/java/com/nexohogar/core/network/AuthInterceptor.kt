package com.nexohogar.core.network

import android.util.Log
import com.google.gson.Gson
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.mapper.toDomain
import com.nexohogar.data.remote.dto.LoginResponse
import com.nexohogar.domain.model.UserSession
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.TimeUnit

/**
 * Interceptor encargado de:
 * 1. Inyectar los headers de Supabase y el JWT en cada petición.
 * 2. Verificar PROACTIVAMENTE si el token está por expirar y refrescarlo
 *    ANTES de enviar la request.
 * 3. Si la respuesta es 401, refrescar el token y reintentar (fallback reactivo).
 * 4. Si el refresh falla POR EL SERVIDOR (token revocado/expirado), limpiar
 *    la sesión para forzar re-login.
 *
 * FIX-SESSION-05: Resultado sellado (RefreshResult) para distinguir entre
 *   éxito, rechazo del servidor y error de red. Solo clearSession() en rechazo.
 *
 * FIX-SESSION-06: Retry con exponential backoff en tryRefreshToken().
 *   3 intentos: 0s → 1s → 2s. Evita logout por error de red transitorio.
 *
 * FIX-SESSION-07: refreshClient tiene timeouts explícitos (15s connect, 15s read)
 *   para evitar bloqueos indefinidos en el refresh.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
        private const val MAX_REFRESH_RETRIES = 3
        private val RETRY_DELAYS_MS = longArrayOf(0, 1000, 2000)
    }

    // ── Resultado sellado para el refresh ────────────────────────────────
    sealed class RefreshResult {
        data class Success(val accessToken: String) : RefreshResult()
        object ServerRejected : RefreshResult()
        data class NetworkError(val message: String) : RefreshResult()
    }

    /** Cliente HTTP separado SIN AuthInterceptor para evitar bucle infinito. */
    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()
    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // ── 1. Proactive refresh ──────────────────────────────────────────
        if (sessionManager.isTokenExpired()) {
            Log.d(TAG, "⏰ Token expirado/por expirar → proactive refresh")
            synchronized(refreshLock) {
                if (sessionManager.isTokenExpired()) {
                    val result = tryRefreshTokenWithRetry()
                    when (result) {
                        is RefreshResult.Success ->
                            Log.d(TAG, "⏰ Proactive refresh: ✅ OK")
                        is RefreshResult.ServerRejected ->
                            Log.d(TAG, "⏰ Proactive refresh: ❌ Servidor rechazó refresh_token")
                        is RefreshResult.NetworkError ->
                            Log.d(TAG, "⏰ Proactive refresh: ⚠️ Error de red (${result.message})")
                    }
                } else {
                    Log.d(TAG, "⏰ Otro hilo ya refrescó el token")
                }
            }
        }

        // ── 2. Enviar request con el token actual ─────────────────────────
        val tokenUsedForRequest = sessionManager.fetchAuthToken()
        val response = chain.proceed(originalRequest.withAuthHeaders(tokenUsedForRequest))

        // ── 3. Reactive refresh en caso de 401 ───────────────────────────
        if (response.code == 401) {
            Log.d(TAG, "🔴 Server respondió 401 para ${originalRequest.url.encodedPath}")
            response.close()

            val reactiveResult = synchronized(refreshLock) {
                val currentToken = sessionManager.fetchAuthToken()
                if (currentToken != tokenUsedForRequest && currentToken != null) {
                    Log.d(TAG, "🔄 Token ya fue refrescado por otro hilo")
                    RefreshResult.Success(currentToken)
                } else {
                    Log.d(TAG, "🔄 Intentando refresh reactivo...")
                    tryRefreshTokenWithRetry()
                }
            }

            return when (reactiveResult) {
                is RefreshResult.Success -> {
                    Log.d(TAG, "🟢 Reintentando request con token nuevo")
                    chain.proceed(originalRequest.withAuthHeaders(reactiveResult.accessToken))
                }
                is RefreshResult.ServerRejected -> {
                    Log.d(TAG, "🔴 Refresh rechazado → clearSession → login")
                    sessionManager.clearSession()
                    syntheticUnauthorizedResponse(originalRequest)
                }
                is RefreshResult.NetworkError -> {
                    // NO limpiar sesión — el refresh_token sigue válido
                    Log.d(TAG, "⚠️ Red no disponible — 401 sin limpiar sesión")
                    syntheticUnauthorizedResponse(originalRequest)
                }
            }
        }

        return response
    }

    // ───────────────────────────────────────────────────────────────────────

    private fun Request.withAuthHeaders(token: String?): Request {
        val builder = newBuilder()
            .header("apikey", SupabaseConfig.API_KEY)
            .header("Content-Type", "application/json")
        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }
        return builder.build()
    }

    /**
     * FIX-SESSION-06: Retry con exponential backoff.
     */
    private fun tryRefreshTokenWithRetry(): RefreshResult {
        var lastResult: RefreshResult = RefreshResult.NetworkError("No se intentó")

        for (attempt in 0 until MAX_REFRESH_RETRIES) {
            if (attempt > 0) {
                val delayMs = RETRY_DELAYS_MS.getOrElse(attempt) { 2000L }
                Log.d(TAG, "🔄 Retry #$attempt tras ${delayMs}ms...")
                try { Thread.sleep(delayMs) } catch (_: InterruptedException) { break }
            }

            lastResult = tryRefreshToken()
            when (lastResult) {
                is RefreshResult.Success -> return lastResult
                is RefreshResult.ServerRejected -> return lastResult // no tiene sentido reintentar
                is RefreshResult.NetworkError -> continue
            }
        }

        Log.d(TAG, "❌ Refresh falló tras $MAX_REFRESH_RETRIES intentos: $lastResult")
        return lastResult
    }

    /**
     * Un solo intento de refresh. Retorna RefreshResult sellado.
     */
    private fun tryRefreshToken(): RefreshResult {
        val refreshToken = sessionManager.fetchRefreshToken()
        if (refreshToken == null) {
            Log.d(TAG, "❌ No hay refresh_token almacenado")
            return RefreshResult.ServerRejected
        }

        Log.d(TAG, "🔑 Enviando refresh_token (${refreshToken.take(8)}...)")

        return try {
            val bodyJson = gson.toJson(mapOf("refresh_token" to refreshToken))
            val body = bodyJson.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}auth/v1/token?grant_type=refresh_token")
                .post(body)
                .header("apikey", SupabaseConfig.API_KEY)
                .header("Content-Type", "application/json")
                .build()

            val response = refreshClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Log.d(TAG, "❌ Refresh response body es null")
                    return RefreshResult.ServerRejected
                }
                val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
                var newSession = loginResponse.toDomain()

                // Fallback: user object puede no venir en respuesta de refresh
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
                        Log.d(TAG, "🔄 Fallback con userId/email almacenados")
                    }
                }

                if (newSession == null) {
                    Log.d(TAG, "❌ toDomain() retornó null")
                    return RefreshResult.ServerRejected
                }

                sessionManager.saveSession(newSession)
                Log.d(TAG, "✅ Token refrescado — expira en ${loginResponse.expiresIn}s")
                RefreshResult.Success(newSession.accessToken)
            } else {
                val errorBody = response.body?.string() ?: "sin body"
                Log.d(TAG, "❌ Servidor rechazó refresh: HTTP ${response.code} — $errorBody")
                RefreshResult.ServerRejected
            }
        } catch (e: Exception) {
            Log.d(TAG, "⚠️ Error de red en refresh: ${e.message}")
            RefreshResult.NetworkError(e.message ?: "Unknown")
        }
    }

    private fun syntheticUnauthorizedResponse(request: Request): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized - session expired")
            .body("".toResponseBody("application/json".toMediaType()))
            .build()
}
