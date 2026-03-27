package com.nexohogar.data.network

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interfaz Retrofit para gestionar tokens FCM en Supabase.
 * Usa las funciones RPC creadas en la migración.
 */
interface FcmApi {

    /**
     * Registra o actualiza el token FCM del dispositivo.
     * Body: { "p_token": "...", "p_device_name": "..." }
     */
    @POST("rest/v1/rpc/rpc_upsert_fcm_token")
    suspend fun upsertToken(@Body params: Map<String, String>)

    /**
     * Elimina el token FCM (al hacer logout).
     * Body: { "p_token": "..." }
     */
    @POST("rest/v1/rpc/rpc_delete_fcm_token")
    suspend fun deleteToken(@Body params: Map<String, String>)
}
