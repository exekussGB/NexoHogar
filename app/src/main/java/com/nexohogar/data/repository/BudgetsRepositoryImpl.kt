package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.data.model.CreateBudgetRequest
import com.nexohogar.data.model.ExpenseByCategoryDto
import com.nexohogar.data.network.BudgetsApi
import com.nexohogar.data.network.CategoriesApi
import com.nexohogar.data.remote.dto.CategoryResponse
import com.nexohogar.domain.model.Budget
import com.nexohogar.domain.repository.BudgetsRepository
import java.util.Calendar

class BudgetsRepositoryImpl(
    private val budgetsApi: BudgetsApi,
    private val categoriesApi: CategoriesApi,
    private val sessionManager: SessionManager
) : BudgetsRepository {

    override suspend fun getBudgets(householdId: String): AppResult<List<Budget>> {
        return try {
            val budgetDtos = budgetsApi.getBudgets("eq.$householdId")
            val categories = try {
                val response = categoriesApi.getCategories("eq.$householdId")
                response.body() ?: emptyList<CategoryResponse>()
            } catch (_: Exception) { emptyList<CategoryResponse>() }

            val categoryMap = categories.associate { it.id to it.name }

            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1

            val spending = try {
                budgetsApi.getBudgetSpending(
                    mapOf("p_household_id" to householdId, "p_year" to year, "p_month" to month)
                )
            } catch (_: Exception) { emptyList() }

            val spendingMap = spending.associate { it.categoryId to it.spent }

            val budgets = budgetDtos.map { dto ->
                Budget(
                    id           = dto.id,
                    householdId  = dto.householdId,
                    categoryId   = dto.categoryId,
                    categoryName = categoryMap[dto.categoryId] ?: "Sin categoría",
                    amountClp    = dto.amountClp,
                    spentClp     = spendingMap[dto.categoryId] ?: 0,
                    period       = dto.period ?: "monthly",
                    isActive     = dto.isActive ?: true
                )
            }
            AppResult.Success(budgets)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al cargar presupuestos")
        }
    }

    override suspend fun createBudget(
        householdId: String,
        categoryName: String,
        amountClp: Long
    ): AppResult<Unit> {
        return try {
            val cal = Calendar.getInstance()
            val body = hashMapOf<String, Any>(
                "p_household_id"  to householdId,
                "p_category_name" to categoryName,
                "p_amount_clp"    to amountClp,
                "p_period_type"   to "monthly",
                "p_year_num"      to cal.get(Calendar.YEAR),
                "p_month_num"     to (cal.get(Calendar.MONTH) + 1)
            )
            val response = budgetApi.createBudgetRpc(body)
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error("Error al crear presupuesto: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error de red al crear presupuesto")
        }
    }

    override suspend fun deleteBudget(budgetId: String): AppResult<Unit> {
        return try {
            val response = budgetsApi.deleteBudget("eq.$budgetId")
            if (response.isSuccessful) AppResult.Success(Unit)
            else AppResult.Error("Error al eliminar presupuesto")
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al eliminar presupuesto")
        }
    }

    override suspend fun getExpensesByCategory(
        householdId: String,
        year: Int?,
        month: Int?,
        userId: String?
    ): AppResult<List<ExpenseByCategoryDto>> {
        return try {
            val body = mutableMapOf<String, Any?>(
                "p_household_id" to householdId
            )
            if (year != null) body["p_year"] = year
            if (month != null) body["p_month"] = month
            if (userId != null) body["p_user_id"] = userId

            val result = budgetsApi.getExpensesByCategory(body)
            AppResult.Success(result)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al cargar gastos por categoría")
        }
    }
}
