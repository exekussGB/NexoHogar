package com.nexohogar.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.repository.BudgetRepository
import com.nexohogar.domain.repository.CategoriesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class BudgetViewModel(
    private val repository          : BudgetRepository,
    private val categoriesRepository: CategoriesRepository,
    private val tenantContext        : TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init { load() }

    // ── Load ────────────────────────────────────────────────────────────────

    fun load(year: Int = currentYear(), month: Int = currentMonth()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, year = year, month = month) }
            val householdId = tenantContext.getCurrentHouseholdId()
            if (householdId == null) {
                _uiState.update { it.copy(isLoading = false, error = "No hay hogar seleccionado") }
                return@launch
            }
            // Load budgets + categories in parallel
            val budgetResult = repository.getBudgetConsumption(householdId, year, month)
            val catResult    = categoriesRepository.getCategories(householdId)

            when (budgetResult) {
                is AppResult.Success -> {
                    val cats = if (catResult is AppResult.Success) catResult.data else emptyList()
                    _uiState.update {
                        it.copy(items = budgetResult.data, categories = cats, isLoading = false)
                    }
                }
                is AppResult.Error -> _uiState.update { it.copy(error = budgetResult.message, isLoading = false) }
                else -> Unit
            }
        }
    }

    // ── Month navigation ────────────────────────────────────────────────────

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

    // ── Create ──────────────────────────────────────────────────────────────

    fun showCreateDialog()  { _uiState.update { it.copy(showCreateDialog = true) } }
    fun hideCreateDialog()  { _uiState.update { it.copy(showCreateDialog = false) } }

    fun createBudget(categoryId: String, amount: Long, memberId: String?) {
        val s = _uiState.value
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            when (val result = repository.createBudget(householdId, categoryId, amount, s.year, s.month, memberId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isCreating = false, showCreateDialog = false, snackMessage = "Presupuesto creado") }
                    load(s.year, s.month)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isCreating = false, snackMessage = result.message) }
                }
                else -> Unit
            }
        }
    }

    // ── Edit ────────────────────────────────────────────────────────────────

    fun showEditDialog(item: com.nexohogar.domain.model.BudgetItem) {
        _uiState.update { it.copy(showEditDialog = item) }
    }
    fun hideEditDialog() { _uiState.update { it.copy(showEditDialog = null) } }

    fun updateBudget(budgetId: String, newAmount: Long) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            when (val result = repository.updateBudget(budgetId, newAmount)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isUpdating = false, showEditDialog = null, snackMessage = "Presupuesto actualizado") }
                    load(s.year, s.month)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isUpdating = false, snackMessage = result.message) }
                }
                else -> Unit
            }
        }
    }

    // ── Delete ──────────────────────────────────────────────────────────────

    fun showDeleteConfirm(item: com.nexohogar.domain.model.BudgetItem) {
        _uiState.update { it.copy(showDeleteConfirm = item) }
    }
    fun hideDeleteConfirm() { _uiState.update { it.copy(showDeleteConfirm = null) } }

    fun deleteBudget(budgetId: String) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            when (val result = repository.deleteBudget(budgetId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isDeleting = false, showDeleteConfirm = null, snackMessage = "Presupuesto eliminado") }
                    load(s.year, s.month)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isDeleting = false, snackMessage = result.message) }
                }
                else -> Unit
            }
        }
    }

    // ── Snackbar ─────────────────────────────────────────────────────────────

    fun clearSnack() { _uiState.update { it.copy(snackMessage = null) } }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun currentYear()  = Calendar.getInstance().get(Calendar.YEAR)
    private fun currentMonth() = Calendar.getInstance().get(Calendar.MONTH) + 1
}
