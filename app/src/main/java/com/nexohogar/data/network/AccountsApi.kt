package com.nexohogar.data.network

import com.nexohogar.data.model.AccountBalanceDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface Retrofit para obtener balances de cuentas desde Supabase.
 */
interface AccountsApi {

    @GET("rest/v1/v_account_balances")
    suspend fun getAccountBalances(
        @Query("household_id") householdIdFilter: String
    ): Response<List<AccountBalanceDto>>
}
