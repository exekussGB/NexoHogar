package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.mapper.toDomain
import com.nexohogar.data.network.CategoriesApi
import com.nexohogar.domain.model.Category
import com.nexohogar.domain.repository.CategoriesRepository

/**
 * Implementación del repositorio de categorías.
 */
class CategoriesRepositoryImpl(
    private val api: CategoriesApi
) : CategoriesRepository {

    override suspend fun getCategories(householdId: String): AppResult<List<Category>> {
        return try {
            val response = api.getCategories(householdFilter = "eq.$householdId")
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                AppResult.Success(body.toDomain())
            } else {
                AppResult.Error("Error al obtener categorías")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error de red")
        }
    }
}
