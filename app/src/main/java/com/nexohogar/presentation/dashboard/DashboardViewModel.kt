package com.nexohogar.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Dashboard feature.
 * Manages fetching data and exposing UI state.
 */
class DashboardViewModel(
    private val repository: DashboardRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    /**
     * Fetches dashboard summary data using the current household context.
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            
            try {
                // Get current householdId from TenantContext
                val householdId = tenantContext.getCurrentHouseholdId()
                
                if (householdId == null) {
                    _uiState.value = DashboardUiState.Error("No household selected")
                    return@launch
                }

                // Fetch data from repository
                when (val result = repository.getDashboardSummary(householdId)) {
                    is AppResult.Success -> {
                        _uiState.value = DashboardUiState.Success(result.data)
                    }
                    is AppResult.Error -> {
                        _uiState.value = DashboardUiState.Error(result.message)
                    }
                    is AppResult.Loading -> {
                        // Handled by manual state setting above
                    }
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error("Unexpected error: ${e.message}")
            }
        }
    }
}
