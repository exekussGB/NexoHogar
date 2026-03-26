package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.CategoryExpense

interface CategoryExpensesRepository {
    suspend fun getCategoryExpenses(householdId: String): AppResult<List<CategoryExpense>>
}
