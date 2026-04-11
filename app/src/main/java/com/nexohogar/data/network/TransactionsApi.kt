package com.nexohogar.data.network

import com.google.gson.JsonElement
import com.nexohogar.data.remote.dto.CreateTransactionRequest
import com.nexohogar.data.remote.dto.CreateTransferRequest
import com.nexohogar.data.remote.dto.TransactionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TransactionsApi {

    @GET("v_transactions_with_user")
    suspend fun getTransactions(
        @Query("household_id") householdFilter: String,
        @Query("select")       select: String = "*",
        @Query("limit")        limit: Int = 30,
        @Query("offset")       offset: Int = 0,
        @Query("order")        order: String = "created_at.desc"
    ): Response<List<TransactionResponse>>

    @POST("rpc/rpc_create_transaction")
    suspend fun createTransaction(
        @Body request: CreateTransactionRequest
    ): Response<Unit>

    @POST("rpc/rpc_transfer")
    suspend fun createTransfer(
        @Body request: CreateTransferRequest
    ): Response<JsonElement>
}
