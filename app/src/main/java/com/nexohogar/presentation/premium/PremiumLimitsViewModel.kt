package com.nexohogar.presentation.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.LimitType
import com.nexohogar.domain.model.Plan
import com.nexohogar.domain.repository.SubscriptionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PremiumLimitsUiState(
    val isLoading: Boolean = true,
    val plan: Plan? = null,
    val errorMessage: String? = null
)

class PremiumLimitsViewModel(
    private val subscriptionsRepository: SubscriptionsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PremiumLimitsUiState>(PremiumLimitsUiState())
    val uiState: StateFlow<PremiumLimitsUiState> = _uiState.asStateFlow()

    init {
        loadUserPlan()
    }

    private fun loadUserPlan() {
        viewModelScope.launch {
            when (val result = subscriptionsRepository.getCurrentUserPlan()) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        plan = result.data,
                        errorMessage = null
                    )
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        plan = null,
                        errorMessage = result.message
                    )
                }
                is AppResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun retry() {
        loadUserPlan()
    }

    // Helper functions para la UI
    fun getLimitDisplay(limitType: LimitType): String {
        val plan = _uiState.value.plan ?: return "Cargando..."
        val limit = plan.getLimit(limitType) ?: return "Ilimitado"
        return limit.toString()
    }

    fun hasLimit(limitType: LimitType): Boolean {
        return _uiState.value.plan?.hasLimit(limitType) ?: false
    }

    fun isPremium(): Boolean {
        return _uiState.value.plan?.isPremium() ?: false
    }
}
