package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.mapper.toDomain
import com.nexohogar.data.model.LoginRequest
import com.nexohogar.data.network.AuthApi
import com.nexohogar.data.remote.dto.RegisterRequest
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

    override suspend fun register(email: String, password: String, name: String): Result<Unit> {
        return try {
            val request = RegisterRequest(
                email = email,
                password = password,
                data = mapOf("full_name" to name)
            )
            val response = authApi.register(request)
            if (response.isSuccessful) {
                val body = response.body()
                val domainSession = body?.toDomain()
                
                if (domainSession != null) {
                    sessionManager.saveSession(domainSession)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("No se recibió token de acceso"))
                }
            } else {
                val errorMsg = when (response.code()) {
                    422 -> "El correo ya está registrado o es inválido"
                    400 -> "Datos inválidos"
                    else -> "Error al registrar (${response.code()})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        sessionManager.clearSession()
    }

    override suspend fun forgotPassword(email: String): AppResult<Unit> {
        return try {
            val response = authApi.forgotPassword(mapOf("email" to email))
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error("No se pudo enviar el correo de recuperación")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error de conexión")
        }
    }

    override fun getCurrentSession(): UserSession? {
        return sessionManager.fetchSession()
    }
}
