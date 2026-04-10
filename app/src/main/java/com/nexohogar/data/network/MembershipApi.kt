package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.UserUsageDto
import retrofit2.http.Body
import retrofit2.http.POST

// ─── Request bodies ────────────────────────────────────────────────────────────
data class GetUserUsageRequest(
    val p_user_id: String,
    val p_household_id: String
)
data class IsPremiumRequest(val p_household_id: String)

// ─── API interface ─────────────────────────────────────────────────────────────
interface MembershipApi {
    @POST("rpc/get_user_usage")
    suspend fun getUserUsage(@Body body: GetUserUsageRequest): UserUsageDto

    @POST("rpc/is_premium")
    suspend fun isPremium(@Body body: IsPremiumRequest): Boolean
}
