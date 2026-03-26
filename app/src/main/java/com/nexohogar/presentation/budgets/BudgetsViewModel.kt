package com.nexohogar.presentation.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.model.Budget
import com.nexohogar.domain.model.Category
import com.nexohogar.domain.repository.BudgetsRepository
import com.nexohogar.domain.repository.CategoriesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetsUiState(
    val budgets: List<Budget> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val selectedCategoryId: String? = null,
    val newAmount: String = "",
    val isCreating: Boolean = false,
    val showDeleteConfirm: String? = null
)

class BudgetsViewModel(
    private val budgetsRepository: BudgetsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    init {
        loadBudgets()
        loadCategories()
    }

    fun loadBudgets() {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = budgetsRepository.getBudgets(householdId)) {
                is AppResult.Success -> _uiState.update { it.copy(budgets = result.data, isLoading = false) }
                is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadCategories() {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        viewModelScope.launch {
            when (val result = categoriesRepository.getCategories(householdId)) {
                is AppResult.Success -> {
                    val expenseCategories = result.data.filter { it.type == "expense" }
                    _uiState.update { it.copy(categories = expenseCategories) }
                }
                else -> {}
            }
        }
    }

    fun showCreateDialog() { _uiState.update { it.copy(showCreateDialog = true, selectedCategoryId = null, newAmount = "") } }
    fun dismissCreateDialog() { _uiState.update { it.copy(showCreateDialog = false) } }
    fun onCategorySelected(id: String) { _uiState.update { it.copy(selectedCategoryId = id) } }
    fun onAmountChange(amount: String) {
        // Only allow digits
        if (amount.all { it.isDigit() } || amount.isEmpty()) {
            _uiState.update { it.copy(newAmount = amount) }
        }
    }

    fun createBudget() {
        val state = _uiState.value
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        val categoryId = state.selectedCategoryId ?: return
        val amount = state.newAmount.toLongOrNull() ?: return

        if (amount <= 0) {
            _uiState.update { it.copy(error = "El monto debe ser mayor a 0") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            when (val result = budgetsRepository.createBudget(householdId, categoryId, amount)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isCreating = false, showCreateDialog = false) }
                    loadBudgets()
                }
                is AppResult.Error -> _uiState.update { it.copy(isCreating = false, error = result.message) }
                else -> _uiState.update { it.copy(isCreating = false) }
            }
        }
    }

    fun showDeleteConfirm(id: String) { _uiState.update { it.copy(showDeleteConfirm = id) } }
    fun dismissDeleteConfirm() { _uiState.update { it.copy(showDeleteConfirm = null) } }

    fun deleteBudget() {
        val budgetId = _uiState.value.showDeleteConfirm ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteConfirm = null, isLoading = true) }
            when (budgetsRepository.deleteBudget(budgetId)) {
                is AppResult.Success -> loadBudgets()
                is AppResult.Error -> _uiState.update { it.copy(isLoading = false) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}
