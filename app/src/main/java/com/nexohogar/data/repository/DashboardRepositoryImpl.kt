package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.model.toDomain
import com.nexohogar.data.network.DashboardApi
import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.repository.DashboardRepository

/**
 * Implementation of DashboardRepository that fetches data from Supabase.
 */
class DashboardRepositoryImpl(
    private val api: DashboardApi
) : DashboardRepository {

    override suspend fun getDashboardSummary(householdId: String): AppResult<DashboardSummary> {
        return try {
            // Supabase filter format: eq.{value}
            val filter = "eq.$householdId"
            val response = api.getDashboardSummary(filter)
            
            if (response.isSuccessful) {
                val list = response.body()
                if (!list.isNullOrEmpty()) {
                    AppResult.Success(list.first().toDomain())
                } else {
                    AppResult.Error("No dashboard data found for this household")
                }
            } else {
                AppResult.Error("Error fetching dashboard: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error("Network failure: ${e.message}")
        }
    }
}
