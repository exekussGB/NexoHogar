package com.nexohogar.presentation.recurringbills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.model.RecurringBill
import com.nexohogar.domain.repository.RecurringBillsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class RecurringBillsUiState(
    val bills: List<RecurringBill>  = emptyList(),
    val isLoading: Boolean          = false,
    val error: String?              = null,

    // Diálogo de creación
    val showCreateDialog: Boolean   = false,
    val isCreating: Boolean         = false,
    val createError: String?        = null,

    // Diálogo de confirmación de pago
    val billToPay: RecurringBill?   = null,
    val isMarkingPaid: Boolean      = false
)

class RecurringBillsViewModel(
    private val repository: RecurringBillsRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringBillsUiState())
    val uiState: StateFlow<RecurringBillsUiState> = _uiState.asStateFlow()

    init { loadBills() }

    // ── Carga ────────────────────────────────────────────────────────────────

    fun loadBills() {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getRecurringBills(householdId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    bills = result.data, isLoading = false
                )
                is AppResult.Error -> _uiState.value = _uiState.value.copy(
                    error = result.message, isLoading = false
                )
            }
        }
    }

    // ── Crear ────────────────────────────────────────────────────────────────

    fun onShowCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true, createError = null)
    }

    fun onDismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false, createError = null)
    }

    fun createBill(name: String, amountClp: Long, dueDayOfMonth: Int, notes: String?) {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, createError = null)
            when (val result = repository.createRecurringBill(householdId, name, amountClp, dueDayOfMonth, notes)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    bills            = _uiState.value.bills + result.data,
                    isCreating       = false,
                    showCreateDialog = false
                )
                is AppResult.Error -> _uiState.value = _uiState.value.copy(
                    createError = result.message,
                    isCreating  = false
                )
            }
        }
    }

    // ── Marcar como pagado ───────────────────────────────────────────────────

    fun confirmMarkAsPaid(bill: RecurringBill) {
        _uiState.value = _uiState.value.copy(billToPay = bill)
    }

    fun dismissPayDialog() {
        _uiState.value = _uiState.value.copy(billToPay = null)
    }

    fun markAsPaid() {
        val bill = _uiState.value.billToPay ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMarkingPaid = true)
            when (val result = repository.markAsPaid(bill.id, today)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    bills = _uiState.value.bills.map { if (it.id == bill.id) result.data else it },
                    billToPay     = null,
                    isMarkingPaid = false
                )
                is AppResult.Error -> _uiState.value = _uiState.value.copy(
                    error         = result.message,
                    billToPay     = null,
                    isMarkingPaid = false
                )
            }
        }
    }

    // ── Activar / desactivar ─────────────────────────────────────────────────

    fun toggleActive(bill: RecurringBill) {
        viewModelScope.launch {
            when (val result = repository.toggleActive(bill.id, !bill.isActive)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    bills = _uiState.value.bills.map { if (it.id == bill.id) result.data else it }
                )
                is AppResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    // ── Eliminar ─────────────────────────────────────────────────────────────

    fun deleteBill(bill: RecurringBill) {
        viewModelScope.launch {
            when (repository.deleteRecurringBill(bill.id)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    bills = _uiState.value.bills.filter { it.id != bill.id }
                )
                is AppResult.Error -> { /* silencioso — el item sigue visible */ }
            }
        }
    }
}
