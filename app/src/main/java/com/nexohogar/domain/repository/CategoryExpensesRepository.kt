package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.CategoryExpenseGroup

interface CategoryExpensesRepository {
    suspend fun getCategoryExpenses(householdId: String, months: Int = 1): AppResult<List<CategoryExpenseGroup>>
}
