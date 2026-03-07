package com.nexohogar.data.network

import com.nexohogar.data.model.CreateTransactionRequest
import com.nexohogar.data.model.TransactionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for transaction-related operations.
 */
interface TransactionsApi {

    @GET("rest/v1/v_movements")
    suspend fun getTransactions(
        @Query("household_id") householdIdFilter: String,
        @Query("order") order: String = "transaction_date.desc"
    ): Response<List<TransactionDto>>

    @POST("rest/v1/rpc/create_transaction_v1")
    suspend fun createTransaction(
        @Body request: CreateTransactionRequest
    ): Response<Unit>
}
