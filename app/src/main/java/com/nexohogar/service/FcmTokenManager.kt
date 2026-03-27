package com.nexohogar.service

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.nexohogar.core.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Gestiona el registro y eliminación del token FCM en Supabase.
 *
 * - registerToken(): Llamar al hacer login o al iniciar la app con sesión activa
 * - unregisterToken(): Llamar al hacer logout
 */
object FcmTokenManager {

    private const val TAG = "FcmTokenManager"
    private const val PREF_KEY = "fcm_token"

    /**
     * Obtiene el token FCM actual y lo registra en Supabase.
     * Se ejecuta en background — no bloquea el hilo principal.
     */
    fun registerToken(context: Context, fcmToken: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = fcmToken ?: FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "Registrando token FCM en Supabase")

                // Guardar localmente para poder eliminarlo en logout
                context.getSharedPreferences("NexoHogarPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREF_KEY, token)
                    .apply()

                // Registrar en Supabase via RPC
                val deviceName = android.os.Build.MODEL
                ServiceLocator.fcmApi.upsertToken(
                    mapOf(
                        "p_token" to token,
                        "p_device_name" to deviceName
                    )
                )
                Log.d(TAG, "Token FCM registrado exitosamente")

            } catch (e: Exception) {
                Log.e(TAG, "Error registrando token FCM", e)
            }
        }
    }

    /**
     * Elimina el token FCM de Supabase. Llamar al hacer logout.
     */
    fun unregisterToken(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = context.getSharedPreferences("NexoHogarPrefs", Context.MODE_PRIVATE)
                    .getString(PREF_KEY, null)

                if (token != null) {
                    ServiceLocator.fcmApi.deleteToken(mapOf("p_token" to token))
                    Log.d(TAG, "Token FCM eliminado de Supabase")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error eliminando token FCM", e)
            }
        }
    }
}
