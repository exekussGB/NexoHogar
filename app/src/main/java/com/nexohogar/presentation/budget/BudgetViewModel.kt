package com.nexohogar.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val householdId = tenantContext.getCurrentHouseholdId()
            if (householdId == null) {
                _uiState.update { it.copy(isLoading = false, error = "No hay hogar seleccionado") }
                return@launch
            }
            when (val result = budgetRepository.getBudgetConsumption(householdId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(budgets = result.data, isLoading = false)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(error = result.message, isLoading = false)
                }
                else -> Unit
            }
        }
    }

    fun createBudget(categoryName: String, amount: Long) {
        viewModelScope.launch {
            val householdId = tenantContext.getCurrentHouseholdId() ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            when (budgetRepository.createBudget(householdId, categoryName, amount)) {
                is AppResult.Success -> load()
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = "Error al crear presupuesto") }
                }
                else -> Unit
            }
        }
    }

    fun updateBudget(budgetId: String, newAmount: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (budgetRepository.updateBudget(budgetId, newAmount)) {
                is AppResult.Success -> load()
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = "Error al actualizar") }
                }
                else -> Unit
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (budgetRepository.deleteBudget(budgetId)) {
                is AppResult.Success -> load()
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = "Error al eliminar") }
                }
                else -> Unit
            }
        }
    }
}
