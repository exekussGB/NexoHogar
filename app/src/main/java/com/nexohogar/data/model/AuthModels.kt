package com.nexohogar.data.model

import com.google.gson.annotations.SerializedName

/**
 * DTOs (Data Transfer Objects) para la comunicación con la API de Supabase.
 * Estas clases solo deben existir en la capa de DATA.
 */

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String?,

    @SerializedName("refresh_token")
    val refreshToken: String?,

    @SerializedName("expires_in")
    val expiresIn: Long?,

    @SerializedName("token_type")
    val tokenType: String?,

    val user: UserResponse?
)

data class UserResponse(
    val id: String?,
    val email: String?
)
