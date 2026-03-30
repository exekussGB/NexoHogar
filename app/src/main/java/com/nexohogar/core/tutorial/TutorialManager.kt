package com.nexohogar.core.tutorial

import android.content.Context
import android.content.SharedPreferences

/**
 * RED-02: Gestor unificado de estado de tutoriales.
 * Absorbe la funcionalidad de TutorialPreferences (presentation/tutorial/).
 * Usa un único archivo SharedPreferences: "nexohogar_tutorials".
 *
 * Para tutoriales por módulo (TutorialModule enum) usa isTutorialCompleted/markTutorialCompleted.
 * Para tutoriales de UI específicos (hub, inventory) usa los métodos isXxxShown/setXxxShown.
 */
class TutorialManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "nexohogar_tutorials"

        // Keys migradas desde TutorialPreferences
        private const val KEY_HUB_TUTORIAL_SHOWN = "hub_tutorial_shown"
        private const val KEY_INVENTORY_TUTORIAL_SHOWN = "inventory_tutorial_shown"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Tutoriales por módulo (enum-based) ───────────────────────────────────

    /**
     * Verifica si el tutorial de un módulo fue completado.
     */
    fun isTutorialCompleted(module: TutorialModule): Boolean {
        return prefs.getBoolean(module.key, false)
    }

    /**
     * Marca el tutorial de un módulo como completado.
     */
    fun markTutorialCompleted(module: TutorialModule) {
        prefs.edit().putBoolean(module.key, true).apply()
    }

    /**
     * Resetea un tutorial específico para poder repetirlo.
     */
    fun resetTutorial(module: TutorialModule) {
        prefs.edit().remove(module.key).apply()
    }

    /**
     * Resetea todos los tutoriales.
     */
    fun resetAllTutorials() {
        prefs.edit().clear().apply()
    }

    /**
     * Retorna el estado de completación de todos los módulos.
     */
    fun getAllStatus(): Map<TutorialModule, Boolean> {
        return TutorialModule.entries.associateWith { isTutorialCompleted(it) }
    }

    // ── Tutoriales de UI específicos (migrados de TutorialPreferences) ───────

    fun isHubTutorialShown(): Boolean = prefs.getBoolean(KEY_HUB_TUTORIAL_SHOWN, false)

    fun setHubTutorialShown() {
        prefs.edit().putBoolean(KEY_HUB_TUTORIAL_SHOWN, true).apply()
    }

    fun isInventoryTutorialShown(): Boolean = prefs.getBoolean(KEY_INVENTORY_TUTORIAL_SHOWN, false)

    fun setInventoryTutorialShown() {
        prefs.edit().putBoolean(KEY_INVENTORY_TUTORIAL_SHOWN, true).apply()
    }
}
