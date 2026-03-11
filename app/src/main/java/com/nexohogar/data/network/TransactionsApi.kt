package com.nexohogar.data.network

import com.nexohogar.data.model.CreateTransactionRequest
import com.nexohogar.data.remote.dto.TransactionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for transaction-related operations.
 */
interface TransactionsApi {

    @GET("rest/v1/transactions")
    suspend fun getTransactions(
        @Query("household_id") householdId: String,
        @Query("select") select: String = "id,description,amount_clp,created_at,account_id,household_id,type",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<TransactionResponse>>

    @Headers(
        "Content-Type: application/json",
        "Prefer: params=single-object"
    )
    @POST("rest/v1/rpc/create_transaction")
    suspend fun createTransaction(
        @Body request: CreateTransactionRequest
    ): Response<Unit>
}
