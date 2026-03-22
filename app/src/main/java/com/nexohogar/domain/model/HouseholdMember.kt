package com.nexohogar.domain.model

/**
 * Modelo de dominio para un miembro del hogar.
 */
data class HouseholdMember(
    val userId: String,
    val role: String,        // "admin" | "member"
    val joinedAt: String?    // ISO-8601
)
