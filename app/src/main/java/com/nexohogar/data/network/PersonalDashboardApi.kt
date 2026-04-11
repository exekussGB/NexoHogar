package com.nexohogar.data.network

import com.nexohogar.data.remote.dto.PersonalDashboardDto
import com.nexohogar.data.remote.dto.MonthlyBalanceDto
import com.nexohogar.data.remote.dto.PersonalTransactionDto
import com.nexohogar.data.remote.dto.PersonalTransactionsRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PersonalDashboardApi {

    @POST("rpc/rpc_personal_dashboard_summary")
    suspend fun getPersonalSummary(
        @Body body: Map<String, String>
    ): Response<List<PersonalDashboardDto>>

    @POST("rpc/rpc_personal_monthly_balance")
    suspend fun getPersonalMonthlyBalance(
        @Body body: Map<String, String>
    ): Response<List<MonthlyBalanceDto>>

    @POST("rpc/rpc_personal_recent_transactions")
    suspend fun getPersonalRecentTransactions(
        @Body body: PersonalTransactionsRequest
    ): Response<List<PersonalTransactionDto>>
}