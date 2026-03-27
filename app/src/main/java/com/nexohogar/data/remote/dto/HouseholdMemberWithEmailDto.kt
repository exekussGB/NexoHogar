package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.HouseholdMember

/**
 * DTO para la respuesta de la función RPC get_members_with_email.
 * Incluye el correo, nombre y status del miembro.
 */
data class HouseholdMemberWithEmailDto(
    @SerializedName("user_id")      val userId: String,
    @SerializedName("role")         val role: String?,
    @SerializedName("joined_at")    val joinedAt: String?,
    @SerializedName("email")        val email: String?,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("status")       val status: String? = "active"
) {
    fun toDomain() = HouseholdMember(
        userId      = userId,
        role        = role ?: "user",
        joinedAt    = joinedAt,
        email       = email,
        displayName = displayName,
        status      = status ?: "active"
    )
}
