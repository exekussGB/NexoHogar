package com.nexohogar

import android.app.Application
import com.nexohogar.core.di.ServiceLocator

/**
 * Clase Application personalizada para la inicialización global de componentes.
 * Aquí se inicializa el ServiceLocator para gestionar la inyección de dependencias.
 */
class NexoHogarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicialización única y global del ServiceLocator
        ServiceLocator.init(this)
    }
}
