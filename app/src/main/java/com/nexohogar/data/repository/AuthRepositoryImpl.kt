package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.mapper.toDomain
import com.nexohogar.data.model.LoginRequest
import com.nexohogar.data.network.AuthApi
import com.nexohogar.domain.model.UserSession
import com.nexohogar.domain.repository.AuthRepository

/**
 * Implementación del repositorio de autenticación.
 * Maneja la lógica de login y la persistencia de la sesión.
 */
class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): AppResult<UserSession> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()
                val domainSession = body?.toDomain()
                
                if (domainSession != null) {
                    // Guardamos la sesión exitosa
                    sessionManager.saveSession(domainSession)
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

    override suspend fun logout() {
        sessionManager.clearSession()
    }

    override fun getCurrentSession(): UserSession? {
        return sessionManager.fetchSession()
    }
}
