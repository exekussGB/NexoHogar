package com.nexohogar.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Clase utilitaria para programar/cancelar el worker de notificaciones.
 *
 * Se llama una vez desde NexoHogarApp.onCreate() y se reactiva automáticamente
 * en cada arranque del dispositivo (WorkManager lo persiste).
 */
object NotificationScheduler {

    private const val WORK_NAME = "RecurringBillsNotification"

    /**
     * Programa la verificación de cuentas recurrentes cada 24 horas.
     * Requiere conexión a red para consultar Supabase.
     *
     * ExistingPeriodicWorkPolicy.KEEP → si ya existe, no lo reemplaza.
     */
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<RecurringBillsNotificationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Cancela las notificaciones (por si el usuario lo desactiva en el futuro).
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
