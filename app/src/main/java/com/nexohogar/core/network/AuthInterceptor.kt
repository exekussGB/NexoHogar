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

/**
 * Interceptor encargado de:
 * 1. Inyectar los headers de Supabase y el JWT en cada petición.
 * 2. Verificar PROACTIVAMENTE si el token está por expirar y refrescarlo
 *    ANTES de enviar la request (evita el primer fallo tras 1 hora).
 * 3. Si la respuesta es 401, refrescar el token y reintentar (fallback).
 * 4. Si el refresh falla POR EL SERVIDOR (token revocado/expirado), limpiar
 *    la sesión para forzar re-login.
 *
 * Bug corregido v1 (race condition):
 * [refreshLock] garantiza que solo UNO refresca a la vez.
 *
 * Bug corregido v2 (401 reactivo incorrecto):
 * Se guarda el token usado en la request y se compara con el token actual.
 *
 * Bug corregido v3 (clearSession en error de red):
 * Excepciones de red devuelven el token actual (no null).
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
    }

    /** Cliente HTTP separado SIN AuthInterceptor para evitar bucle infinito en refresh. */
    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    private val gson = Gson()

    /**
     * Mutex que serializa todos los intentos de refresh.
     * Solo un hilo puede refrescar a la vez; los demás esperan.
     */
    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // ── 1. Proactive refresh ──────────────────────────────────────────────
        // Si el token está expirado o a punto de expirar (margen 2 min),
        // refrescamos ANTES de enviar.
        val isExpired = sessionManager.isTokenExpired()
        if (isExpired) {
            Log.d(TAG, "⏰ Token expirado/por expirar → proactive refresh")
            synchronized(refreshLock) {
                // Double-check: otro hilo puede haber refrescado mientras esperábamos.
                if (sessionManager.isTokenExpired()) {
                    val result = tryRefreshToken()
                    Log.d(TAG, "⏰ Proactive refresh result: ${if (result != null) "✅ OK" else "❌ FAILED"}")
                } else {
                    Log.d(TAG, "⏰ Otro hilo ya refrescó el token")
                }
            }
        }

        // ── 2. Enviar request con el token actual ─────────────────────────────
        // Guardamos el token exacto que usamos para poder compararlo luego.
        val tokenUsedForRequest = sessionManager.fetchAuthToken()
        val response = chain.proceed(originalRequest.withAuthHeaders(tokenUsedForRequest))

        // ── 3. Reactive refresh en caso de 401 ───────────────────────────────
        if (response.code == 401) {
            Log.d(TAG, "🔴 Server respondió 401 para ${originalRequest.url.encodedPath}")
            response.close()

            val newToken = synchronized(refreshLock) {
                val currentToken = sessionManager.fetchAuthToken()
                if (currentToken != tokenUsedForRequest && currentToken != null) {
                    // Otro hilo ya refrescó el token mientras esperábamos el lock.
                    Log.d(TAG, "🔄 Token ya fue refrescado por otro hilo")
                    currentToken
                } else {
                    // El token no cambió: el servidor rechazó este token → refresh.
                    Log.d(TAG, "🔄 Intentando refresh reactivo...")
                    val result = tryRefreshToken()
                    Log.d(TAG, "🔄 Reactive refresh result: ${if (result != null) "✅ OK" else "❌ FAILED"}")
                    result
                }
            }

            if (newToken != null) {
                Log.d(TAG, "🟢 Reintentando request con token nuevo")
                return chain.proceed(originalRequest.withAuthHeaders(newToken))
            }

            // Refresh falló definitivamente (servidor rechazó el refresh_token).
            Log.d(TAG, "🔴 Refresh falló → clearSession → redirigir a login")
            sessionManager.clearSession()
            return syntheticUnauthorizedResponse(originalRequest)
        }

        return response
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────────────────

    /** Añade los headers de Supabase y el Bearer token a la request. */
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
     * Intenta renovar la sesión usando el refresh_token almacenado.
     * Usa [refreshClient] (sin el AuthInterceptor) para evitar bucles.
     *
     * @return el nuevo access_token si el refresh fue exitoso,
     *         el token actual si hubo error de RED (para no destruir la sesión),
     *         null solo si el SERVIDOR rechazó el refresh_token (sesión inválida).
     */
    private fun tryRefreshToken(): String? {
        val refreshToken = sessionManager.fetchRefreshToken()
        if (refreshToken == null) {
            Log.d(TAG, "❌ No hay refresh_token almacenado")
            return null
        }

        Log.d(TAG, "🔑 Enviando refresh_token a Supabase (${refreshToken.take(8)}...)")

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
                    return null
                }
                val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
                var newSession = loginResponse.toDomain()

                // Fallback: si toDomain() falla (user object ausente en la respuesta de refresh),
                // reconstruimos la sesión con los tokens nuevos + userId/email almacenados.
                // Esto ocurre cuando Supabase no incluye el objeto `user` en el refresh response.
                if (newSession == null && loginResponse.accessToken != null && loginResponse.refreshToken != null) {
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
                        Log.d(TAG, "🔄 Refresh: toDomain() usó fallback con userId/email de sesión almacenada")
                    }
                }

                if (newSession == null) {
                    Log.d(TAG, "❌ toDomain() retornó null — accessToken=${loginResponse.accessToken != null}, refreshToken=${loginResponse.refreshToken != null}, user=${loginResponse.user != null}, userId=${loginResponse.user?.id != null}, email=${loginResponse.user?.email != null}")
                    return null
                }
                sessionManager.saveSession(newSession)
                Log.d(TAG, "✅ Token refrescado OK — expira en ${loginResponse.expiresIn}s, nuevo refresh=${newSession.refreshToken.take(8)}...")
                newSession.accessToken
            } else {
                // El servidor rechazó el refresh_token (expirado/revocado).
                val errorBody = response.body?.string() ?: "sin body"
                Log.d(TAG, "❌ Servidor rechazó refresh: HTTP ${response.code} — $errorBody")
                null
            }
        } catch (e: Exception) {
            // Error de RED temporal (WiFi reconectando, timeout, etc.).
            // NO limpiar la sesión — el refresh_token sigue siendo válido.
            Log.d(TAG, "⚠️ Error de red en refresh (no se limpia sesión): ${e.message}")
            sessionManager.fetchAuthToken()
        }
    }

    /**
     * Construye una respuesta 401 sintética sin necesidad de hacer una
     * petición de red adicional.
     */
    private fun syntheticUnauthorizedResponse(request: Request): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized - session expired")
            .body("".toResponseBody("application/json".toMediaType()))
            .build()
}
