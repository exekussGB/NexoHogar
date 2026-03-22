package com.nexohogar.core.network

import com.google.gson.Gson
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.mapper.toDomain
import com.nexohogar.data.model.LoginResponse
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * Interceptor encargado de:
 * 1. Inyectar automáticamente los headers de Supabase y el token JWT en cada petición.
 * 2. Si la respuesta es 401 (token expirado), intentar renovar el token usando el
 *    refresh_token y reintentar la petición original.
 * 3. Si el refresh falla, limpiar la sesión para forzar re-login en el próximo acceso.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    // Cliente HTTP separado (sin AuthInterceptor) exclusivo para el refresh.
    // Evita la dependencia circular con el OkHttpClient principal.
    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1. Construir la request con el token actual
        val authenticatedRequest = originalRequest.withAuthHeaders(sessionManager.fetchAuthToken())
        val response = chain.proceed(authenticatedRequest)

        // 2. Si recibimos 401, intentar renovar el token
        if (response.code == 401) {
            response.close()

            val newToken = tryRefreshToken()
            if (newToken != null) {
                // 3. Reintentar con el nuevo token
                val retryRequest = originalRequest.withAuthHeaders(newToken)
                return chain.proceed(retryRequest)
            }

            // Refresh falló → limpiar sesión. El próximo acceso a una pantalla
            // protegida detectará que no hay token y redirigirá al login.
            sessionManager.clearSession()

            // Construir una respuesta 401 "vacía" para devolver sin token
            return chain.proceed(originalRequest.withAuthHeaders(null))
        }

        return response
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

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
     * Usa refreshClient (sin el AuthInterceptor) para evitar bucles infinitos.
     * @return el nuevo access_token si el refresh fue exitoso, null en caso contrario.
     */
    private fun tryRefreshToken(): String? {
        val refreshToken = sessionManager.fetchRefreshToken() ?: return null

        return try {
            val bodyJson  = gson.toJson(mapOf("refresh_token" to refreshToken))
            val mediaType = "application/json".toMediaType()
            val body      = bodyJson.toRequestBody(mediaType)

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
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
