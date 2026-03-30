package com.nexohogar.data.mapper

import com.nexohogar.data.remote.dto.HouseholdResponse
import com.nexohogar.data.remote.dto.LoginResponse
import com.nexohogar.domain.model.Household
import com.nexohogar.domain.model.UserSession

/**
 * Mapeadores de DTO a Dominio para mantener las capas desacopladas.
 */

fun LoginResponse.toDomain(): UserSession? {
    val token = accessToken ?: return null
    val refresh = refreshToken ?: return null
    val userId = user?.id ?: return null
    val email = user.email ?: return null
    
    val expiresInSeconds = expiresIn ?: 3600L
    val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000)

    return UserSession(
        accessToken = token,
        refreshToken = refresh,
        userId = userId,
        email = email,
        expiresAt = expiresAt
    )
}

fun HouseholdResponse.toDomain(): Household {
    return Household(
        id = id,
        name = name,
        description = description ?: ""
    )
}

fun List<HouseholdResponse>.toDomain(): List<Household> {
    return this.map { it.toDomain() }
}
