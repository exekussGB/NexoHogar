package com.nexohogar.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nexohogar.R
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.core.util.AppLogger
import com.nexohogar.presentation.MainActivity

/**
 * Servicio FCM. SEC-04: Logs via AppLogger (solo DEBUG).
 */
class NexoHogarMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM"

        const val CHANNEL_GENERAL   = "nexohogar_general"
        const val CHANNEL_HOUSEHOLD = "nexohogar_household"
        const val CHANNEL_BILLS     = "nexohogar_bills"
        const val CHANNEL_BUDGET    = "nexohogar_budget"
        const val CHANNEL_INVENTORY = "nexohogar_inventory"

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)

                val channels = listOf(
                    NotificationChannel(
                        CHANNEL_GENERAL, "General",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply { description = "Notificaciones generales" },

                    NotificationChannel(
                        CHANNEL_HOUSEHOLD, "Hogar y Miembros",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply { description = "Solicitudes de unión y gestión de miembros" },

                    NotificationChannel(
                        CHANNEL_BILLS, "Cuentas Recurrentes",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply { description = "Alertas de vencimiento de cuentas" },

                    NotificationChannel(
                        CHANNEL_BUDGET, "Presupuestos",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply { description = "Alertas de presupuesto al límite" },

                    NotificationChannel(
                        CHANNEL_INVENTORY, "Inventario",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply { description = "Alertas de stock bajo" }
                )

                channels.forEach { manager.createNotificationChannel(it) }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AppLogger.d(TAG, "Nuevo token FCM generado")

        val session = ServiceLocator.sessionManager.fetchSession()
        if (session != null) {
            FcmTokenManager.registerToken(
                context = applicationContext,
                fcmToken = token
            )
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        AppLogger.d(TAG, "Mensaje recibido de: ${message.from}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "NexoHogar"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""

        val type = message.data["type"] ?: "general"

        if (body.isNotBlank()) {
            val notifPrefs = ServiceLocator.notificationPreferences
            if (!notifPrefs.isTypeEnabled(type)) {
                AppLogger.d(TAG, "Notificación tipo '$type' deshabilitada por el usuario")
                return
            }

            val channelId = when (type) {
                "member_request", "member_decision", "household_join" -> CHANNEL_HOUSEHOLD
                "recurring_bill" -> CHANNEL_BILLS
                "budget_alert" -> CHANNEL_BUDGET
                "low_stock" -> CHANNEL_INVENTORY
                else -> CHANNEL_GENERAL
            }
            showNotification(title, body, channelId, message.data)
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        data: Map<String, String>
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = when (channelId) {
            CHANNEL_HOUSEHOLD -> android.R.drawable.ic_dialog_info
            CHANNEL_BILLS     -> android.R.drawable.ic_dialog_alert
            CHANNEL_BUDGET    -> android.R.drawable.ic_dialog_info
            CHANNEL_INVENTORY -> android.R.drawable.ic_dialog_alert
            else              -> android.R.drawable.ic_dialog_info
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
