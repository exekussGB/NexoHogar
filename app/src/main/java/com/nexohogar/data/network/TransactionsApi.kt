package com.nexohogar.data.network

import com.nexohogar.data.model.TransactionDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for transaction-related operations.
 */
interface TransactionsApi {

    @GET("rest/v1/v_transactions")
    suspend fun getTransactions(
        @Query("household_id") householdIdFilter: String,
        @Query("order") order: String = "transaction_date.desc"
    ): Response<List<TransactionDto>>
}
