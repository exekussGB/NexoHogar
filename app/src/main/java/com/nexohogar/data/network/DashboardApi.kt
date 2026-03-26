package com.nexohogar.data.network

import com.nexohogar.data.model.AccountBalanceDto
import com.nexohogar.data.model.DashboardDto
import com.nexohogar.data.model.DualDashboardDto
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

    @POST("rest/v1/rpc/rpc_monthly_balance")
    suspend fun getMonthlyBalance(
        @Body body: Map<String, String>
    ): Response<List<MonthlyBalanceDto>>

    @POST("rest/v1/rpc/get_account_balances_v2")
    suspend fun getAccountBalancesV2(
        @Body body: Map<String, String>
    ): Response<List<AccountBalanceDto>>

    @POST("rest/v1/rpc/rpc_dashboard_dual")
    suspend fun getDualDashboard(
        @Body body: Map<String, String>
    ): Response<List<DualDashboardDto>>
}
