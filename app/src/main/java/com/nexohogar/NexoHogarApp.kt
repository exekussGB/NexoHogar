package com.nexohogar

import android.app.Application
import androidx.work.*
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.data.sync.SyncWorker
import com.nexohogar.service.FcmTokenManager
import com.nexohogar.service.NexoHogarMessagingService
import com.nexohogar.worker.NotificationScheduler
import java.util.concurrent.TimeUnit

class NexoHogarApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inyección de dependencias (incluye inicialización del SupabaseClient)
        ServiceLocator.init(this)

        // Programar sincronización de fondo (Offline-First)
        setupBackgroundSync()

        // Crear canales de notificación push
        NexoHogarMessagingService.createNotificationChannels(this)

        // Programar verificación diaria de cuentas recurrentes (local)
        NotificationScheduler.schedule(this)
        NotificationScheduler.scheduleScannerNudge(this)

        // Registrar token FCM si hay sesión activa
        if (ServiceLocator.sessionManager.fetchSession() != null) {
            FcmTokenManager.registerToken(this)
        }
    }

    private fun setupBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BackgroundSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
