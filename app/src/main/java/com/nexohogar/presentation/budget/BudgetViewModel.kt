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
import java.util.Calendar

class BudgetViewModel(
    private val repository   : BudgetRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init { load() }

    fun load(year: Int = currentYear(), month: Int = currentMonth()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, year = year, month = month) }
            val householdId = tenantContext.getCurrentHouseholdId()
            if (householdId == null) {
                _uiState.update { it.copy(isLoading = false, error = "No hay hogar seleccionado") }
                return@launch
            }
            when (val result = repository.getBudgetConsumption(householdId, year, month)) {
                is AppResult.Success -> _uiState.update { it.copy(items = result.data, isLoading = false) }
                is AppResult.Error   -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                else                 -> Unit
            }
        }
    }

    fun previousMonth() {
        val s = _uiState.value
        val (y, m) = if (s.month == 1) s.year - 1 to 12 else s.year to s.month - 1
        load(y, m)
    }

    fun nextMonth() {
        val s = _uiState.value
        val (y, m) = if (s.month == 12) s.year + 1 to 1 else s.year to s.month + 1
        load(y, m)
    }

    private fun currentYear()  = Calendar.getInstance().get(Calendar.YEAR)
    private fun currentMonth() = Calendar.getInstance().get(Calendar.MONTH) + 1
}
