package com.nexohogar.data.network

import com.nexohogar.data.model.DashboardSummaryDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for dashboard related operations.
 */
interface DashboardApi {

    @GET("rest/v1/v_dashboard_summary")
    suspend fun getDashboardSummary(
        @Query("household_id") householdIdFilter: String
    ): Response<List<DashboardSummaryDto>>
}
