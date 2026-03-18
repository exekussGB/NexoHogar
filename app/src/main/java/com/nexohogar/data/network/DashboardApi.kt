package com.nexohogar.data.network

import com.nexohogar.data.model.DashboardDto
import com.nexohogar.data.model.MonthlyTrendDto
import com.nexohogar.data.remote.dto.MonthlyBalanceDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Interface Retrofit para obtener datos del Dashboard desde vistas de Supabase.
 */
interface DashboardApi {

    @GET("rest/v1/v_dashboard")
    suspend fun getDashboardSummary(
        @Query("household_id") householdFilter: String
    ): Response<List<DashboardDto>>

    @GET("rest/v1/v_monthly_summary")
    suspend fun getMonthlyTrends(
        @Query("household_id") householdFilter: String,
        @Query("limit") limit: Int = 6
    ): Response<List<MonthlyTrendDto>>

    @POST("rpc/rpc_monthly_balance")
    suspend fun getMonthlyBalance(
        @Body body: Map<String, String>
    ): List<MonthlyBalanceDto>
}
