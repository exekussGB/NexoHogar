package com.nexohogar.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nexohogar.domain.model.UserSession

/**
 * Clase encargada de gestionar la persistencia de la sesión del usuario.
 * SEC-02: Migrado a EncryptedSharedPreferences para cifrar tokens en reposo.
 */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    companion object {
        private const val PREFS_NAME            = "NexoHogarSecurePrefs"
        private const val ACCESS_TOKEN          = "access_token"
        private const val REFRESH_TOKEN         = "refresh_token"
        private const val USER_ID               = "user_id"
        private const val USER_EMAIL            = "user_email"
        private const val EXPIRES_AT            = "expires_at"
        private const val SELECTED_HOUSEHOLD_ID = "selected_household_id"

        /** Margen de seguridad: considera expirado si faltan menos de 2 minutos. */
        private const val EXPIRY_MARGIN_MS = 2 * 60 * 1000L

        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    /**
     * Guarda la sesión completa del usuario usando el modelo de Dominio.
     */
    fun saveSession(session: UserSession) {
        prefs.edit().apply {
            putString(ACCESS_TOKEN,  session.accessToken)
            putString(REFRESH_TOKEN, session.refreshToken)
            putString(USER_ID,       session.userId)
            putString(USER_EMAIL,    session.email)
            putLong(EXPIRES_AT,      session.expiresAt)
            apply()
        }
    }

    /**
     * Recupera la sesión actual y la mapea al modelo de Dominio.
     * Retorna null si no hay un token de acceso persistido.
     */
    fun fetchSession(): UserSession? {
        val token   = prefs.getString(ACCESS_TOKEN, null) ?: return null
        val refresh = prefs.getString(REFRESH_TOKEN, "") ?: ""
        val id      = prefs.getString(USER_ID, "") ?: ""
        val email   = prefs.getString(USER_EMAIL, "") ?: ""
        val expires = prefs.getLong(EXPIRES_AT, 0L)

        return UserSession(
            accessToken  = token,
            refreshToken = refresh,
            userId       = id,
            email        = email,
            expiresAt    = expires
        )
    }

    /**
     * Método de conveniencia para el Interceptor de red.
     */
    fun fetchAuthToken(): String? {
        return prefs.getString(ACCESS_TOKEN, null)
    }

    /**
     * Retorna true si el token de acceso ya expiró o está a punto de expirar.
     */
    fun isTokenExpired(): Boolean {
        val token     = prefs.getString(ACCESS_TOKEN, null) ?: return false
        val expiresAt = prefs.getLong(EXPIRES_AT, 0L)
        if (expiresAt == 0L) return false
        return System.currentTimeMillis() >= (expiresAt - EXPIRY_MARGIN_MS)
    }

    /**
     * Retorna el refresh token almacenado, o null si no existe.
     */
    fun fetchRefreshToken(): String? {
        val token = prefs.getString(REFRESH_TOKEN, null)
        return if (token.isNullOrBlank()) null else token
    }

    /**
     * Guarda el ID del household seleccionado.
     */
    fun saveSelectedHouseholdId(id: String) {
        prefs.edit().putString(SELECTED_HOUSEHOLD_ID, id).apply()
    }

    /**
     * Recupera el ID del household seleccionado.
     */
    fun fetchSelectedHouseholdId(): String? {
        return prefs.getString(SELECTED_HOUSEHOLD_ID, null)
    }

    /**
     * Elimina todos los datos de la sesión (Logout completo).
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }

    // ── Métodos genéricos para almacenamiento cifrado extra ──────────────────
    // RED-04: Permiten a otros componentes (FcmTokenManager, etc.) usar el
    // almacenamiento cifrado sin acceder directamente a SharedPreferences planas.

    /**
     * Guarda un valor String cifrado con la clave dada.
     */
    fun saveExtra(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    /**
     * Recupera un valor String cifrado, o null si no existe.
     */
    fun getExtra(key: String): String? {
        return prefs.getString(key, null)
    }

    /**
     * Elimina un valor cifrado por su clave.
     */
    fun removeExtra(key: String) {
        prefs.edit().remove(key).apply()
    }
}
