package com.nexohogar.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nexohogar.domain.model.UserSession
import java.io.File

class SessionManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = createEncryptedPrefs(appContext)

    companion object {
        private const val TAG = "SessionManager"
        private const val PREFS_NAME = "NexoHogarSecurePrefs"
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
        private const val USER_ID = "user_id"
        private const val USER_EMAIL = "user_email"
        private const val EXPIRES_AT_STR = "expires_at_str"
        private const val IS_GUEST = "is_guest"
        private const val SELECTED_HOUSEHOLD_ID = "selected_household_id"
        private const val EXPIRY_MARGIN_MS = 5 * 60 * 1000L
        private const val BIOMETRIC_ENABLED = "biometric_enabled"

        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            return try {
                buildEncryptedPrefs(context)
            } catch (e: Exception) {
                Log.w(TAG, "EncryptedSharedPreferences init failed (${e.message}). Recovering...")
                try {
                    context.deleteSharedPreferences(PREFS_NAME)
                    val ks = java.security.KeyStore.getInstance("AndroidKeyStore")
                    ks.load(null)
                    if (ks.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                        ks.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    }
                } catch (cleanupEx: Exception) {
                    Log.e(TAG, "Cleanup error: ${cleanupEx.message}")
                }
                try {
                    buildEncryptedPrefs(context)
                } catch (retryEx: Exception) {
                    Log.e(TAG, "EncryptedSharedPreferences unrecoverable, using plain fallback")
                    context.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
                }
            }
        }

        private fun buildEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context, PREFS_NAME, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    // ── Core session operations ────────────────────────────────────────────

    fun saveSession(session: UserSession) {
        // Write to EncryptedSharedPreferences
        val success = try {
            prefs.edit().apply {
                putString(ACCESS_TOKEN, session.accessToken)
                putString(REFRESH_TOKEN, session.refreshToken)
                putString(USER_ID, session.userId)
                putString(USER_EMAIL, session.email)
                putString(EXPIRES_AT_STR, session.expiresAt.toString())
                putBoolean(IS_GUEST, session.isGuest)
            }.commit()
        } catch (e: Exception) {
            Log.e(TAG, "❌ EncryptedPrefs commit threw: ${e.message}")
            false
        }

        if (!success) {
            Log.w(TAG, "⚠️ EncryptedPrefs commit() returned false — data may not be persisted!")
        }
        // Verify write
        try {
            val readBack = prefs.getString(REFRESH_TOKEN, null)
            if (readBack != session.refreshToken) {
                Log.e(TAG, "❌ VERIFY FAILED: refresh_token not persisted! readBack=${readBack?.take(8)}, expected=${session.refreshToken.take(8)}")
            } else {
                Log.d(TAG, "✅ Session saved & verified (token ${session.accessToken.take(8)}..., expires ${session.expiresAt})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Verify read threw: ${e.message}")
        }
    }

    fun fetchSession(): UserSession? {
        // Try EncryptedSharedPreferences first
        val fromPrefs = try {
            val token = prefs.getString(ACCESS_TOKEN, null)
            if (token == null && !prefs.getBoolean(IS_GUEST, false)) {
                Log.d(TAG, "fetchSession: no access_token and not guest")
                null
            } else {
                val refresh = prefs.getString(REFRESH_TOKEN, "") ?: ""
                val id = prefs.getString(USER_ID, "") ?: ""
                val email = prefs.getString(USER_EMAIL, "") ?: ""
                val expires = prefs.getString(EXPIRES_AT_STR, null)?.toLongOrNull() ?: 0L
                val guest = prefs.getBoolean(IS_GUEST, false)
                UserSession(token ?: "", refresh, id, email, expires, guest)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchSession from prefs threw: ${e.message}")
            null
        }

        if (fromPrefs != null && fromPrefs.refreshToken.isNotBlank()) {
            return fromPrefs
        }

        return null
    }

    fun fetchAuthToken(): String? {
        return try {
            prefs.getString(ACCESS_TOKEN, null)
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchAuthToken threw: ${e.message}")
            return null
        }
    }

    fun isTokenExpired(): Boolean {
        val session = fetchSession() ?: return false
        if (session.expiresAt == 0L) {
            Log.d(TAG, "⚠️ expiresAt=0 → assuming expired")
            return true
        }
        val expired = System.currentTimeMillis() >= (session.expiresAt - EXPIRY_MARGIN_MS)
        if (expired) {
            Log.d(TAG, "⏰ Token expired (expiresAt=${session.expiresAt}, now=${System.currentTimeMillis()})")
        }
        return expired
    }

    fun fetchRefreshToken(): String? {
        // Try prefs first
        val fromPrefs = try {
            prefs.getString(REFRESH_TOKEN, null)
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchRefreshToken threw: ${e.message}")
            null
        }
        if (!fromPrefs.isNullOrBlank()) return fromPrefs

        return null
    }

    fun saveSelectedHouseholdId(id: String) {
        try { prefs.edit().putString(SELECTED_HOUSEHOLD_ID, id).apply() } catch (_: Exception) {}
    }

    fun fetchSelectedHouseholdId(): String? {
        return try { prefs.getString(SELECTED_HOUSEHOLD_ID, null) } catch (_: Exception) { null }
    }

    fun clearSession() {
        Log.d(TAG, "🗑️ clearSession() called")
        try { prefs.edit().clear().commit() } catch (_: Exception) {}
    }

    fun saveExtra(key: String, value: String) {
        try { prefs.edit().putString(key, value).apply() } catch (_: Exception) {}
    }
    fun getExtra(key: String): String? {
        return try { prefs.getString(key, null) } catch (_: Exception) { null }
    }
    fun removeExtra(key: String) {
        try { prefs.edit().remove(key).apply() } catch (_: Exception) {}
    }

    // ── Biometric preference ──────────────────────────────────────────────
    fun setBiometricEnabled(enabled: Boolean) {
        try { prefs.edit().putBoolean(BIOMETRIC_ENABLED, enabled).apply() } catch (_: Exception) {}
    }

    fun isBiometricEnabled(): Boolean {
        return try { prefs.getBoolean(BIOMETRIC_ENABLED, false) } catch (_: Exception) { false }
    }

    /**
     * Returns true if the user has previously logged in (has stored credentials)
     * AND has enabled biometric authentication.
     */
    fun shouldOfferBiometric(): Boolean {
        return isBiometricEnabled() && fetchSession() != null
    }
}
