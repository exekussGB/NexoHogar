package com.nexohogar.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.model.BudgetConsumption
import com.nexohogar.domain.repository.BudgetRepository
import com.nexohogar.domain.repository.CategoriesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val categoriesRepository: CategoriesRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val householdId = tenantContext.requireHouseholdId()

            // Load categories (expense only for budget creation)
            when (val catResult = categoriesRepository.getCategories(householdId)) {
                is AppResult.Success -> {
                    val expenseCategories = catResult.data.filter { it.type == "expense" }
                    _uiState.update { it.copy(categories = expenseCategories) }
                }
                is AppResult.Error -> { /* Non-blocking */ }
                is AppResult.Loading -> Unit
            }

            // Load budget consumption
            loadBudgets()
        }
    }

    fun loadBudgets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val householdId = tenantContext.requireHouseholdId()
            val year = _uiState.value.selectedYear
            val month = _uiState.value.selectedMonth

            when (val result = budgetRepository.getBudgetConsumption(householdId, year, month)) {
                is AppResult.Success -> {
                    val filtered = filterBySelectedMember(result.data)
                    val totalBudgeted = filtered.sumOf { it.budgetedAmount }
                    val totalConsumed = filtered.sumOf { it.consumedAmount }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            budgets = filtered,
                            totalBudgeted = totalBudgeted,
                            totalConsumed = totalConsumed,
                            errorMessage = null
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun createBudget(categoryId: String, amount: Double, memberId: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }

            val householdId = tenantContext.requireHouseholdId()
            val year = _uiState.value.selectedYear
            val month = _uiState.value.selectedMonth

            when (val result = budgetRepository.createBudget(
                householdId = householdId,
                categoryId = categoryId,
                amountClp = amount,
                periodType = "monthly",
                yearNum = year,
                monthNum = month,
                weekNum = null,
                memberId = memberId
            )) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(isCreating = false, showCreateDialog = false)
                    }
                    loadBudgets()
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(isCreating = false, errorMessage = result.message)
                    }
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun updateBudget(budgetId: String, newAmount: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }

            when (val result = budgetRepository.updateBudget(budgetId, newAmount)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            showEditDialog = false,
                            editingBudget = null
                        )
                    }
                    loadBudgets()
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(isCreating = false, errorMessage = result.message)
                    }
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            when (val result = budgetRepository.deleteBudget(budgetId)) {
                is AppResult.Success -> loadBudgets()
                is AppResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    fun changeMonth(year: Int, month: Int) {
        _uiState.update { it.copy(selectedYear = year, selectedMonth = month) }
        loadBudgets()
    }

    fun goToPreviousMonth() {
        val current = YearMonth.of(_uiState.value.selectedYear, _uiState.value.selectedMonth)
        val prev = current.minusMonths(1)
        changeMonth(prev.year, prev.monthValue)
    }

    fun goToNextMonth() {
        val current = YearMonth.of(_uiState.value.selectedYear, _uiState.value.selectedMonth)
        val next = current.plusMonths(1)
        changeMonth(next.year, next.monthValue)
    }

    fun filterByMember(memberId: String?) {
        _uiState.update { it.copy(selectedMemberId = memberId) }
        loadBudgets()
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun showEditDialog(budget: BudgetConsumption) {
        _uiState.update {
            it.copy(showEditDialog = true, editingBudget = budget)
        }
    }

    fun hideEditDialog() {
        _uiState.update {
            it.copy(showEditDialog = false, editingBudget = null)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun filterBySelectedMember(budgets: List<BudgetConsumption>): List<BudgetConsumption> {
        val memberId = _uiState.value.selectedMemberId
        return if (memberId == null) {
            budgets
        } else {
            budgets.filter { it.memberId == memberId || it.memberId == null }
        }
    }
}