package com.nexohogar.data.network

import com.nexohogar.data.model.AccountBalanceDto
import com.nexohogar.data.model.AccountDto
import com.nexohogar.data.model.CreateAccountRequest
import com.nexohogar.data.remote.dto.AccountResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface AccountsApi {

    /**
     * Obtiene las cuentas activas (no eliminadas) de un hogar.
     * householdId debe pasarse con prefijo "eq." → "eq.{uuid}"
     */
    @GET("rest/v1/accounts")
    suspend fun getAccounts(
        @Query("household_id") householdId: String,
        @Query("is_deleted")   isDeleted: String = "eq.false",
        @Query("select")       select: String = "*",
        @Query("order")        order: String = "name.asc"
    ): List<AccountDto>

    /**
     * Crea una nueva cuenta.
     */
    @Headers("Prefer: return=representation")
    @POST("rest/v1/accounts")
    suspend fun createAccount(
        @Body request: CreateAccountRequest
    ): List<AccountResponse>

    /**
     * Soft-delete de cuenta vía RPC.
     */
    @POST("rest/v1/rpc/rpc_delete_account")
    suspend fun deleteAccount(
        @Body body: Map<String, String>
    ): Response<Unit>

    /**
     * Saldos calculados desde transacciones reales.
     */
    @POST("rest/v1/rpc/get_calculated_balances")
    suspend fun getCalculatedBalances(
        @Body body: Map<String, String>
    ): List<AccountBalanceDto>
}