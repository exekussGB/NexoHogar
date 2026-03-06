package com.nexohogar.data.network

import com.nexohogar.data.model.TransactionEntryDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for fetching transaction entry details from Supabase.
 */
interface TransactionDetailApi {

    @GET("rest/v1/v_transaction_entries")
    suspend fun getTransactionEntries(
        @Query("transaction_id") transactionIdFilter: String
    ): Response<List<TransactionEntryDto>>
}
