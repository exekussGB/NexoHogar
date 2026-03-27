package com.nexohogar.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.presentation.MainActivity

/**
 * Servicio que maneja mensajes entrantes de Firebase Cloud Messaging.
 *
 * - Muestra notificaciones cuando la app está en background/foreground
 * - Actualiza el token FCM en Supabase cuando cambia
 */
class NexoHogarMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM"
        const val CHANNEL_ID = "nexohogar_general"
        const val CHANNEL_NAME = "NexoHogar"
    }

    /**
     * Se llama cuando Firebase genera un nuevo token (primer inicio,
     * reinstalación, o rotación de token).
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM generado")

        // Registrar en Supabase si el usuario tiene sesión activa
        val session = ServiceLocator.sessionManager.fetchSession()
        if (session != null) {
            FcmTokenManager.registerToken(
                context = applicationContext,
                fcmToken = token
            )
        }
    }

    /**
     * Se llama cuando llega un mensaje (data o notification).
     * Siempre mostramos nuestra propia notificación para tener control total.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Mensaje recibido de: ${message.from}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "NexoHogar"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""

        if (body.isNotBlank()) {
            showNotification(title, body, message.data)
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Pasar datos para navegación al abrir la app
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Reemplazar con ícono propio
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones generales de NexoHogar"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
