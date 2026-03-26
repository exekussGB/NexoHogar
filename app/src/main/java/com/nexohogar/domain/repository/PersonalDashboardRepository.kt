package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.MonthlyBalance
import com.nexohogar.domain.model.PersonalDashboardSummary
import com.nexohogar.domain.model.Transaction

interface PersonalDashboardRepository {
    suspend fun getPersonalSummary(householdId: String, userId: String): AppResult<PersonalDashboardSummary>
    suspend fun getPersonalMonthlyBalance(householdId: String, userId: String): AppResult<List<MonthlyBalance>>
    suspend fun getPersonalRecentTransactions(householdId: String, userId: String, limit: Int = 5): AppResult<List<Transaction>>
}
