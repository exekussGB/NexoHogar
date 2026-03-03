package com.nexohogar.data.repository

import com.nexohogar.data.model.HouseholdResponse
import com.nexohogar.data.model.LoginRequest
import com.nexohogar.data.model.LoginResponse
import com.nexohogar.data.network.AuthApi
import retrofit2.Response

/**
 * Repositorio para gestionar las operaciones de autenticación y households con Supabase.
 * Los headers comunes (apikey y Authorization) se gestionan mediante un Interceptor.
 */
class AuthRepository(private val api: AuthApi) {

    suspend fun login(request: LoginRequest): Response<LoginResponse> {
        // La apiKey se inyecta automáticamente vía Interceptor, solo enviamos el body
        return api.login(request)
    }

    suspend fun getHouseholds(): Response<List<HouseholdResponse>> {
        // El token y la apiKey se inyectan automáticamente vía Interceptor
        return api.getHouseholds()
    }
}
