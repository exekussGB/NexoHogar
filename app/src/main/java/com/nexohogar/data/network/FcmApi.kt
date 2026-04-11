package com.nexohogar.data.network

import retrofit2.http.Body
import retrofit2.http.POST

interface FcmApi {

    @POST("rpc/rpc_upsert_fcm_token")
    suspend fun upsertToken(@Body params: Map<String, String>)

    @POST("rpc/rpc_delete_fcm_token")
    suspend fun deleteToken(@Body params: Map<String, String>)
}
