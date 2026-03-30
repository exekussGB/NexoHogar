package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.remote.dto.toDomain
import com.nexohogar.data.network.PersonalDashboardApi
import com.nexohogar.data.remote.dto.PersonalTransactionsRequest
import com.nexohogar.domain.model.MonthlyBalance
import com.nexohogar.domain.model.PersonalDashboardSummary
import com.nexohogar.domain.model.Transaction
import com.nexohogar.domain.repository.PersonalDashboardRepository

class PersonalDashboardRepositoryImpl(
    private val api: PersonalDashboardApi
) : PersonalDashboardRepository {

    override suspend fun getPersonalSummary(
        householdId: String,
        userId: String
    ): AppResult<PersonalDashboardSummary> {
        return try {
            val response = api.getPersonalSummary(
                mapOf("p_household_id" to householdId, "p_user_id" to userId)
            )
            if (response.isSuccessful) {
                val list = response.body()
                if (!list.isNullOrEmpty()) {
                    AppResult.Success(list.first().toDomain())
                } else {
                    // Sin cuentas personales → resumen vacío
                    AppResult.Success(
                        PersonalDashboardSummary(
                            totalBalance      = 0.0,
                            totalIncome       = 0.0,
                            totalExpense      = 0.0,
                            accountsCount     = 0,
                            transactionsCount = 0
                        )
                    )
                }
            } else {
                AppResult.Error("Error al cargar resumen personal: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}")
        }
    }

    override suspend fun getPersonalMonthlyBalance(
        householdId: String,
        userId: String
    ): AppResult<List<MonthlyBalance>> {
        return try {
            val response = api.getPersonalMonthlyBalance(
                mapOf("p_household_id" to householdId, "p_user_id" to userId)
            )
            if (response.isSuccessful) {
                val domain = response.body()?.map {
                    MonthlyBalance(
                        yearNum  = it.yearNum,
                        monthNum = it.monthNum,
                        income   = it.income,
                        expense  = it.expense,
                        net      = it.net
                    )
                } ?: emptyList()
                AppResult.Success(domain)
            } else {
                AppResult.Error("Error tendencia mensual personal: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al cargar tendencia mensual personal")
        }
    }

    override suspend fun getPersonalRecentTransactions(
        householdId: String,
        userId: String,
        limit: Int
    ): AppResult<List<Transaction>> {
        return try {
            val response = api.getPersonalRecentTransactions(
                PersonalTransactionsRequest(
                    p_household_id = householdId,
                    p_user_id      = userId,
                    p_limit        = limit
                )
            )
            if (response.isSuccessful) {
                val txns = response.body()?.map { dto ->
                    Transaction(
                        id          = dto.id,
                        accountId   = dto.accountId,
                        amount      = dto.amountClp,
                        description = dto.description,
                        createdAt   = dto.createdAt,
                        type        = dto.type
                    )
                } ?: emptyList()
                AppResult.Success(txns)
            } else {
                AppResult.Error("Error al cargar movimientos personales: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al cargar movimientos personales")
        }
    }
}
