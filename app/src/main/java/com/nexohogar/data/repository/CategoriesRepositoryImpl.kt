package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.mapper.toDomain
import com.nexohogar.data.network.CategoriesApi
import com.nexohogar.data.remote.dto.CreateCategoryRequest
import com.nexohogar.domain.model.Category
import com.nexohogar.domain.repository.CategoriesRepository

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

    override suspend fun createCategory(
        name: String,
        type: String,
        householdId: String
    ): AppResult<Category> {
        return try {
            val request = CreateCategoryRequest(
                name = name,
                type = type,
                householdId = householdId
            )
            val response = api.createCategory(request = request)
            if (response.isSuccessful) {
                val body = response.body()?.firstOrNull()
                if (body != null) {
                    AppResult.Success(body.toDomain())
                } else {
                    AppResult.Error("No se recibió respuesta al crear categoría")
                }
            } else {
                AppResult.Error("Error al crear categoría: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error de red")
        }
    }
}