package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.HouseholdMember

data class HouseholdMemberDto(
    @SerializedName("user_id")    val userId: String,
    @SerializedName("role")       val role: String?,
    @SerializedName("joined_at")  val joinedAt: String?,
    @SerializedName("status")     val status: String? = "active"
) {
    fun toDomain() = HouseholdMember(
        userId   = userId,
        role     = role ?: "user",
        joinedAt = joinedAt,
        status   = status ?: "active"
    )
}
