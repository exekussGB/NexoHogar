package com.nexohogar.data.network

import com.nexohogar.data.model.AccountBalanceDto
import com.nexohogar.data.model.DashboardDto
import com.nexohogar.data.remote.dto.MonthlyBalanceDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface DashboardApi {

    @GET("rest/v1/v_dashboard")
    suspend fun getDashboardSummary(
        @Query("household_id") householdFilter: String
    ): Response<List<DashboardDto>>

    // FIX: path corregido de "rpc/rpc_monthly_balance" → "rest/v1/rpc/rpc_monthly_balance"
    @POST("rest/v1/rpc/rpc_monthly_balance")
    suspend fun getMonthlyBalance(
        @Body body: Map<String, String>
    ): Response<List<MonthlyBalanceDto>>

    // NUEVO: saldos de cuentas para el dashboard
    @GET("rest/v1/rpc/get_account_balances")
    suspend fun getAccountBalances(
        @Query("p_household_id") householdId: String
    ): Response<List<AccountBalanceDto>>
    // Saldos calculados desde transacciones reales
    @POST("rest/v1/rpc/get_calculated_balances")
    suspend fun getCalculatedBalances(
        @Body body: Map<String, String>
    ): Response<List<AccountBalanceDto>>
}
