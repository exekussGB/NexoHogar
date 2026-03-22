package com.nexohogar

import android.app.Application
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.worker.NotificationScheduler

/**
 * Clase Application personalizada para la inicialización global de componentes.
 */
class NexoHogarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inyección de dependencias
        ServiceLocator.init(this)
        // Programar verificación diaria de cuentas recurrentes
        NotificationScheduler.schedule(this)
    }
}
