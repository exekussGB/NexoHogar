package com.nexohogar.data.network

import com.nexohogar.data.model.AccountBalanceDto   // ← CORRECTO: data.model
import com.nexohogar.data.model.AccountDto
import com.nexohogar.data.remote.dto.AccountResponse
import com.nexohogar.data.remote.dto.CreateAccountRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * API de cuentas. Sin @Header(Authorization) porque AuthInterceptor
 * lo inyecta automáticamente en todas las requests via OkHttpClient.
 */
interface AccountsApi {

    @GET("rest/v1/accounts")
    suspend fun getAccounts(
        @Query("household_id") householdId: String,
        @Query("select") select: String = "*",
        @Query("is_active") isActive: String = "eq.true"
    ): List<AccountDto>

    @GET("rest/v1/rpc/get_account_balances")
    suspend fun getAccountBalances(
        @Query("p_household_id") householdId: String
    ): List<AccountBalanceDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/accounts")
    suspend fun createAccount(
        @Body request: CreateAccountRequest
    ): List<AccountResponse>
}