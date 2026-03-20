package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.model.MonthlyBalance

interface DashboardRepository {
    suspend fun getDashboardSummary(householdId: String): AppResult<DashboardSummary>
    suspend fun getMonthlyBalance(householdId: String): AppResult<List<MonthlyBalance>>
    suspend fun getAccountBalances(householdId: String): AppResult<List<AccountBalance>>
}