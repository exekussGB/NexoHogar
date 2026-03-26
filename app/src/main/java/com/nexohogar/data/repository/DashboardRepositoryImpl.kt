package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.model.toSection
import com.nexohogar.data.model.toDomain
import com.nexohogar.data.network.DashboardApi
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.model.DashboardSection
import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.model.DualDashboard
import com.nexohogar.domain.model.MonthlyBalance
import com.nexohogar.domain.repository.DashboardRepository

class DashboardRepositoryImpl(
    private val dashboardApi: DashboardApi
) : DashboardRepository {

    override suspend fun getDashboardSummary(householdId: String): AppResult<DashboardSummary> {
        return try {
            val response = dashboardApi.getDashboardSummary("eq.$householdId")
            if (response.isSuccessful) {
                val list = response.body()
                if (!list.isNullOrEmpty()) {
                    AppResult.Success(list.first().toDomain())
                } else {
                    AppResult.Error("No hay datos para este hogar")
                }
            } else {
                AppResult.Error("Error al cargar resumen: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error("Fallo de red: ${e.message}")
        }
    }

    override suspend fun getMonthlyBalance(householdId: String): AppResult<List<MonthlyBalance>> {
        return try {
            val response = dashboardApi.getMonthlyBalance(
                mapOf("p_household_id" to householdId)
            )
            if (response.isSuccessful) {
                val domain = response.body()?.map { dto ->
                    MonthlyBalance(
                        yearNum  = dto.yearNum,
                        monthNum = dto.monthNum,
                        income   = dto.income,
                        expense  = dto.expense,
                        net      = dto.net
                    )
                } ?: emptyList()
                AppResult.Success(domain)
            } else {
                AppResult.Error("Error tendencia mensual: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al cargar tendencia mensual")
        }
    }

    override suspend fun getAccountBalances(householdId: String, userId: String): AppResult<List<AccountBalance>> {
        return try {
            val response = dashboardApi.getAccountBalancesV2(
                mapOf("p_household_id" to householdId, "p_user_id" to userId)
            )
            if (response.isSuccessful) {
                val balances = response.body()?.map { dto ->
                    AccountBalance(
                        accountId = dto.accountId,
                        accountName = dto.accountName,
                        accountType = dto.accountType,
                        movementBalance = dto.movementBalance.toLong(),
                        isShared = dto.isShared ?: true
                    )
                } ?: emptyList()
                AppResult.Success(balances)
            } else {
                AppResult.Error("Error saldos de cuentas: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al cargar saldos")
        }
    }

    override suspend fun getDualDashboard(householdId: String, userId: String): AppResult<DualDashboard> {
        return try {
            val response = dashboardApi.getDualDashboard(
                mapOf("p_household_id" to householdId, "p_user_id" to userId)
            )
            if (response.isSuccessful) {
                val rows = response.body() ?: emptyList()
                val sharedRow = rows.find { it.section == "shared" }
                val personalRow = rows.find { it.section == "personal" }

                val shared = sharedRow?.toSection() ?: DashboardSection(0.0, 0.0, 0.0, 0)
                val personal = personalRow?.toSection()?.takeIf { it.accountsCount > 0 }

                AppResult.Success(DualDashboard(shared = shared, personal = personal))
            } else {
                AppResult.Error("Error dashboard dual: ${response.code()}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Error al cargar dashboard dual")
        }
    }
}
