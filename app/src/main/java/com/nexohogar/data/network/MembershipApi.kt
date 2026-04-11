package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.UserUsageDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Headers

// ─── Request bodies ────────────────────────────────────────────────────────────
data class GetUserUsageRequest(
    val p_user_id: String,
    val p_household_id: String
)
data class IsPremiumRequest(val p_household_id: String)

// ─── API interface ─────────────────────────────────────────────────────────────
interface MembershipApi {

    @POST("rpc/get_user_usage")
    @Headers("Content-Type: application/json")
    suspend fun getUserUsage(@Body body: GetUserUsageRequest): List<UserUsageDto>

    @POST("rpc/is_premium")
    @Headers("Content-Type: application/json")
    suspend fun isPremium(@Body body: IsPremiumRequest): Boolean
}