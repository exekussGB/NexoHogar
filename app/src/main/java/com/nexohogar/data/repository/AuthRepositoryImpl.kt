package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.core.util.AppLogger
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.mapper.toDomain
import com.nexohogar.data.remote.dto.LoginRequest
import com.nexohogar.data.network.AuthApi
import com.nexohogar.data.remote.dto.RegisterRequest
import com.nexohogar.domain.model.UserSession
import com.nexohogar.domain.repository.AuthRepository
import com.nexohogar.data.remote.dto.UpdatePasswordRequest
import com.nexohogar.data.remote.dto.VerifyOtpRequest

/**
 * Implementación del repositorio de autenticación.
 * COH-04: register() ahora retorna AppResult (consistente con login, forgotPassword, etc.)
 * SEC-04: Logging via AppLogger.
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
                    sessionManager.saveSession(domainSession)
                    AppResult.Success(domainSession)
                } else {
                    AppResult.Error("Respuesta de sesión inválida")
                }
            } else {
                AppResult.Error("Credenciales inválidas")
            }
        } catch (e: Exception) {
            AppLogger.e("AuthRepository", "Error en login: ${e.message}")
            AppResult.Error("Error de conexión: ${e.message}")
        }
    }

    override suspend fun register(email: String, password: String, name: String): AppResult<Unit> {
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
                    AppResult.Success(Unit)
                } else {
                    AppResult.Error("No se recibió token de acceso")
                }
            } else {
                val errorMsg = when (response.code()) {
                    422 -> "El correo ya está registrado o es inválido"
                    400 -> "Datos inválidos"
                    else -> "Error al registrar (${response.code()})"
                }
                AppResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            AppLogger.e("AuthRepository", "Error en register: ${e.message}")
            AppResult.Error("Error de conexión: ${e.message}")
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
            AppLogger.e("AuthRepository", "Error en forgotPassword: ${e.message}")
            AppResult.Error(e.message ?: "Error de conexión")
        }
    }

    override suspend fun updatePassword(accessToken: String, newPassword: String): AppResult<Unit> {
        return try {
            val response = authApi.updatePassword(
                token = "Bearer $accessToken",
                request = UpdatePasswordRequest(password = newPassword)
            )
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                AppResult.Error(errorBody ?: "No se pudo cambiar la contraseña")
            }
        } catch (e: Exception) {
            AppLogger.e("AuthRepository", "Error en updatePassword: ${e.message}")
            AppResult.Error(e.message ?: "Error de conexión")
        }
    }

    override fun getCurrentSession(): UserSession? {
        return sessionManager.fetchSession()
    }

    override suspend fun verifyOtp(email: String, code: String): AppResult<String> {
        return try {
            val response = authApi.verifyOtp(
                VerifyOtpRequest(email = email, token = code, type = "recovery")
            )
            if (response.isSuccessful) {
                val accessToken = response.body()?.accessToken
                if (accessToken != null) {
                    AppResult.Success(accessToken)
                } else {
                    AppResult.Error("No se recibió el token de acceso")
                }
            } else {
                AppResult.Error("Código inválido o expirado")
            }
        } catch (e: Exception) {
            AppLogger.e("AuthRepository", "Error en verifyOtp: ${e.message}")
            AppResult.Error(e.message ?: "Error de conexión")
        }
    }
}
