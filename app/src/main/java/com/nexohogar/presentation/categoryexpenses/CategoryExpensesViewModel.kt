package com.nexohogar.presentation.categoryexpenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.repository.CategoryExpensesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryExpensesViewModel(
    private val repository: CategoryExpensesRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryExpensesUiState())
    val uiState: StateFlow<CategoryExpensesUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val householdId = tenantContext.getCurrentHouseholdId()
            if (householdId == null) {
                _uiState.update { it.copy(isLoading = false, error = "No hay hogar seleccionado") }
                return@launch
            }
            when (val result = repository.getCategoryExpenses(householdId, _uiState.value.selectedMonths)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(categories = result.data, isLoading = false)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(error = result.message, isLoading = false)
                }
                else -> Unit
            }
        }
    }

    fun setMonths(months: Int) {
        _uiState.update { it.copy(selectedMonths = months) }
        load()
    }
}
