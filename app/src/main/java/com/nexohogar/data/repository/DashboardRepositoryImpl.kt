package com.nexohogar.data.repository

import com.nexohogar.core.result.AppResult
import com.nexohogar.data.remote.dto.toDomain
import com.nexohogar.data.network.DashboardApi
import com.nexohogar.data.remote.dto.toDomain
import com.nexohogar.domain.model.AccountBalance
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

    /**
     * Saldos desde la VISTA account_balances.
     * userId se recibe por firma de interfaz pero el filtro de cuentas
     * compartidas/personales se hace en AccountsRepositoryImpl.
     */
    override suspend fun getAccountBalances(
        householdId: String,
        userId: String
    ): AppResult<List<AccountBalance>> {
        return try {
            val response = dashboardApi.getAccountBalances("eq.$householdId")
            if (response.isSuccessful) {
                val balances = response.body()
                    ?.filter { !it.accountName.lowercase().contains("system") }
                    ?.map { dto ->
                        AccountBalance(
                            accountId       = dto.accountId,
                            accountName     = dto.accountName,
                            accountType     = dto.accountType,
                            movementBalance = dto.balanceClp.toLong()
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

    /**
     * No usado por ningún ViewModel actualmente — stub vacío para cumplir interfaz.
     */
    override suspend fun getDualDashboard(
        householdId: String,
        userId: String
    ): AppResult<DualDashboard> {
        return AppResult.Error("getDualDashboard no implementado")
    }
}
