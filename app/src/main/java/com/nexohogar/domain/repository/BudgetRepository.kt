package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.BudgetItem

interface BudgetRepository {
    suspend fun getBudgetConsumption(
        householdId : String,
        year        : Int,
        month       : Int
    ): AppResult<List<BudgetItem>>

    suspend fun createBudget(
        householdId : String,
        categoryId  : String,
        amountClp   : Long,
        year        : Int,
        month       : Int,
        memberId    : String?
    ): AppResult<Unit>

    suspend fun updateBudget(
        budgetId  : String,
        amountClp : Long
    ): AppResult<Unit>

    suspend fun deleteBudget(budgetId: String): AppResult<Unit>
}
