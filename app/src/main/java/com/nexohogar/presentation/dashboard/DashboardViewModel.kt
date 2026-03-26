package com.nexohogar.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
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
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { refreshDashboard() }

    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val householdId = tenantContext.getCurrentHouseholdId()
            val userId = tenantContext.getCurrentUserId()
            if (householdId == null || userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "No hay hogar o usuario seleccionado") }
                return@launch
            }

            val summaryDeferred      = async { dashboardRepository.getDashboardSummary(householdId) }
            val transactionsDeferred = async { transactionsRepository.getTransactions(householdId) }
            val monthlyDeferred      = async { dashboardRepository.getMonthlyBalance(householdId) }
            val accountsDeferred     = async { dashboardRepository.getAccountBalances(householdId, userId) }
            val dualDeferred         = async { dashboardRepository.getDualDashboard(householdId, userId) }

            val summaryResult      = summaryDeferred.await()
            val transactionsResult = transactionsDeferred.await()
            val monthlyResult      = monthlyDeferred.await()
            val accountsResult     = accountsDeferred.await()
            val dualResult         = dualDeferred.await()

            val allAccounts = if (accountsResult is AppResult.Success) accountsResult.data else emptyList()

            _uiState.update {
                it.copy(
                    summary            = if (summaryResult is AppResult.Success) summaryResult.data else null,
                    recentTransactions = if (transactionsResult is AppResult.Success) transactionsResult.data.take(5) else emptyList(),
                    monthlyBalance     = if (monthlyResult is AppResult.Success) monthlyResult.data else emptyList(),
                    accountBalances    = allAccounts,
                    sharedSection      = if (dualResult is AppResult.Success) dualResult.data.shared else null,
                    personalSection    = if (dualResult is AppResult.Success) dualResult.data.personal else null,
                    sharedAccounts     = allAccounts.filter { ab -> ab.isShared },
                    personalAccounts   = allAccounts.filter { ab -> !ab.isShared },
                    isLoading          = false,
                    error              = if (summaryResult is AppResult.Error) summaryResult.message else null
                )
            }
        }
    }
}
