package com.nexohogar.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.RecurringBill
import com.nexohogar.presentation.MainActivity
import java.time.LocalDate
import com.nexohogar.R
/**
 * Worker que se ejecuta una vez al día (o según programación).
 * Verifica cuentas recurrentes y envía notificaciones para las que vencen pronto.
 *
 * Umbral: notifica si la cuenta vence en los próximos DAYS_AHEAD días o ya venció.
 */
class RecurringBillsNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID   = "nexohogar_recurring_bills"
        const val CHANNEL_NAME = "Cuentas Recurrentes"
        const val DAYS_AHEAD   = 3    // Notificar si vence en 3 días o menos
    }

    override suspend fun doWork(): Result {
        return try {
            // Si el usuario deshabilitó notificaciones de gastos recurrentes, no hacer nada
            if (!ServiceLocator.notificationPreferences.billsEnabled) {
                return Result.success()
            }

            createNotificationChannel()

            val householdId = ServiceLocator.sessionManager.fetchSelectedHouseholdId()
                ?: return Result.success()   // Sin hogar → nada que notificar

            val result = ServiceLocator.recurringBillsRepository.getRecurringBills(householdId)

            if (result is AppResult.Success<*>) {
                @Suppress("UNCHECKED_CAST")
                val bills = result.data as? List<RecurringBill> ?: emptyList()

                val today      = LocalDate.now()
                val dayOfMonth = today.dayOfMonth

                val billsDue = bills.filter { bill: RecurringBill ->
                    if (!bill.isActive) return@filter false

                    // ¿Está pagada este mes?
                    val lastPaid = bill.lastPaidDate
                    if (lastPaid != null) {
                        val paidDate = try {
                            LocalDate.parse(lastPaid.take(10))
                        } catch (e: Exception) {
                            null
                        }
                        if (paidDate != null &&
                            paidDate.year == today.year &&
                            paidDate.monthValue == today.monthValue) {
                            return@filter false   // ya pagada este mes
                        }
                    }

                    // ¿Vence en los próximos DAYS_AHEAD días o ya venció?
                    val daysUntilDue = bill.dueDayOfMonth - dayOfMonth
                    daysUntilDue in -30..DAYS_AHEAD
                }

                billsDue.forEachIndexed { index, bill: RecurringBill ->
                    val daysUntilDue = bill.dueDayOfMonth - dayOfMonth
                    val message = when {
                        daysUntilDue < 0  -> "Vencida hace ${-daysUntilDue} día(s)"
                        daysUntilDue == 0 -> "Vence hoy"
                        daysUntilDue == 1 -> "Vence mañana"
                        else              -> "Vence en $daysUntilDue días (día ${bill.dueDayOfMonth})"
                    }
                    sendNotification(
                        id      = index + 1000,
                        title   = "💸 ${bill.name}",
                        message = message
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            // No fallar — si hay un error silencioso, reagendar en el próximo ciclo
            Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertas de cuentas recurrentes próximas a vencer"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }
}
