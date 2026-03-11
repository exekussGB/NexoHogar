package com.nexohogar.domain.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.Category

/**
 * Interfaz del repositorio para la gestión de categorías.
 */
interface CategoriesRepository {
    suspend fun getCategories(householdId: String): AppResult<List<Category>>
}
