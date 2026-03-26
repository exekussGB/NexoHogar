package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.CategoryExpensesApi
import com.nexohogar.domain.model.CategoryExpenseByUser
import com.nexohogar.domain.model.CategoryExpenseGroup
import com.nexohogar.domain.repository.CategoryExpensesRepository

class CategoryExpensesRepositoryImpl(
    private val api: CategoryExpensesApi
) : CategoryExpensesRepository {

    override suspend fun getCategoryExpenses(
        householdId: String,
        months: Int
    ): AppResult<List<CategoryExpenseGroup>> {
        return try {
            val body = hashMapOf<String, Any>(
                "p_household_id" to householdId,
                "p_months" to months
            )
            val response = api.getCategoryExpenses(body)
            if (response.isSuccessful) {
                val data = response.body() ?: emptyList()

                // Group flat rows by category_name
                val grouped = data
                    .groupBy { it.categoryName }
                    .map { (catName, rows) ->
                        val catTotal = rows.sumOf { it.totalAmount }
                        val catPct = rows.sumOf { it.percentage }
                        val users = rows.map { dto ->
                            CategoryExpenseByUser(
                                categoryName = dto.categoryName,
                                userId = dto.userId,
                                userName = dto.userName ?: "Desconocido",
                                totalAmount = dto.totalAmount,
                                percentage = dto.percentage
                            )
                        }
                        CategoryExpenseGroup(
                            categoryName = catName,
                            totalAmount = catTotal,
                            percentage = catPct,
                            users = users
                        )
                    }
                    .sortedByDescending { it.totalAmount }

                AppResult.Success(grouped)
            } else {
                AppResult.Error("Error ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error desconocido")
        }
    }
}
