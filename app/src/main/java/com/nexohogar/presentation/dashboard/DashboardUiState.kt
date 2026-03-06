package com.nexohogar.presentation.dashboard

import com.nexohogar.domain.model.DashboardSummary

/**
 * UI State for the Dashboard screen.
 */
sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(val summary: DashboardSummary) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}
