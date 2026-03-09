package com.nexohogar.data.network

import com.nexohogar.data.model.AccountBalanceDto
import com.nexohogar.data.remote.dto.AccountResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface Retrofit para obtener información de cuentas desde Supabase.
 */
interface AccountsApi {

    @GET("rest/v1/v_account_balances")
    suspend fun getAccountBalances(
        @Query("household_id") householdIdFilter: String
    ): Response<List<AccountBalanceDto>>

    /**
     * Obtiene la lista de cuentas filtrada por household.
     * Se sincroniza con el DTO AccountResponse y el modelo de dominio Account.
     */
    @GET("rest/v1/accounts")
    suspend fun getAccounts(
        @Query("household_id") householdFilter: String,
        @Query("is_active") activeFilter: String = "eq.true",
        @Query("select") select: String = "id,name,account_type,balance,household_id",
        @Query("order") order: String = "name.asc"
    ): Response<List<AccountResponse>>
}
