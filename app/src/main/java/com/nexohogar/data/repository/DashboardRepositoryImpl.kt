package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.model.toDomain
import com.nexohogar.data.network.DashboardApi
import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.model.MonthlyBalance
import com.nexohogar.domain.repository.DashboardRepository

/**
 * Implementation of DashboardRepository that fetches data from Supabase.
 */
class DashboardRepositoryImpl(
    private val dashboardApi: DashboardApi
) : DashboardRepository {

    override suspend fun getDashboardSummary(householdId: String): AppResult<DashboardSummary> {
        return try {
            // Supabase filter format: eq.{value}
            val filter = "eq.$householdId"
            val response = dashboardApi.getDashboardSummary(filter)
            
            if (response.isSuccessful) {
                val list = response.body()
                if (!list.isNullOrEmpty()) {
                    val dto = list.first()
                    AppResult.Success(dto.toDomain())
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

    override suspend fun getMonthlyBalance(householdId: String): AppResult<List<MonthlyBalance>> {
        return try {
            val response = dashboardApi.getMonthlyBalance(
                mapOf("p_household_id" to householdId)
            )
            val domain = response.map { dto ->
                MonthlyBalance(
                    yearNum  = dto.yearNum,
                    monthNum = dto.monthNum,
                    income   = dto.income,
                    expense  = dto.expense,
                    net      = dto.net
                )
            }
            AppResult.Success(domain)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al cargar tendencia mensual")
        }
    }
}
