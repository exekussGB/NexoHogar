package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.core.util.AppLogger
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.network.AuthApi
import com.nexohogar.domain.model.UserSession
import com.nexohogar.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.OtpType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Implementación del repositorio de autenticación usando Supabase SDK.
 */
class AuthRepositoryImpl(
    private val sessionManager: SessionManager,
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"

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
                this.data = buildJsonObject {
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
        // Limpiar legacy store PRIMERO — antes de cualquier fallo de red
        sessionManager.clearSession()
        try {
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error al cerrar sesión en supabase: ${e.message}")
            // El SDK limpiará su DataStore en el próximo inicio aunque falle aquí
        }
    }

    override fun getCurrentSession(): UserSession? = buildAndCacheDomainSession()

    // ── Recuperación de contraseña (Retrofit) ─────────────────────────────────
    // Estos flujos no requieren sesión persistente → se mantienen con Retrofit.

    override suspend fun forgotPassword(email: String): AppResult<Unit> {
        return try {
            supabaseClient.auth.resetPasswordForEmail(email)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error en forgotPassword (SDK): ${e.message}")
            AppResult.Error(parseLoginError(e))
        }
    }

    override suspend fun updatePassword(accessToken: String, newPassword: String): AppResult<Unit> {
        return try {
            // El SDK permite actualizar el usuario actual si la sesión está activa
            // O podemos usar el flujo de reset si venimos de un token
            supabaseClient.auth.updateUser {
                password = newPassword
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error en updatePassword (SDK): ${e.message}")
            AppResult.Error(parseLoginError(e))
        }
    }

    override suspend fun verifyOtp(email: String, code: String): AppResult<String> {
        return try {
            supabaseClient.auth.verifyEmailOtp(type = OtpType.Email.RECOVERY, email = email, token = code)
            val session = supabaseClient.auth.currentSessionOrNull()
            if (session != null) {
                AppResult.Success(session.accessToken)
            } else {
                AppResult.Error("No se pudo iniciar sesión tras verificar el código")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error en verifyOtp (SDK): ${e.message}")
            AppResult.Error("Código inválido o expirado")
        }
    }
}
