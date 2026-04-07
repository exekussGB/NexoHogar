package com.nexohogar.presentation.tutorial

import android.content.Context
import android.content.SharedPreferences

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ARCHIVO NUEVO: Gestión de estado del tutorial
 *
 * Usa SharedPreferences para recordar si el usuario ya vio cada tutorial.
 * Se puede agregar al ServiceLocator o inyectar directamente con Context.
 * ═══════════════════════════════════════════════════════════════════════════════
 */
class TutorialPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nexohogar_tutorial", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HUB_TUTORIAL_SHOWN = "hub_tutorial_shown"
        private const val KEY_INVENTORY_TUTORIAL_SHOWN = "inventory_tutorial_shown"
    }

    // ── Hub Tutorial ──────────────────────────────────────────────────────
    fun isHubTutorialShown(): Boolean = prefs.getBoolean(KEY_HUB_TUTORIAL_SHOWN, false)

    fun setHubTutorialShown() {
        prefs.edit().putBoolean(KEY_HUB_TUTORIAL_SHOWN, true).apply()
    }

    // ── Inventory Tutorial ────────────────────────────────────────────────
    fun isInventoryTutorialShown(): Boolean = prefs.getBoolean(KEY_INVENTORY_TUTORIAL_SHOWN, false)

    fun setInventoryTutorialShown() {
        prefs.edit().putBoolean(KEY_INVENTORY_TUTORIAL_SHOWN, true).apply()
    }

    // ── Reset (útil para testing) ─────────────────────────────────────────
    fun resetAll() {
        prefs.edit().clear().apply()
    }
}
