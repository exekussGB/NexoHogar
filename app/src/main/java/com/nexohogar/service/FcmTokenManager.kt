package com.nexohogar.service

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.core.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Gestiona el registro y eliminación del token FCM en Supabase.
 * SEC-04: Logs condicionados a DEBUG via AppLogger.
 */
object FcmTokenManager {

    private const val TAG = "FcmTokenManager"
    private const val PREF_KEY = "fcm_token"

    fun registerToken(context: Context, fcmToken: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = fcmToken ?: FirebaseMessaging.getInstance().token.await()
                AppLogger.d(TAG, "Registrando token FCM en Supabase")

                context.getSharedPreferences("NexoHogarPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREF_KEY, token)
                    .apply()

                val deviceName = android.os.Build.MODEL
                ServiceLocator.fcmApi.upsertToken(
                    mapOf(
                        "p_token" to token,
                        "p_device_name" to deviceName
                    )
                )
                AppLogger.d(TAG, "Token FCM registrado exitosamente")

            } catch (e: Exception) {
                AppLogger.e(TAG, "Error registrando token FCM", e)
            }
        }
    }

    fun unregisterToken(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = context.getSharedPreferences("NexoHogarPrefs", Context.MODE_PRIVATE)
                    .getString(PREF_KEY, null)

                if (token != null) {
                    ServiceLocator.fcmApi.deleteToken(mapOf("p_token" to token))
                    AppLogger.d(TAG, "Token FCM eliminado de Supabase")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error eliminando token FCM", e)
            }
        }
    }
}
