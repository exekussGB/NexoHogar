package com.nexohogar

import android.app.Application
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.service.FcmTokenManager
import com.nexohogar.service.NexoHogarMessagingService
import com.nexohogar.worker.NotificationScheduler

class NexoHogarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inyección de dependencias
        ServiceLocator.init(this)
        // Crear canales de notificación push
        NexoHogarMessagingService.createNotificationChannels(this)
        // Programar verificación diaria de cuentas recurrentes (local)
        NotificationScheduler.schedule(this)
        // Registrar token FCM si hay sesión activa
        if (ServiceLocator.sessionManager.fetchSession() != null) {
            FcmTokenManager.registerToken(this)
        }

        // FIX-SESSION-05: Inicializar SessionRefresher para mantener
        // la sesión activa automáticamente (lifecycle-aware)
        ServiceLocator.sessionRefresher.init()
    }
}
