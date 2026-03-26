package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.model.ExpenseByCategoryDto
import com.nexohogar.domain.model.Budget

interface BudgetsRepository {
    suspend fun getBudgets(householdId: String): AppResult<List<Budget>>
    suspend fun createBudget(householdId: String, categoryId: String, amountClp: Long, period: String = "monthly"): AppResult<Budget>
    suspend fun deleteBudget(budgetId: String): AppResult<Unit>
    suspend fun getExpensesByCategory(householdId: String, year: Int?, month: Int?, userId: String? = null): AppResult<List<ExpenseByCategoryDto>>
}
