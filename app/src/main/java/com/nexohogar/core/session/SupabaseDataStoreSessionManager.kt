package com.nexohogar.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementación persistente del SessionManager del SDK de Supabase.
 *
 * Problema resuelto:
 *   Sin esta clase, el SDK usa MemorySessionManager (en RAM).
 *   Al morir el proceso o expirar el token (1h), el usuario era forzado
 *   a re-autenticarse aunque tuviera refresh_token válido.
 *
 * Solución:
 *   - Persiste la UserSession serializada en DataStore.
 *   - El SDK carga la sesión al iniciar → restaura access_token + refresh_token.
 *   - Con [alwaysAutoRefresh = true] en ServiceLocator, el SDK renueva el
 *     access_token automáticamente antes de que expire, sin intervención del usuario.
 */
private val Context.nexoHogarSessionDataStore by preferencesDataStore(name = "nexo_hogar_session")

class SupabaseDataStoreSessionManager(private val context: Context) : SessionManager {

    private companion object {
        val SESSION_KEY = stringPreferencesKey("supabase_user_session")

        // Json con ignoreUnknownKeys por compatibilidad con futuras versiones del SDK
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }
    }

    override suspend fun saveSession(session: UserSession) {
        val serialized = json.encodeToString(session)
        context.nexoHogarSessionDataStore.edit { prefs ->
            prefs[SESSION_KEY] = serialized
        }
    }

    override suspend fun loadSession(): UserSession? {
        val prefs = context.nexoHogarSessionDataStore.data.firstOrNull() ?: return null
        val serialized = prefs[SESSION_KEY] ?: return null
        return runCatching {
            json.decodeFromString<UserSession>(serialized)
        }.getOrNull()
    }

    override suspend fun deleteSession() {
        context.nexoHogarSessionDataStore.edit { prefs ->
            prefs.remove(SESSION_KEY)
        }
    }
}
