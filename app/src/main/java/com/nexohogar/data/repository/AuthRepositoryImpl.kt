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
 * SEC-ERR: Error bodies are logged but not exposed to user — user-friendly messages only.
 */
class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"

        /**
         * Maps HTTP error codes to user-friendly messages.
         * Actual error bodies are logged for debugging but never exposed to the UI.
         */
        private fun userFriendlyMessage(code: Int): String = when (code) {
            400 -> "Datos inválidos"
            401 -> "Sesión expirada"
            422 -> "Los datos no cumplen los requisitos"
            429 -> "Demasiados intentos, intenta más tarde"
            else -> "Error inesperado, intenta nuevamente"
        }
    }

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
                val errorBody = response.errorBody()?.string()
                AppLogger.e(TAG, "Error en login HTTP ${response.code()}: $errorBody")
                AppResult.Error(userFriendlyMessage(response.code()))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error en login: ${e.message}")
            AppResult.Error("Error de conexión, intenta nuevamente")
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
                val errorBody = response.errorBody()?.string()
                AppLogger.e(TAG, "Error en register HTTP ${response.code()}: $errorBody")
                val errorMsg = when (response.code()) {
                    422 -> "El correo ya está registrado o es inválido"
                    400 -> "Datos inválidos"
                    429 -> "Demasiados intentos, intenta más tarde"
                    else -> "Error inesperado, intenta nuevamente"
                }
                AppResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error en register: ${e.message}")
            AppResult.Error("Error de conexión, intenta nuevamente")
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
                val errorBody = response.errorBody()?.string()
                AppLogger.e(TAG, "Error en forgotPassword HTTP ${response.code()}: $errorBody")
                AppResult.Error(userFriendlyMessage(response.code()))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error en forgotPassword: ${e.message}")
            AppResult.Error("Error de conexión, intenta nuevamente")
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
                AppLogger.e(TAG, "Error en updatePassword HTTP ${response.code()}: $errorBody")
                AppResult.Error(userFriendlyMessage(response.code()))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error en updatePassword: ${e.message}")
            AppResult.Error("Error de conexión, intenta nuevamente")
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
                val errorBody = response.errorBody()?.string()
                AppLogger.e(TAG, "Error en verifyOtp HTTP ${response.code()}: $errorBody")
                AppResult.Error(userFriendlyMessage(response.code()))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error en verifyOtp: ${e.message}")
            AppResult.Error("Error de conexión, intenta nuevamente")
        }
    }
}
