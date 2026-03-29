package com.nexohogar.core.tutorial

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de estado de tutoriales por módulo.
 * Persiste el estado de completación en SharedPreferences.
 */
class TutorialManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "nexohogar_tutorials"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
}
