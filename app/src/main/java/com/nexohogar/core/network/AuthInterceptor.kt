package com.nexohogar.core.network

import com.google.gson.Gson
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.mapper.toDomain
import com.nexohogar.data.remote.dto.LoginResponse
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
 * Bug corregido (race condition):
 * ANTES: sin sincronización. Si N screens hacían requests simultáneamente
 * al expirar el token, todas obtenían 401 y todas intentaban refrescar.
 * La primera tenía éxito; las demás usaban el refresh_token ya rotado
 * (Supabase rotation → token invalidado) → fallo → clearSession() → login forzado.
 *
 * AHORA: [refreshLock] garantiza que solo UNO refresca a la vez.
 * Los demás threads esperan y luego usan el token ya renovado.
 *
 * Bug corregido (401 reactivo incorrecto):
 * ANTES: en el path reactivo se comparaba con isTokenExpired() local.
 * Si el servidor decía 401 pero localmente el token "no estaba expirado"
 * (clock drift o token revocado), se reintentaba con el MISMO token → loop.
 *
 * AHORA: se guarda el token usado en la request y se compara con el token
 * actual. Si otro hilo ya lo refrescó, usamos el nuevo. Si no cambió,
 * intentamos refresh explícitamente.
 *
 * Bug corregido (clearSession en error de red):
 * ANTES: cualquier excepción en tryRefreshToken() devolvía null → clearSession().
 * Si el dispositivo volvía de estar dormido con WiFi reconectando, la sesión
 * se destruía aunque el refresh_token siguiera siendo válido.
 *
 * AHORA: las excepciones de red devuelven el token actual (no null),
 * así el ViewModel muestra error de red y el usuario puede reintentar.
 * Solo se limpia la sesión cuando el SERVIDOR rechaza el refresh (4xx).
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

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
        // Si el token está expirado o a punto de expirar (margen 2 min definido
        // en SessionManager.isTokenExpired()), refresheamos ANTES de enviar.
        if (sessionManager.isTokenExpired()) {
            synchronized(refreshLock) {
                // Double-check: otro hilo puede haber refrescado mientras esperábamos.
                if (sessionManager.isTokenExpired()) {
                    tryRefreshToken()
                }
            }
        }

        // ── 2. Enviar request con el token actual ─────────────────────────────
        // Guardamos el token exacto que usamos para poder compararlo luego.
        val tokenUsedForRequest = sessionManager.fetchAuthToken()
        val response = chain.proceed(originalRequest.withAuthHeaders(tokenUsedForRequest))

        // ── 3. Reactive refresh en caso de 401 inerante (p.ej. token revocado) ─
        if (response.code == 401) {
            response.close()

            val newToken = synchronized(refreshLock) {
                val currentToken = sessionManager.fetchAuthToken()
                if (currentToken != tokenUsedForRequest) {
                    // Otro hilo ya refrescó el token mientras esperábamos el lock.
                    // Usamos el nuevo token sin hacer nada.
                    currentToken
                } else {
                    // El token no cambió desde que enviamos la request fallida:
                    // el servidor nos rechazó este token específico → intentar refresh.
                    tryRefreshToken()
                }
            }

            if (newToken != null) {
                return chain.proceed(originalRequest.withAuthHeaders(newToken))
            }

            // Refresh falló definitivamente (servidor rechazó el refresh_token).
            // El ViewModel recibirá 401 y redirigirá a login.
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
        val refreshToken = sessionManager.fetchRefreshToken() ?: return null

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
                val responseBody = response.body?.string() ?: return null
                val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
                val newSession = loginResponse.toDomain() ?: return null
                sessionManager.saveSession(newSession)
                newSession.accessToken
            } else {
                // El servidor rechazó el refresh_token (expirado/revocado).
                // Devolvemos null para que el caller limpie la sesión.
                null
            }
        } catch (e: Exception) {
            // Error de RED temporal (WiFi reconectando, timeout, etc.).
            // NO limpiar la sesión — el refresh_token sigue siendo válido.
            // Devolvemos el token actual para que el ViewModel muestre
            // error de red y el usuario pueda reintentar más tarde.
            sessionManager.fetchAuthToken()
        }
    }

    /**
     * Construye una respuesta 401 sintética sin necesidad de hacer una
     * petición de red adicional. Evita el bucle infinito que ocurría antes
     * cuando se llamaba [chain.proceed] sin token tras un refresh fallido.
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
