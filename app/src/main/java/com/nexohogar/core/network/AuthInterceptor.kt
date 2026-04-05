package com.nexohogar.core.network

import android.util.Log
import com.nexohogar.data.local.SessionManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * OkHttp interceptor que:
 * 1. Inyecta las cabeceras de Supabase y el JWT actual en cada request.
 * 2. Obtiene el token SIEMPRE del SDK de Supabase (supabase-kt), que lo
 *    mantiene fresco gracias a [alwaysAutoRefresh = true]. Eliminamos la
 *    lógica manual de expiración/refresco que causaba el cierre de sesión.
 * 3. En caso de 401, reintenta una vez con el token más reciente.
 */
class AuthInterceptor(
    private val sessionManager: SessionManager,
    private val supabaseClient: SupabaseClient
) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // ── 1. Obtener token fresco del SDK (supabase-kt maneja el refresh) ──
        val token = supabaseClient.auth.currentSessionOrNull()?.accessToken
            ?: sessionManager.fetchAuthToken() // fallback por si el SDK aún no inicializó

        val response = chain.proceed(originalRequest.withAuthHeaders(token))

        // ── 2. Reintento reactivo en 401 (el SDK puede haber refrescado mientras) ──
        if (response.code == 401) {
            Log.d(TAG, "🔴 Server respondió 401 — reintentando con token actualizado")
            response.close()

            val freshToken = supabaseClient.auth.currentSessionOrNull()?.accessToken
                ?: sessionManager.fetchAuthToken()

            val retryResponse = chain.proceed(originalRequest.withAuthHeaders(freshToken))

            if (retryResponse.code == 401) {
                Log.w(TAG, "🔴 Segundo 401 consecutivo — posible sesión expirada")
                // No limpiamos la sesión aquí; el Splash screen detectará el estado
                // via supabaseClient.auth.sessionStatus al próximo inicio.
            }

            return retryResponse
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

    @Suppress("unused")
    private fun syntheticResponse(request: Request, code: Int, message: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body("".toResponseBody("application/json".toMediaType()))
            .build()
}
