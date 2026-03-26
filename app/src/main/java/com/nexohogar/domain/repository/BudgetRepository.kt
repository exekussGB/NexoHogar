package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.BudgetItem

interface BudgetRepository {
    suspend fun getBudgetConsumption(
        householdId : String
    ): AppResult<List<BudgetItem>>

    suspend fun createBudget(
        householdId  : String,
        categoryName : String,
        amountClp    : Long
    ): AppResult<Unit>

    suspend fun updateBudget(
        budgetId  : String,
        amountClp : Long
    ): AppResult<Unit>

    suspend fun deleteBudget(budgetId: String): AppResult<Unit>
}
