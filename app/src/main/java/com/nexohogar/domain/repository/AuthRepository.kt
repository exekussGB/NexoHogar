package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.UserSession

/**
 * Interfaz del repositorio de autenticación en la capa de dominio.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): AppResult<UserSession>
    suspend fun register(email: String, password: String, name: String): Result<Unit>
    suspend fun logout()
    fun getCurrentSession(): UserSession?
}
