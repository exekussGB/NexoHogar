package com.nexohogar.domain.model

/**
 * Modelo de dominio puro para la sesión.
 * Representa al usuario autenticado independientemente del proveedor (Supabase/Backend).
 */
data class UserSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val expiresAt: Long
)
