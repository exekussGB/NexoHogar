package com.nexohogar

import android.app.Application
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.service.FcmTokenManager
import com.nexohogar.service.NexoHogarMessagingService
import com.nexohogar.worker.NotificationScheduler

class NexoHogarApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inyección de dependencias (incluye inicialización del SupabaseClient)
        ServiceLocator.init(this)

        // Crear canales de notificación push
        NexoHogarMessagingService.createNotificationChannels(this)

        // Programar verificación diaria de cuentas recurrentes (local)
        NotificationScheduler.schedule(this)

        // Registrar token FCM si hay sesión activa.
        // La sesión se lee del SessionManager (cacheado en login) como check rápido sincrónico.
        // supabase-kt cargará la sesión completa desde DataStore de forma asíncrona
        // durante el Splash screen (awaitInitialization vía sessionStatus).
        if (ServiceLocator.sessionManager.fetchSession() != null) {
            FcmTokenManager.registerToken(this)
        }

        // NOTA: SessionRefresher ya NO se inicializa aquí.
        // El SDK de Supabase (supabase-kt) con alwaysAutoRefresh=true y
        // SupabaseDataStoreSessionManager gestiona el ciclo de vida del token
        // automáticamente, sin intervención de SessionRefresher ni TokenRefreshCoordinator.
    }
}
