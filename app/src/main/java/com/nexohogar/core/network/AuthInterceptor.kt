package com.nexohogar.core.network

import com.nexohogar.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor encargado de inyectar automáticamente los headers de Supabase
 * y el token de sesión (JWT) en cada petición saliente.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val requestBuilder = originalRequest.newBuilder()
            .addHeader("apikey", SupabaseConfig.API_KEY)
            .addHeader("Content-Type", "application/json")

        // Si el usuario tiene una sesión activa, inyectamos el Bearer Token
        sessionManager.fetchAuthToken()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
