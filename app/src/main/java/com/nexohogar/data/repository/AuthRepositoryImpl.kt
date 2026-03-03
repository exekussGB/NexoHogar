package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.mapper.toDomain
import com.nexohogar.data.model.LoginRequest
import com.nexohogar.data.network.AuthApi
import com.nexohogar.domain.model.UserSession
import com.nexohogar.domain.repository.AuthRepository

/**
 * Implementación del repositorio de autenticación.
 * Transforma los DTOs de la capa de datos en modelos de dominio.
 */
class AuthRepositoryImpl(
    private val api: AuthApi
) : AuthRepository {

    override suspend fun login(email: String, password: String): AppResult<UserSession> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()
                val domainSession = body?.toDomain()
                
                if (domainSession != null) {
                    AppResult.Success(domainSession)
                } else {
                    AppResult.Error("Respuesta de sesión inválida")
                }
            } else {
                AppResult.Error("Credenciales inválidas")
            }
        } catch (e: Exception) {
            AppResult.Error("Error de conexión: ${e.message}")
        }
    }
}
