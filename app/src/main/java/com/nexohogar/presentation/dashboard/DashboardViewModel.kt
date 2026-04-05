package com.nexohogar.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.repository.AccountsRepository
import com.nexohogar.domain.repository.DashboardRepository
import com.nexohogar.domain.repository.TransactionsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val transactionsRepository: TransactionsRepository,
    private val accountsRepository: AccountsRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { refreshDashboard() }

    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val householdId = tenantContext.getCurrentHouseholdId()
            if (householdId == null) {
                _uiState.update { it.copy(isLoading = false, error = "No hay hogar seleccionado") }
                return@launch
            }

            val userId = tenantContext.getCurrentUserId()

            val summaryDeferred      = async { dashboardRepository.getDashboardSummary(householdId) }
            val transactionsDeferred = async { transactionsRepository.getTransactions(householdId) }
            val monthlyDeferred      = async { dashboardRepository.getMonthlyBalance(householdId) }
            val personalDeferred     = async {
                if (userId != null) accountsRepository.hasPersonalAccounts(householdId, userId)
                else AppResult.Success(false)
            }
            // 🆕 Feature 2: Calcular total de ahorro desde los saldos de cuentas
            val balancesDeferred     = async { accountsRepository.getAccountBalances(householdId) }

            val summaryResult      = summaryDeferred.await()
            val transactionsResult = transactionsDeferred.await()
            val monthlyResult      = monthlyDeferred.await()
            val personalResult     = personalDeferred.await()
            val balancesResult     = balancesDeferred.await()

            // 🆕 Feature 2: Sumar saldos de cuentas marcadas como ahorro
            val savingsTotal = if (balancesResult is AppResult.Success) {
                balancesResult.data
                    .filter { it.isSavings }
                    .sumOf { it.movementBalance }
            } else 0L

            // 🆕 Fix #3: Compute total balance client-side from account balances
            val computedTotalBalance = if (balancesResult is AppResult.Success) {
                balancesResult.data.sumOf { it.movementBalance }.toDouble()
            } else null

            _uiState.update {
                it.copy(
                    summary             = if (summaryResult is AppResult.Success) summaryResult.data else null,
                    recentTransactions  = if (transactionsResult is AppResult.Success) transactionsResult.data.take(5) else emptyList(),
                    monthlyBalance      = if (monthlyResult is AppResult.Success) monthlyResult.data else emptyList(),
                    hasPersonalAccounts = if (personalResult is AppResult.Success) personalResult.data else false,
                    totalSavings        = savingsTotal,    // 🆕 Feature 2
                    computedTotalBalance = computedTotalBalance,
                    isLoading           = false,
                    error               = if (summaryResult is AppResult.Error) summaryResult.message else null
                )
            }
        }
    }
}
