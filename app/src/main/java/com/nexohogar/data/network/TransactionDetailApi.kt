package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.AccountNameDto
import com.nexohogar.data.remote.dto.TransactionDetailDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * API para obtener detalles de una transacción.
 * Usa enfoque de dos llamadas separadas para evitar ambigüedad
 * de PostgREST cuando la tabla tiene múltiples FK al mismo padre.
 */
interface TransactionDetailApi {

    @GET("rest/v1/transactions")
    suspend fun getTransactionDetail(
        @Header("Authorization") token: String,
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<TransactionDetailDto>>

    @GET("rest/v1/accounts")
    suspend fun getAccountName(
        @Header("Authorization") token: String,
        @Query("id") idFilter: String,
        @Query("select") select: String = "id,name"
    ): Response<List<AccountNameDto>>
}