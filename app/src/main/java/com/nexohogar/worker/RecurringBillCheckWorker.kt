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

/**
 * Worker que consulta facturas próximas a vencer y emite notificación con acción rápida.
 * Se ejecuta diariamente a las 9:00 AM (programado desde NotificationScheduler).
 *
 * Notifica 2 días antes del vencimiento con acción "Ya pagué".
 */
class RecurringBillCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "RecurringBillCheck"
        const val CHANNEL_ID = "nexohogar_bills"
        const val DAYS_AHEAD = 2
    }

    override suspend fun doWork(): Result {
        return try {
            if (!ServiceLocator.notificationPreferences.billsEnabled) {
                return Result.success()
            }

            createNotificationChannel()

            val householdId = ServiceLocator.sessionManager.fetchSelectedHouseholdId()
                ?: return Result.success()

            val result = ServiceLocator.recurringBillsRepository.getRecurringBills(householdId)

            when (result) {
                is AppResult.Success -> {
                    val bills = result.data
                    val today = LocalDate.now()
                    val dayOfMonth = today.dayOfMonth

                    val billsDue = bills.filter { bill: RecurringBill ->
                        if (!bill.isActive) return@filter false

                        // ¿Está pagada este mes?
                        val lastPaid = bill.lastPaidDate
                        if (lastPaid != null) {
                            val paidDate = try {
                                LocalDate.parse(lastPaid.take(10))
                            } catch (e: Exception) { null }
                            if (paidDate != null &&
                                paidDate.year == today.year &&
                                paidDate.monthValue == today.monthValue) {
                                return@filter false
                            }
                        }

                        // Vence exactamente en DAYS_AHEAD días o menos (sin contar vencidas)
                        val daysUntilDue = bill.dueDayOfMonth - dayOfMonth
                        daysUntilDue in 0..DAYS_AHEAD
                    }

                    billsDue.forEachIndexed { index, bill ->
                        val daysUntilDue = bill.dueDayOfMonth - dayOfMonth
                        val message = when (daysUntilDue) {
                            0 -> "Vence hoy — $%,d".format(bill.amountClp)
                            1 -> "Vence mañana — $%,d".format(bill.amountClp)
                            else -> "Vence en $daysUntilDue días (día ${bill.dueDayOfMonth}) — $%,d".format(bill.amountClp)
                        }
                        sendNotificationWithAction(
                            id = index + 2000,
                            title = "⏰ ${bill.name}",
                            message = message,
                            billId = bill.id
                        )
                    }
                }
                else -> { /* error silencioso */ }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cuentas Recurrentes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de vencimiento de cuentas con acción rápida"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendNotificationWithAction(id: Int, title: String, message: String, billId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción rápida "Ya pagué"
        val markPaidIntent = Intent("com.nexohogar.ACTION_MARK_BILL_PAID").apply {
            putExtra("bill_id", billId)
            setPackage(context.packageName)
        }
        val markPaidPending = PendingIntent.getBroadcast(
            context, billId.hashCode(), markPaidIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(0, "✓ Ya pagué", markPaidPending)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }
}
