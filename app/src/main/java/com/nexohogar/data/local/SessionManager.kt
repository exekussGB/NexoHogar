package com.nexohogar.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nexohogar.domain.model.UserSession

/**
 * Clase encargada de gestionar la persistencia de la sesión del usuario.
 * SEC-02: Migrado a EncryptedSharedPreferences para cifrar tokens en reposo.
 *
 * FIX-SESSION-01: expiresAt se almacena como String para evitar el bug conocido
 *   de EncryptedSharedPreferences con getLong() en security-crypto:1.0.x.
 *   Ver: https://issuetracker.google.com/issues/164901843
 *
 * FIX-SESSION-02: saveSession() usa commit() (síncrono) para garantizar que los
 *   tokens se persisten en disco antes de continuar, evitando pérdida de datos
 *   si el proceso es terminado inmediatamente después.
 *
 * FIX-SESSION-03: isTokenExpired() trata expiresAt == 0L como token potencialmente
 *   expirado (en lugar de "no expirado"), forzando un refresh proactivo. Esto cubre
 *   la migración de sesiones antiguas y el caso edge donde expiresAt no fue guardado.
 *
 * FIX-SESSION-04: createEncryptedPrefs() tiene manejo de error robusto. Si el
 *   AndroidKeyStore invalida la clave (post-update, cambio de biometría, etc.),
 *   se limpian las prefs corruptas y se recrean en lugar de crashear.
 */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    companion object {
        private const val TAG = "SessionManager"
        private const val PREFS_NAME            = "NexoHogarSecurePrefs"
        private const val ACCESS_TOKEN          = "access_token"
        private const val REFRESH_TOKEN         = "refresh_token"
        private const val USER_ID               = "user_id"
        private const val USER_EMAIL            = "user_email"
        // FIX-SESSION-01: nueva clave para almacenamiento como String
        private const val EXPIRES_AT_STR        = "expires_at_str"
        private const val SELECTED_HOUSEHOLD_ID = "selected_household_id"

        /** Margen de seguridad: considera expirado si faltan menos de 5 minutos. */
        private const val EXPIRY_MARGIN_MS = 5 * 60 * 1000L

        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            // FIX-SESSION-04: manejo robusto de errores en inicialización
            return try {
                buildEncryptedPrefs(context)
            } catch (e: Exception) {
                Log.w(TAG, "EncryptedSharedPreferences init falló (${e.message}). " +
                        "Intentando recuperar borrando prefs corruptas…")
                // Borrar el archivo de prefs corruptas y el alias de keystore
                try {
                    context.deleteSharedPreferences(PREFS_NAME)
                    val ks = java.security.KeyStore.getInstance("AndroidKeyStore")
                    ks.load(null)
                    if (ks.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                        ks.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    }
                } catch (cleanupEx: Exception) {
                    Log.e(TAG, "Error limpiando prefs corruptas: ${cleanupEx.message}")
                }
                // Segundo intento tras la limpieza
                try {
                    buildEncryptedPrefs(context)
                } catch (retryEx: Exception) {
                    Log.e(TAG, "EncryptedSharedPreferences no pudo inicializarse, " +
                            "usando SharedPreferences planas como fallback de emergencia. " +
                            "Los tokens NO estarán cifrados en este dispositivo.")
                    context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
                }
            }
        }

        private fun buildEncryptedPrefs(context: Context): SharedPreferences {
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
     * FIX-SESSION-02: usa commit() para escritura síncrona y confiable.
     */
    fun saveSession(session: UserSession) {
        prefs.edit().apply {
            putString(ACCESS_TOKEN,     session.accessToken)
            putString(REFRESH_TOKEN,    session.refreshToken)
            putString(USER_ID,          session.userId)
            putString(USER_EMAIL,       session.email)
            // FIX-SESSION-01: almacenar expiresAt como String
            putString(EXPIRES_AT_STR,   session.expiresAt.toString())
            commit()  // síncrono — garantiza persistencia antes de continuar
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
        val expires = prefs.getString(EXPIRES_AT_STR, null)?.toLongOrNull() ?: 0L

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
     *
     * FIX-SESSION-03: si expiresAt == 0 (nunca guardado o migración desde formato
     * antiguo) Y hay un token, se considera potencialmente expirado para forzar
     * un refresh proactivo. Esto es seguro: si el token sigue siendo válido en el
     * servidor, el refresh simplemente devuelve uno nuevo sin interrumpir al usuario.
     */
    fun isTokenExpired(): Boolean {
        val token     = prefs.getString(ACCESS_TOKEN, null) ?: return false
        val expiresAt = prefs.getString(EXPIRES_AT_STR, null)?.toLongOrNull() ?: 0L

        // FIX-SESSION-03: tratar expiresAt == 0 como expirado para forzar refresh
        if (expiresAt == 0L) {
            Log.d(TAG, "⚠️ expiresAt no disponible para token ${token.take(8)}… → asumiendo expirado")
            return true
        }

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
        prefs.edit().clear().commit()
    }

    // ── Métodos genéricos para almacenamiento cifrado extra ──────────────────

    fun saveExtra(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getExtra(key: String): String? {
        return prefs.getString(key, null)
    }

    fun removeExtra(key: String) {
        prefs.edit().remove(key).apply()
    }
}
