package com.nexohogar.data.network

import com.nexohogar.data.model.TransactionDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for fetching transaction details from Supabase.
 */
interface TransactionDetailApi {

    @GET("transactions")
    suspend fun getTransactionDetail(
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<TransactionDto>>
}
