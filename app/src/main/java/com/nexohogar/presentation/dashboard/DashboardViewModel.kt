package com.nexohogar.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.repository.DashboardRepository
import com.nexohogar.domain.repository.TransactionsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val transactionsRepository: TransactionsRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true, error = null) }

            val householdId = tenantContext.getCurrentHouseholdId()

            if (householdId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No hay hogar seleccionado"
                    )
                }
                return@launch
            }

            when (val summaryResult = dashboardRepository.getDashboardSummary(householdId)) {

                is AppResult.Success -> {

                    val transactionsResult = transactionsRepository.getTransactions(householdId)

                    val recent = if (transactionsResult is AppResult.Success) {
                        transactionsResult.data.take(5)
                    } else {
                        emptyList()
                    }

                    _uiState.update {
                        it.copy(
                            summary = summaryResult.data,
                            recentTransactions = recent,
                            isLoading = false
                        )
                    }
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = summaryResult.message
                        )
                    }
                }
                else -> {}
            }
        }
    }
}
