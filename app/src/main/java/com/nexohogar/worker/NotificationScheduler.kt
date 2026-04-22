package com.nexohogar.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Clase utilitaria para programar/cancelar el worker de notificaciones.
 *
 * Se llama una vez desde NexoHogarApp.onCreate() y se reactiva automáticamente
 * en cada arranque del dispositivo (WorkManager lo persiste).
 */
object NotificationScheduler {

    private const val WORK_NAME = "RecurringBillsNotification"
    private const val RECURRING_BILL_CHECK_WORK_NAME = "recurring_bill_check"
    private const val SCANNER_NUDGE_WORK_NAME = "scanner_nudge"

    /**
     * Programa la verificación de cuentas recurrentes cada 24 horas.
     * Requiere conexión a red para consultar Supabase.
     *
     * ExistingPeriodicWorkPolicy.UPDATE → reemplaza si ya existe (reinicia timer
     * en cada actualización de la app para garantizar ejecución fresca).
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
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    /**
     * Cancela las notificaciones (por si el usuario lo desactiva en el futuro).
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Programa la verificación de cuentas recurrentes pendientes de pago.
     * Se ejecuta diariamente a las 9:00 AM.
     */
    fun scheduleRecurringBillCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<RecurringBillCheckWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RECURRING_BILL_CHECK_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    /**
     * Ejecuta UNA verificación inmediata de cuentas recurrentes.
     * Usar desde Settings → "Probar notificación ahora" para confirmar que el
     * sistema funciona sin esperar el ciclo de 24 horas.
     */
    fun testNotification(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<RecurringBillsNotificationWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(oneTimeRequest)
    }

    /**
     * Programa un recordatorio para escanear boletas cada 3 días.
     */
    fun scheduleScannerNudge(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<ScannerNudgeWorker>(
            3, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SCANNER_NUDGE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun calculateInitialDelay(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
