package com.nexohogar.service

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.core.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Gestiona el registro y eliminación del token FCM en Supabase.
 *
 * OTH-06: Corregido para usar un CoroutineScope con SupervisorJob que puede
 * cancelarse al hacer logout, evitando coroutines huérfanas.
 *
 * RED-04 (parcial): Usa SessionManager (cifrado) para almacenar el token FCM
 * en lugar de SharedPreferences planas.
 *
 * SEC-04: Logs condicionados a DEBUG via AppLogger.
 */
object FcmTokenManager {

    private const val TAG = "FcmTokenManager"
    private const val PREF_KEY = "fcm_token"

    /**
     * Scope con SupervisorJob: si una coroutine falla, no cancela las demás.
     * Se puede cancelar en logout con [cancelScope].
     */
    private var job = SupervisorJob()
    private var scope = CoroutineScope(Dispatchers.IO + job)

    fun registerToken(context: Context, fcmToken: String? = null) {
        scope.launch {
            try {
                val token = fcmToken ?: FirebaseMessaging.getInstance().token.await()
                AppLogger.d(TAG, "Registrando token FCM en Supabase")

                // RED-04: Usar SessionManager (cifrado) en lugar de SharedPreferences planas
                ServiceLocator.sessionManager.saveExtra(PREF_KEY, token)

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
        scope.launch {
            try {
                val token = ServiceLocator.sessionManager.getExtra(PREF_KEY)

                if (token != null) {
                    ServiceLocator.fcmApi.deleteToken(mapOf("p_token" to token))
                    ServiceLocator.sessionManager.removeExtra(PREF_KEY)
                    AppLogger.d(TAG, "Token FCM eliminado de Supabase")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error eliminando token FCM", e)
            }
        }
    }

    /**
     * Cancela las coroutines pendientes (llamar en logout).
     * Después de cancelar, crea un nuevo scope para operaciones futuras.
     */
    fun cancelScope() {
        job.cancel()
        job = SupervisorJob()
        scope = CoroutineScope(Dispatchers.IO + job)
    }
}
