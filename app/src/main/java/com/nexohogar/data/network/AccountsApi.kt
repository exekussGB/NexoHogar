package com.nexohogar.data.network

import com.nexohogar.data.model.AccountDto
import com.nexohogar.data.model.CreateAccountRequest
import com.nexohogar.data.remote.dto.AccountBalanceViewDto
import com.nexohogar.data.remote.dto.AccountResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface AccountsApi {

    /**
     * Consulta la VISTA account_balances para obtener saldos reales
     * (initial_balance_clp + movimientos de transaction_entries).
     * householdId debe pasarse con prefijo "eq." → "eq.{uuid}"
     */
    @GET("rest/v1/account_balances")
    suspend fun getBalances(
        @Query("household_id") householdId: String,
        @Query("select")       select: String = "account_id,name,account_type,balance_clp",
        @Query("order")        order: String  = "name.asc"
    ): List<AccountBalanceViewDto>

    /**
     * Obtiene cuentas del hogar (sin balance calculado, solo metadata).
     * Usado únicamente para el diálogo de selección de cuenta.
     */
    @GET("rest/v1/accounts")
    suspend fun getAccounts(
        @Query("household_id") householdId: String,
        @Query("select")       select: String = "*",
        @Query("order")        order: String  = "name.asc"
    ): List<AccountDto>

    /**
     * Crea una nueva cuenta.
     * account_type debe ser UPPERCASE: ASSET, LIABILITY, INCOME, EXPENSE
     * currency_code es obligatorio: "CLP"
     */
    @Headers("Prefer: return=representation")
    @POST("rest/v1/accounts")
    suspend fun createAccount(
        @Body request: CreateAccountRequest
    ): List<AccountResponse>
}
