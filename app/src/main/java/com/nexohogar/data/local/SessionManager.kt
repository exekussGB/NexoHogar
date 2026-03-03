package com.nexohogar.data.local

import android.content.Context
import android.content.SharedPreferences
import com.nexohogar.domain.model.UserSession

/**
 * Clase encargada de gestionar la persistencia de la sesión del usuario.
 * Almacena tokens, información de usuario y tiempos de expiración.
 * Esta clase reside en la capa de DATA (local) y utiliza el modelo de DOMINIO para comunicarse.
 */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("NexoHogarPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
        private const val USER_ID = "user_id"
        private const val USER_EMAIL = "user_email"
        private const val EXPIRES_AT = "expires_at"
        private const val SELECTED_HOUSEHOLD_ID = "selected_household_id"
    }

    /**
     * Guarda la sesión completa del usuario usando el modelo de Dominio.
     */
    fun saveSession(session: UserSession) {
        prefs.edit().apply {
            putString(ACCESS_TOKEN, session.accessToken)
            putString(REFRESH_TOKEN, session.refreshToken)
            putString(USER_ID, session.userId)
            putString(USER_EMAIL, session.email)
            putLong(EXPIRES_AT, session.expiresAt)
            apply()
        }
    }

    /**
     * Recupera la sesión actual y la mapea al modelo de Dominio.
     * Retorna null si no hay un token de acceso persistido.
     */
    fun fetchSession(): UserSession? {
        val token = prefs.getString(ACCESS_TOKEN, null) ?: return null
        val refresh = prefs.getString(REFRESH_TOKEN, "") ?: ""
        val id = prefs.getString(USER_ID, "") ?: ""
        val email = prefs.getString(USER_EMAIL, "") ?: ""
        val expires = prefs.getLong(EXPIRES_AT, 0L)

        return UserSession(
            accessToken = token,
            refreshToken = refresh,
            userId = id,
            email = email,
            expiresAt = expires
        )
    }

    /**
     * Método de conveniencia para el Interceptor de red.
     */
    fun fetchAuthToken(): String? {
        return prefs.getString(ACCESS_TOKEN, null)
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
}
