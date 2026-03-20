package com.nexohogar.data.network

import com.nexohogar.data.model.HouseholdResponse
import com.nexohogar.data.model.LoginRequest
import com.nexohogar.data.model.LoginResponse
import com.nexohogar.data.remote.dto.CreateHouseholdRequest
import com.nexohogar.data.remote.dto.CreateHouseholdResponse
import com.nexohogar.data.remote.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("auth/v1/signup")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<LoginResponse>

    @GET("rest/v1/households")
    suspend fun getHouseholds(
        @Query("select") select: String = "*"
    ): Response<List<HouseholdResponse>>

    @POST("rest/v1/rpc/create_household_with_defaults")
    suspend fun createHousehold(
        @Body request: CreateHouseholdRequest
    ): Response<CreateHouseholdResponse>
}