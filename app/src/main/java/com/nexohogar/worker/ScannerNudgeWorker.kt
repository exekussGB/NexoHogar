package com.nexohogar.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexohogar.R
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.service.NexoHogarMessagingService

class ScannerNudgeWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Solo enviar si el usuario tiene habilitadas las notificaciones generales
        if (!ServiceLocator.notificationPreferences.generalEnabled) {
            return Result.success()
        }

        // Lógica: Si no ha habido escaneos en los últimos 3 días (opcional, por ahora enviamos siempre según ciclo)
        
        showNudgeNotification()
        return Result.success()
    }

    private fun showNudgeNotification() {
        val notification = NotificationCompat.Builder(applicationContext, NexoHogarMessagingService.CHANNEL_GENERAL)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("¿Tienes boletas pendientes?")
            .setContentText("No olvides escanear tus boletas de hoy para mantener tus gastos al día. 📸")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(2001, notification)
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones en API 33+
        }
    }
}
