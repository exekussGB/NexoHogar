package com.nexohogar.domain.repository

import com.nexohogar.domain.model.AppResult
import com.nexohogar.domain.model.Budget
import com.nexohogar.domain.model.BudgetConsumption

interface BudgetRepository {

    suspend fun getBudgets(
        householdId: String,
        yearNum: Int,
        monthNum: Int
    ): AppResult<List<Budget>>

    suspend fun getBudgetConsumption(
        householdId: String,
        yearNum: Int,
        monthNum: Int
    ): AppResult<List<BudgetConsumption>>

    suspend fun createBudget(
        householdId: String,
        categoryId: String,
        amountClp: Double,
        periodType: String,
        yearNum: Int,
        monthNum: Int?,
        weekNum: Int?,
        memberId: String?
    ): AppResult<Budget>

    suspend fun updateBudget(
        budgetId: String,
        amountClp: Double
    ): AppResult<Unit>

    suspend fun deleteBudget(
        budgetId: String
    ): AppResult<Unit>
}