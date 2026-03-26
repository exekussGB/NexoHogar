package com.nexohogar.presentation.personaldashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.local.SessionManager
import com.nexohogar.domain.repository.PersonalDashboardRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonalDashboardViewModel(
    private val personalDashboardRepository: PersonalDashboardRepository,
    private val tenantContext: TenantContext,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalDashboardUiState())
    val uiState: StateFlow<PersonalDashboardUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val householdId = tenantContext.getCurrentHouseholdId()
            val userId      = sessionManager.fetchSession()?.userId

            if (householdId == null || userId == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "No hay hogar o sesión activa")
                }
                return@launch
            }

            // Cargar en paralelo
            val summaryDeferred      = async { personalDashboardRepository.getPersonalSummary(householdId, userId) }
            val monthlyDeferred      = async { personalDashboardRepository.getPersonalMonthlyBalance(householdId, userId) }
            val transactionsDeferred = async { personalDashboardRepository.getPersonalRecentTransactions(householdId, userId) }

            val summaryResult      = summaryDeferred.await()
            val monthlyResult      = monthlyDeferred.await()
            val transactionsResult = transactionsDeferred.await()

            val summary = if (summaryResult is AppResult.Success) summaryResult.data else null

            _uiState.update {
                it.copy(
                    summary             = summary,
                    hasPersonalAccounts = (summary?.accountsCount ?: 0) > 0,
                    monthlyBalance      = if (monthlyResult      is AppResult.Success) monthlyResult.data      else emptyList(),
                    recentTransactions  = if (transactionsResult is AppResult.Success) transactionsResult.data else emptyList(),
                    isLoading           = false,
                    error               = if (summaryResult is AppResult.Error) summaryResult.message else null
                )
            }
        }
    }
}
