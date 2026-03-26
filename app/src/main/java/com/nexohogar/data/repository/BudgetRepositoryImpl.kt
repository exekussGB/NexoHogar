package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.network.BudgetApi
import com.nexohogar.data.remote.dto.toDomain
import com.nexohogar.domain.model.BudgetItem
import com.nexohogar.domain.repository.BudgetRepository

class BudgetRepositoryImpl(
    private val budgetApi: BudgetApi
) : BudgetRepository {

    override suspend fun getBudgetConsumption(
        householdId : String,
        year        : Int,
        month       : Int
    ): AppResult<List<BudgetItem>> {
        return try {
            val body = mapOf<String, Any>(
                "p_household_id" to householdId,
                "p_year"         to year,
                "p_month"        to month
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
}
