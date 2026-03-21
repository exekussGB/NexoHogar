package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.AccountNameDto
import com.nexohogar.data.remote.dto.TransactionDetailDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * AuthInterceptor agrega apikey + Bearer token automáticamente.
 * No se necesita @Header("Authorization") explícito aquí.
 * IMPORTANTE: El path DEBE incluir "rest/v1/" para que Supabase lo resuelva.
 */
interface TransactionDetailApi {

    @GET("rest/v1/transactions")
    suspend fun getTransactionDetail(
        @Query("id")     idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<TransactionDetailDto>>

    @GET("rest/v1/accounts")
    suspend fun getAccountName(
        @Query("id")     idFilter: String,
        @Query("select") select: String = "id,name"
    ): Response<List<AccountNameDto>>
}