package com.nexohogar.data.network

import com.google.gson.JsonObject
import com.nexohogar.data.remote.dto.HouseholdResponse
import com.nexohogar.data.remote.dto.LoginRequest
import com.nexohogar.data.remote.dto.LoginResponse
import com.nexohogar.data.remote.dto.CreateHouseholdRequest
import com.nexohogar.data.remote.dto.CreateHouseholdResponse
import com.nexohogar.data.remote.dto.HouseholdMemberWithEmailDto
import com.nexohogar.data.remote.dto.InviteCodeRequest
import com.nexohogar.data.remote.dto.JoinHouseholdRequest
import com.nexohogar.data.remote.dto.RegisterRequest
import com.nexohogar.data.remote.dto.UpdatePasswordRequest
import com.nexohogar.data.remote.dto.VerifyOtpRequest
import com.nexohogar.data.remote.dto.VerifyOtpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
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

    // ── Recuperación de contraseña ────────────────────────────────────

    @POST("auth/v1/recover")
    suspend fun forgotPassword(
        @Body request: Map<String, String>
    ): Response<Unit>

    @PUT("auth/v1/user")
    suspend fun updatePassword(
        @Header("Authorization") token: String,
        @Body request: UpdatePasswordRequest
    ): Response<Unit>

    @POST("auth/v1/verify")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): Response<VerifyOtpResponse>

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

    // ── NUEVO: Siempre genera un código nuevo ───────────────────────────
    @POST("rest/v1/rpc/regenerate_invite_code")
    suspend fun regenerateInviteCode(
        @Body request: InviteCodeRequest
    ): Response<String>

    /**
     * FIX: Retorna JsonObject (antes Unit) porque la función SQL
     * retorna json_build_object con success/message.
     */
    @POST("rest/v1/rpc/join_household_by_code")
    suspend fun joinHouseholdByCode(
        @Body request: JoinHouseholdRequest
    ): Response<JsonObject>

    @POST("rest/v1/rpc/get_members_with_email")
    suspend fun getHouseholdMembersWithEmail(
        @Body request: Map<String, String>
    ): Response<List<HouseholdMemberWithEmailDto>>

    // ── Aceptar / Rechazar miembros ─────────────────────────────────────

    @POST("rest/v1/rpc/rpc_accept_member")
    suspend fun acceptMember(
        @Body request: Map<String, String>
    ): Response<JsonObject>

    @POST("rest/v1/rpc/rpc_reject_member")
    suspend fun rejectMember(
        @Body request: Map<String, String>
    ): Response<JsonObject>

    @POST("rest/v1/rpc/delete_household")
    suspend fun deleteHousehold(
        @Body params: Map<String, String>
    ): Response<Void>
}
