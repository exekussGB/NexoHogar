package com.nexohogar

import android.app.Application
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.service.FcmTokenManager
import com.nexohogar.worker.NotificationScheduler

class NexoHogarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inyección de dependencias
        ServiceLocator.init(this)
        // Programar verificación diaria de cuentas recurrentes
        NotificationScheduler.schedule(this)
        // Registrar token FCM si hay sesión activa
        if (ServiceLocator.sessionManager.fetchSession() != null) {
            FcmTokenManager.registerToken(this)
        }
    }
}
