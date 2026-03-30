package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.DashboardDto
import com.nexohogar.data.remote.dto.AccountBalanceViewDto
import com.nexohogar.data.remote.dto.MonthlyBalanceDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface DashboardApi {

    /** Resumen del dashboard desde la vista v_dashboard. */
    @GET("rest/v1/v_dashboard")
    suspend fun getDashboardSummary(
        @Query("household_id") householdFilter: String
    ): Response<List<DashboardDto>>

    /** Tendencia mensual desde la RPC rpc_monthly_balance. */
    @POST("rest/v1/rpc/rpc_monthly_balance")
    suspend fun getMonthlyBalance(
        @Body body: Map<String, String>
    ): Response<List<MonthlyBalanceDto>>

    /**
     * Saldos de cuentas desde la VISTA account_balances.
     * balance_clp = saldo real calculado (initial + movimientos).
     * householdId debe pasarse con prefijo "eq." → "eq.{uuid}"
     */
    @GET("rest/v1/account_balances")
    suspend fun getAccountBalances(
        @Query("household_id") householdId: String,
        @Query("select")       select: String = "account_id,name,account_type,balance_clp",
        @Query("order")        order: String  = "name.asc"
    ): Response<List<AccountBalanceViewDto>>
}
