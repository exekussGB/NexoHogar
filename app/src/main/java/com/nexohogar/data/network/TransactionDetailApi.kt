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
 *
 * ✅ CAMBIO: Usar v_transactions_with_user en lugar de transactions
 *    para obtener created_by_name directamente desde la vista.
 */
interface TransactionDetailApi {

    // ✅ CAMBIO: "rest/v1/transactions" → "rest/v1/v_transactions_with_user"
    @GET("rest/v1/v_transactions_with_user")
    suspend fun getTransactionDetail(
        @Query("id")     idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<TransactionDetailDto>>
    @GET("rest/v1/v_transactions_with_user")
    suspend fun getTransactionsByAccount(
        @Query("household_id") householdIdFilter: String,
        @Query("account_id") accountIdFilter: String,
        @Query("limit") limit: Int = 10,
        @Query("order") order: String = "transaction_date.desc",
        @Query("select") select: String = "id,type,description,transaction_date,amount_clp,status,account_id,to_account_id"
    ): Response<List<TransactionDto>>
    @GET("rest/v1/accounts")
    suspend fun getAccountName(
        @Query("id")     idFilter: String,
        @Query("select") select: String = "id,name"
    ): Response<List<AccountNameDto>>
}
