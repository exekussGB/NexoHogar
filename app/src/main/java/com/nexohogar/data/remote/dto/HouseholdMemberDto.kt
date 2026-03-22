package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.HouseholdMember

/**
 * DTO para un miembro del hogar proveniente de Supabase.
 * Tabla: household_members
 * Columnas: user_id, household_id, role, joined_at
 */
data class HouseholdMemberDto(
    @SerializedName("user_id")    val userId: String,
    @SerializedName("role")       val role: String?,
    @SerializedName("joined_at")  val joinedAt: String?
) {
    fun toDomain() = HouseholdMember(
        userId   = userId,
        role     = role ?: "member",
        joinedAt = joinedAt
    )
}
