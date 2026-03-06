package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.DashboardSummary

/**
 * Interface for the dashboard repository.
 */
interface DashboardRepository {
    suspend fun getDashboardSummary(householdId: String): AppResult<DashboardSummary>
}
