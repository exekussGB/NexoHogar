package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.CategoryExpensesApi
import com.nexohogar.domain.model.CategoryExpense
import com.nexohogar.domain.repository.CategoryExpensesRepository

class CategoryExpensesRepositoryImpl(
    private val api: CategoryExpensesApi
) : CategoryExpensesRepository {

    override suspend fun getCategoryExpenses(householdId: String): AppResult<List<CategoryExpense>> {
        return try {
            val response = api.getCategoryExpenses(mapOf("p_household_id" to householdId))
            if (response.isSuccessful) {
                val data = response.body() ?: emptyList()
                AppResult.Success(data.map {
                    CategoryExpense(
                        categoryName = it.categoryName,
                        totalAmount  = it.totalAmount,
                        percentage   = it.percentage
                    )
                })
            } else {
                AppResult.Error("Error ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }
}
