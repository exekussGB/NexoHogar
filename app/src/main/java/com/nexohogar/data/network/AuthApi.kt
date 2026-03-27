package com.nexohogar.data.network

import com.nexohogar.data.model.HouseholdResponse
import com.nexohogar.data.model.LoginRequest
import com.nexohogar.data.model.LoginResponse
import com.nexohogar.data.remote.dto.CreateHouseholdRequest
import com.nexohogar.data.remote.dto.CreateHouseholdResponse
import com.nexohogar.data.remote.dto.HouseholdMemberWithEmailDto
import com.nexohogar.data.remote.dto.InviteCodeRequest
import com.nexohogar.data.remote.dto.JoinHouseholdRequest
import com.nexohogar.data.remote.dto.JoinHouseholdResponse
import com.nexohogar.data.remote.dto.RemoveMemberResponse
import com.nexohogar.data.remote.dto.RegisterRequest
import com.nexohogar.data.model.UpdatePasswordRequest
import retrofit2.http.PUT
import retrofit2.http.Header
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/v1/signup")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @GET("rest/v1/households")
    suspend fun getHouseholds(
        @Query("select") select: String = "*"
    ): Response<List<HouseholdResponse>>

    @POST("rest/v1/rpc/create_household_with_defaults")
    suspend fun createHousehold(
        @Body request: CreateHouseholdRequest
    ): Response<CreateHouseholdResponse>

    @POST("rest/v1/rpc/get_or_create_invite_code")
    suspend fun getOrCreateInviteCode(
        @Body request: InviteCodeRequest
    ): Response<String>

    @POST("rest/v1/rpc/join_household_by_code")
    suspend fun joinHouseholdByCode(
        @Body request: JoinHouseholdRequest
    ): Response<JoinHouseholdResponse>

    @POST("rest/v1/rpc/get_members_with_email")
    suspend fun getHouseholdMembersWithEmail(
        @Body request: Map<String, String>
    ): Response<List<HouseholdMemberWithEmailDto>>

    @POST("rest/v1/rpc/remove_household_member")
    suspend fun removeHouseholdMember(
        @Body request: Map<String, String>
    ): Response<RemoveMemberResponse>

    // ── Solicitudes pendientes ────────────────────────────────────────────
    @POST("rest/v1/rpc/rpc_get_pending_members")
    suspend fun getPendingMembers(
        @Body request: Map<String, String>
    ): Response<List<HouseholdMemberWithEmailDto>>

    @POST("rest/v1/rpc/rpc_accept_member")
    suspend fun acceptMember(
        @Body request: Map<String, String>
    ): Response<Boolean>

    @POST("rest/v1/rpc/rpc_reject_member")
    suspend fun rejectMember(
        @Body request: Map<String, String>
    ): Response<Boolean>

    @POST("auth/v1/recover")
    suspend fun forgotPassword(
        @Body request: Map<String, String>
    ): Response<Unit>

    @PUT("auth/v1/user")
    suspend fun updatePassword(
        @Header("Authorization") token: String,
        @Body request: UpdatePasswordRequest
    ): Response<Unit>
}
