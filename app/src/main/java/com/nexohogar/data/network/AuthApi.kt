package com.nexohogar.data.network

import com.nexohogar.data.model.HouseholdResponse
import com.nexohogar.data.model.LoginRequest
import com.nexohogar.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Interface de Retrofit para definir los endpoints de autenticación y households de Supabase.
 * Los headers comunes (apikey y Authorization) se gestionan mediante un Interceptor.
 */
interface AuthApi {
    
    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("rest/v1/households")
    suspend fun getHouseholds(): Response<List<HouseholdResponse>>
}
