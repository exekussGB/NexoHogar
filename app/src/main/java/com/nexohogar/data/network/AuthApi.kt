package com.nexohogar.data.network

import com.nexohogar.data.model.HouseholdResponse
import com.nexohogar.data.remote.dto.CreateHouseholdRequest
import com.nexohogar.data.remote.dto.CreateHouseholdResponse
import com.nexohogar.data.remote.dto.LoginRequest
import com.nexohogar.data.remote.dto.LoginResponse
import com.nexohogar.data.remote.dto.RegisterRequest
import com.nexohogar.data.remote.dto.RegisterResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @POST("auth/v1/signup")
    suspend fun register(
        @Body request: RegisterRequest
    ): RegisterResponse

    @GET("rest/v1/households")
    suspend fun getHouseholds(
        @Header("Authorization") token: String,
        @Query("select") select: String = "*",
        @Query("household_members.user_id") userId: String? = null
    ): List<HouseholdResponse>

    @POST("rest/v1/rpc/create_household_with_defaults")
    suspend fun createHousehold(
        @Header("Authorization") token: String,
        @Body request: CreateHouseholdRequest
    ): CreateHouseholdResponse
}