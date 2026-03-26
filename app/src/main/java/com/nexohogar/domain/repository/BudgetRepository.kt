package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.BudgetItem

interface BudgetRepository {
    suspend fun getBudgetConsumption(
        householdId : String,
        year        : Int,
        month       : Int
    ): AppResult<List<BudgetItem>>
}
