package com.nexohogar.data.network

import com.nexohogar.data.model.HouseholdResponse
import com.nexohogar.data.model.LoginRequest
import com.nexohogar.data.model.LoginResponse
import com.nexohogar.data.remote.dto.CreateHouseholdRequest
import com.nexohogar.data.remote.dto.CreateHouseholdResponse
import com.nexohogar.data.remote.dto.HouseholdMemberDto
import com.nexohogar.data.remote.dto.InviteCodeRequest
import com.nexohogar.data.remote.dto.JoinHouseholdRequest
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

    /**
     * Obtiene o crea el código de invitación del hogar.
     * Llama a la función SQL: get_or_create_invite_code(p_household_id UUID)
     */
    @POST("rest/v1/rpc/get_or_create_invite_code")
    suspend fun getOrCreateInviteCode(
        @Body request: InviteCodeRequest
    ): Response<String>

    /**
     * Une al usuario autenticado al hogar identificado por el código.
     */
    @POST("rest/v1/rpc/join_household_by_code")
    suspend fun joinHouseholdByCode(
        @Body request: JoinHouseholdRequest
    ): Response<Unit>

    /**
     * Obtiene la lista de miembros de un hogar.
     * Tabla: household_members
     * IMPORTANTE: householdId debe pasarse con prefijo "eq." → "eq.{uuid}"
     */
    @GET("rest/v1/household_members")
    suspend fun getHouseholdMembers(
        @Query("household_id") householdId: String,
        @Query("select")       select: String = "user_id,role,joined_at",
        @Query("order")        order: String = "joined_at.asc"
    ): Response<List<HouseholdMemberDto>>
}
