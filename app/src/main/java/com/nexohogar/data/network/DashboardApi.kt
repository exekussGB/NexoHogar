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

    @GET("v_dashboard")
    suspend fun getDashboardSummary(
        @Query("household_id") householdFilter: String
    ): Response<List<DashboardDto>>

    @POST("rpc/rpc_monthly_balance")
    suspend fun getMonthlyBalance(
        @Body body: Map<String, String>
    ): Response<List<MonthlyBalanceDto>>

    @GET("account_balances")
    suspend fun getAccountBalances(
        @Query("household_id") householdId: String,
        @Query("select")       select: String = "account_id,name,account_type,balance_clp",
        @Query("order")        order: String  = "name.asc"
    ): Response<List<AccountBalanceViewDto>>
}
