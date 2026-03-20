package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.Category

interface CategoriesRepository {
    suspend fun getCategories(householdId: String): AppResult<List<Category>>
    suspend fun createCategory(name: String, type: String, householdId: String): AppResult<Category>
}