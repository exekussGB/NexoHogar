package com.nexohogar.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.model.ExpenseByCategoryDto
import com.nexohogar.domain.model.HouseholdMember
import com.nexohogar.domain.repository.BudgetsRepository
import com.nexohogar.domain.repository.HouseholdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class ExpenseByCategoryUiState(
    val expenses: List<ExpenseByCategoryDto> = emptyList(),
    val members: List<HouseholdMember> = emptyList(),
    val selectedUserId: String? = null, // null = global
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val totalExpenses: Long = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ExpenseByCategoryViewModel(
    private val budgetsRepository: BudgetsRepository,
    private val householdRepository: HouseholdRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseByCategoryUiState())
    val uiState: StateFlow<ExpenseByCategoryUiState> = _uiState.asStateFlow()

    init {
        loadMembers()
        loadExpenses()
    }

    private fun loadMembers() {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        viewModelScope.launch {
            when (val result = householdRepository.getHouseholdMembers(householdId)) {
                is AppResult.Success -> _uiState.update { it.copy(members = result.data) }
                else -> { /* silently fail */ }
            }
        }
    }

    fun loadExpenses() {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = budgetsRepository.getExpensesByCategory(
                householdId = householdId,
                year        = state.selectedYear,
                month       = state.selectedMonth,
                userId      = state.selectedUserId
            )) {
                is AppResult.Success -> {
                    val total = result.data.sumOf { it.totalAmount }
                    _uiState.update { it.copy(expenses = result.data, totalExpenses = total, isLoading = false) }
                }
                is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onUserFilterChanged(userId: String?) {
        _uiState.update { it.copy(selectedUserId = userId) }
        loadExpenses()
    }

    fun onMonthChanged(month: Int, year: Int) {
        _uiState.update { it.copy(selectedMonth = month, selectedYear = year) }
        loadExpenses()
    }
}
