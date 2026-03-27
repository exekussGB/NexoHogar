package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.BudgetApi
import com.nexohogar.data.remote.dto.toDomain
import com.nexohogar.domain.model.BudgetItem
import com.nexohogar.domain.repository.BudgetRepository
import java.util.Calendar

class BudgetRepositoryImpl(
    private val budgetApi: BudgetApi
) : BudgetRepository {

    override suspend fun getBudgetConsumption(
        householdId: String
    ): AppResult<List<BudgetItem>> {
        return try {
            val cal = Calendar.getInstance()
            val body = hashMapOf<String, Any>(
                "p_household_id" to householdId,
                "p_year"         to cal.get(Calendar.YEAR),
                "p_month"        to (cal.get(Calendar.MONTH) + 1)
            )
            val response = budgetApi.getBudgetConsumption(body)
            if (response.isSuccessful) {
                AppResult.Success(response.body()?.toDomain() ?: emptyList())
            } else {
                AppResult.Error("Error al cargar presupuestos: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error de red al cargar presupuestos")
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

    override suspend fun updateBudget(
        budgetId: String,
        amountClp: Long
    ): AppResult<Unit> {
        return try {
            val body = hashMapOf<String, Any>(
                "amount_clp" to amountClp
            )
            val response = budgetApi.updateBudget("eq.$budgetId", body)
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error("Error al actualizar presupuesto: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error de red al actualizar presupuesto")
        }
    }

    override suspend fun deleteBudget(budgetId: String): AppResult<Unit> {
        return try {
            val response = budgetApi.deleteBudget("eq.$budgetId")
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error("Error al eliminar presupuesto: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error de red al eliminar presupuesto")
        }
    }
}
