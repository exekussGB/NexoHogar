package com.nexohogar.domain.model

/**
 * Modelo de dominio para un miembro del hogar.
 * status: "active", "pending", "rejected"
 */
data class HouseholdMember(
    val userId: String,
    val role: String,           // "super_user" | "user" | "viewer"
    val joinedAt: String?,      // ISO-8601
    val email: String? = null,
    val displayName: String? = null,
    val status: String = "active" // "active" | "pending" | "rejected"
) {
    val id: String get() = userId

    val isPending: Boolean get() = status == "pending"
    val isActive: Boolean get() = status == "active"

    fun label(): String = when {
        !displayName.isNullOrBlank() -> displayName
        !email.isNullOrBlank()       -> email
        else                         -> "Usuario ${userId.take(8).uppercase()}"
    }
}
