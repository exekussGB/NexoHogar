package com.nexohogar.data.local

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Preferencias de tema de la aplicación.
 * Usa Compose State para reactividad inmediata.
 */
class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("NexoHogarPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "theme_mode"
    }

    /** Valores posibles: "system", "light", "dark" */
    var themeMode by mutableStateOf(prefs.getString(KEY_THEME, "system") ?: "system")
        private set

    fun setTheme(mode: String) {
        themeMode = mode
        prefs.edit().putString(KEY_THEME, mode).apply()
    }
}
