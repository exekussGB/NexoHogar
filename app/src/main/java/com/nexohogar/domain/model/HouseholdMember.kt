package com.nexohogar.domain.model

/**
 * Modelo de dominio para un miembro del hogar.
 * displayName: nombre o correo del usuario (obtenido via RPC get_members_with_email)
 */
data class HouseholdMember(
    val userId: String,
    val role: String,           // "admin" | "member"
    val joinedAt: String?,      // ISO-8601
    val email: String? = null,
    val displayName: String? = null
) {
    /** Alias para acceso más natural */
    val id: String get() = userId

    /** Texto a mostrar: displayName si existe, email si no, o fragmento del userId */
    fun label(): String = when {
        !displayName.isNullOrBlank() -> displayName
        !email.isNullOrBlank()       -> email
        else                         -> "Usuario ${userId.take(8).uppercase()}"
    }
}
