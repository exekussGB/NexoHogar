package com.nexohogar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.nexohogar.domain.model.HouseholdMember

/**
 * DTO para la respuesta de la función RPC get_members_with_email.
 * Incluye el correo y nombre del usuario obtenido desde auth.users.
 *
 * SQL requerido en Supabase (ver instrucciones de migración):
 *   CREATE OR REPLACE FUNCTION get_members_with_email(p_household_id UUID)
 *   RETURNS TABLE(user_id UUID, role TEXT, joined_at TIMESTAMPTZ, email TEXT, display_name TEXT)
 */
data class HouseholdMemberWithEmailDto(
    @SerializedName("user_id")      val userId: String,
    @SerializedName("role")         val role: String?,
    @SerializedName("joined_at")    val joinedAt: String?,
    @SerializedName("email")        val email: String?,
    @SerializedName("display_name") val displayName: String?
) {
    fun toDomain() = HouseholdMember(
        userId      = userId,
        role        = role ?: "member",
        joinedAt    = joinedAt,
        email       = email,
        displayName = displayName
    )
}
