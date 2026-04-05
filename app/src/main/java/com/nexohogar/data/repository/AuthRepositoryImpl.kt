package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.core.util.AppLogger
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.AuthApi
import com.nexohogar.data.remote.dto.UpdatePasswordRequest
import com.nexohogar.data.remote.dto.VerifyOtpRequest
import com.nexohogar.domain.model.UserSession
import com.nexohogar.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

/**
 * Implementación del repositorio de autenticación.
 *
 * LOGIN / REGISTER / LOGOUT — delegado al SDK de Supabase (supabase-kt).
 *   El SDK persiste la sesión en DataStore y la refresca automáticamente
 *   → el usuario nunca tiene que volver a hacer login mientras el refresh_token sea válido.
 *
 * FORGOT PASSWORD / VERIFY OTP / UPDATE PASSWORD — siguen usando Retrofit
 *   ya que ese flujo no requiere gestión de sesión de larga duración.
 */
class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager,
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"

        private fun userFriendlyMessage(code: Int): String = when (code) {
            400 -> "Datos inválidos"
            401 -> "Sesión expirada"
            422 -> "Los datos no cumplen los requisitos"
            429 -> "Demasiados intentos, intenta más tarde"
            else -> "Error inesperado, intenta nuevamente"
        }

        private fun parseLoginError(e: Throwable): String = when {
            e.message?.contains("email_not_confirmed", ignoreCase = true) == true ->
                "Email no confirmado. Revisa tu bandeja de entrada."
            e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                "Email o contraseña incorrectos."
            e.message?.contains("rate limit", ignoreCase = true) == true ||
            e.message?.contains("over_email_send_rate_limit", ignoreCase = true) == true ->
                "Demasiados intentos. Espera unos minutos e intenta nuevamente."
            e.message?.contains("network", ignoreCase = true) == true ||
            e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                "Sin conexión a internet. Verifica tu red."
            else -> "Error al iniciar sesión. Intenta nuevamente."
        }

        private fun parseRegisterError(e: Throwable): String = when {
            e.message?.contains("User already registered", ignoreCase = true) == true ->
                "El correo ya está registrado o es inválido"
            e.message?.contains("Password should be at least", ignoreCase = true) == true ->
                "La contraseña debe tener al menos 6 caracteres"
            e.message?.contains("rate limit", ignoreCase = true) == true ->
                "Demasiados intentos, intenta más tarde"
            e.message?.contains("network", ignoreCase = true) == true ||
            e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                "Sin conexión a internet. Verifica tu red."
            else -> "Error inesperado, intenta nuevamente"
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Construye el modelo de dominio a partir de la sesión activa del SDK.
     * También guarda una copia en SessionManager para retrocompatibilidad
     * (TenantContext y otros componentes leen userId/email desde ahí).
     */
    private fun buildAndCacheDomainSession(): UserSession? {
        val sdkSession = supabaseClient.auth.currentSessionOrNull() ?: return null
        val user = sdkSession.user
        val domainSession = UserSession(
            accessToken  = sdkSession.accessToken,
            refreshToken = sdkSession.refreshToken,
            userId       = user?.id ?: "",
            email        = user?.email ?: "",
            expiresAt    = System.currentTimeMillis() + (sdkSession.expiresIn * 1000L)
        )
        sessionManager.saveSession(domainSession)
        return domainSession
    }

    // ── Auth principal ────────────────────────────────────────────────────────

    override suspend fun login(email: String, password: String): AppResult<UserSession> {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email    = email
                this.password = password
            }
            val domainSession = buildAndCacheDomainSession()
                ?: return AppResult.Error("Respuesta de sesión inválida")
            AppResult.Success(domainSession)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error en login: ${e.message}")
            AppResult.Error(parseLoginError(e))
        }
    }

    override suspend fun register(email: String, password: String, name: String): AppResult<Unit> {
        return try {
            supabaseClient.auth.signUpWith(Email) {
                this.email    = email
                this.password = password
                data {
                    put("full_name", name)
                }
            }
            // Guardar sesión si el registro fue inmediato (sin confirmación de email)
            buildAndCacheDomainSession()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error en register: ${e.message}")
            AppResult.Error(parseRegisterError(e))
        }
    }

    override suspend fun logout() {
        try {
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error al cerrar sesión en supabase: ${e.message}")
        }
        sessionManager.clearSession()
    }

    override fun getCurrentSession(): UserSession? = buildAndCacheDomainSession()

    // ── Recuperación de contraseña (Retrofit) ─────────────────────────────────
    // Estos flujos no requieren sesión persistente → se mantienen con Retrofit.

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
                token   = "Bearer $accessToken",
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
