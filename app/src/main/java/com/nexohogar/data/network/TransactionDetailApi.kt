package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.AccountNameDto
import com.nexohogar.data.remote.dto.TransactionDetailDto
import com.nexohogar.data.remote.dto.TransactionResponse
import com.nexohogar.data.remote.dto.UpdateTransactionRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TransactionDetailApi {

    @GET("v_transactions_with_user")
    suspend fun getTransactionDetail(
        @Query("id")     idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<TransactionDetailDto>>

    @GET("v_transactions_with_user")
    suspend fun getTransactionsByAccount(
        @Query("household_id") householdIdFilter: String,
        @Query("account_id")   accountIdFilter: String,
        @Query("limit")        limit: Int = 10,
        @Query("order")        order: String = "transaction_date.desc",
        @Query("select")       select: String = "id,type,description,transaction_date,amount_clp,status,account_id,to_account_id"
    ): Response<List<TransactionResponse>>

    @GET("accounts")
    suspend fun getAccountName(
        @Query("id")     idFilter: String,
        @Query("select") select: String = "id,name"
    ): Response<List<AccountNameDto>>

    @POST("rpc/rpc_update_transaction")
    suspend fun updateTransaction(
        @Body request: UpdateTransactionRequest
    ): Response<Unit>
}
