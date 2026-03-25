package com.nexohogar.data.repository

import com.nexohogar.data.network.BudgetApi
import com.nexohogar.data.remote.dto.CreateBudgetRequest
import com.nexohogar.data.remote.dto.UpdateBudgetRequest
import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.Budget
import com.nexohogar.domain.model.BudgetConsumption
import com.nexohogar.domain.repository.BudgetRepository
import java.time.Instant

class BudgetRepositoryImpl(
    private val budgetApi: BudgetApi
) : BudgetRepository {

    override suspend fun getBudgets(
        householdId: String,
        yearNum: Int,
        monthNum: Int
    ): AppResult<List<Budget>> {
        return try {
            val response = budgetApi.getBudgets(
                householdId = "eq.$householdId",
                yearNum = "eq.$yearNum",
                monthNum = "eq.$monthNum"
            )
            if (response.isSuccessful) {
                val budgets = response.body()?.map { it.toDomain() } ?: emptyList()
                AppResult.Success(budgets)
            } else {
                AppResult.Error("Error al obtener presupuestos: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error("Error de conexión: ${e.localizedMessage}")
        }
    }

    override suspend fun getBudgetConsumption(
        householdId: String,
        yearNum: Int,
        monthNum: Int
    ): AppResult<List<BudgetConsumption>> {
        return try {
            val params = mapOf(
                "p_household_id" to householdId,
                "p_year" to yearNum,
                "p_month" to monthNum
            )
            val response = budgetApi.getBudgetConsumption(params)
            if (response.isSuccessful) {
                val consumption = response.body()?.map { it.toDomain() } ?: emptyList()
                AppResult.Success(consumption)
            } else {
                AppResult.Error("Error al obtener consumo: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error("Error de conexión: ${e.localizedMessage}")
        }
    }

    override suspend fun createBudget(
        householdId: String,
        categoryId: String,
        amountClp: Double,
        periodType: String,
        yearNum: Int,
        monthNum: Int?,
        weekNum: Int?,
        memberId: String?
    ): AppResult<Budget> {
        return try {
            val request = CreateBudgetRequest(
                householdId = householdId,
                categoryId = categoryId,
                amountClp = amountClp,
                periodType = periodType,
                yearNum = yearNum,
                monthNum = monthNum,
                weekNum = weekNum,
                createdBy = null, // Supabase RLS sets auth.uid()
                memberId = memberId
            )
            val response = budgetApi.createBudget(request)
            if (response.isSuccessful) {
                val created = response.body()?.firstOrNull()?.toDomain()
                if (created != null) {
                    AppResult.Success(created)
                } else {
                    AppResult.Error("Respuesta vacía al crear presupuesto")
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                if (response.code() == 409 || errorBody.contains("uq_budgets_period")) {
                    AppResult.Error("Ya existe un presupuesto para esta categoría en este período")
                } else {
                    AppResult.Error("Error al crear presupuesto: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            AppResult.Error("Error de conexión: ${e.localizedMessage}")
        }
    }

    override suspend fun updateBudget(
        budgetId: String,
        amountClp: Double
    ): AppResult<Unit> {
        return try {
            val request = UpdateBudgetRequest(
                amountClp = amountClp,
                updatedAt = Instant.now().toString()
            )
            val response = budgetApi.updateBudget(
                id = "eq.$budgetId",
                request = request
            )
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error("Error al actualizar presupuesto: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error("Error de conexión: ${e.localizedMessage}")
        }
    }

    override suspend fun deleteBudget(
        budgetId: String
    ): AppResult<Unit> {
        return try {
            val response = budgetApi.deleteBudget(id = "eq.$budgetId")
            if (response.isSuccessful) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error("Error al eliminar presupuesto: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error("Error de conexión: ${e.localizedMessage}")
        }
    }
}