package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.model.MonthlyBalance

/**
 * Interface for the dashboard repository.
 */
interface DashboardRepository {
    suspend fun getDashboardSummary(householdId: String): AppResult<DashboardSummary>
    suspend fun getMonthlyBalance(householdId: String): AppResult<List<MonthlyBalance>>
}
