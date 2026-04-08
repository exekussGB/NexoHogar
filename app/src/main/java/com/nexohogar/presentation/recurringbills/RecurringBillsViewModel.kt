package com.nexohogar.presentation.recurringbills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.core.util.InputSanitizer
import com.nexohogar.data.remote.dto.RecurringBillPaymentDto
import com.nexohogar.data.remote.dto.RecurringBillWithStatusDto
import com.nexohogar.data.remote.dto.RecurringSummaryDto
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.Category
import com.nexohogar.domain.model.RecurringBill
import com.nexohogar.domain.repository.AccountsRepository
import com.nexohogar.domain.repository.CategoriesRepository
import com.nexohogar.domain.repository.RecurringBillsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class RecurringBillsUiState(
    val bills: List<RecurringBill>                   = emptyList(),
    val billsWithStatus: List<RecurringBillWithStatusDto> = emptyList(),
    val summary: RecurringSummaryDto?                 = null,
    val accounts: List<Account>                      = emptyList(),
    val categories: List<Category>                   = emptyList(),
    val isLoading: Boolean                           = false,
    val error: String?                               = null,

    // Diálogo de creación
    val showCreateDialog: Boolean                    = false,
    val isCreating: Boolean                          = false,
    val createError: String?                         = null,

    // Diálogo de edición
    val billToEdit: RecurringBill?                   = null,
    val isUpdating: Boolean                          = false,
    val updateError: String?                         = null,

    // Diálogo de pago mejorado
    val billToPay: RecurringBillWithStatusDto?       = null,
    val isPayingBill: Boolean                        = false,

    // Historial de pagos
    val showHistoryFor: RecurringBillWithStatusDto?   = null,
    val paymentHistory: List<RecurringBillPaymentDto> = emptyList(),
    val isLoadingHistory: Boolean                     = false
)

class RecurringBillsViewModel(
    private val repository: RecurringBillsRepository,
    private val accountsRepository: AccountsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringBillsUiState())
    val uiState: StateFlow<RecurringBillsUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    // ── Carga completa ──────────────────────────────────────────────────────

    fun loadAll() {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val billsJob = launch {
                when (val result = repository.getBillsWithStatus(householdId)) {
                    is AppResult.Success -> _uiState.update { it.copy(billsWithStatus = result.data) }
                    is AppResult.Error   -> _uiState.update { it.copy(error = result.message) }
                    is AppResult.Loading -> Unit
                }
            }
            val summaryJob = launch {
                when (val result = repository.getRecurringSummary(householdId)) {
                    is AppResult.Success -> _uiState.update { it.copy(summary = result.data) }
                    is AppResult.Error   -> {}
                    is AppResult.Loading -> Unit
                }
            }
            val accountsJob = launch {
                when (val result = accountsRepository.getAccounts(householdId)) {
                    is AppResult.Success -> _uiState.update { it.copy(accounts = result.data) }
                    is AppResult.Error   -> {}
                    is AppResult.Loading -> Unit
                }
            }
            val categoriesJob = launch {
                when (val result = categoriesRepository.getCategories(householdId)) {
                    is AppResult.Success -> _uiState.update { it.copy(categories = result.data) }
                    is AppResult.Error   -> {}
                    is AppResult.Loading -> Unit
                }
            }
            val legacyJob = launch {
                when (val result = repository.getRecurringBills(householdId)) {
                    is AppResult.Success -> _uiState.update { it.copy(bills = result.data) }
                    is AppResult.Error   -> {}
                    is AppResult.Loading -> Unit
                }
            }

            billsJob.join()
            summaryJob.join()
            accountsJob.join()
            categoriesJob.join()
            legacyJob.join()

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun loadBills() = loadAll()

    // ── Crear ────────────────────────────────────────────────────────────────

    fun onShowCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, createError = null) }
    }

    fun onDismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, createError = null) }
    }

    fun createBill(
        name: String,
        amountClp: Long,
        dueDayOfMonth: Int,
        notes: String?,
        totalInstallments: Int? = null,
        paidInstallments: Int? = null,
        categoryId: String? = null
    ) {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return

        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(createError = "El nombre es obligatorio") }
            return
        }
        if (trimmedName.length > 100) {
            _uiState.update { it.copy(createError = "El nombre es demasiado largo (máx. 100 caracteres)") }
            return
        }
        val sanitizedName = InputSanitizer.sanitizeText(trimmedName, 100)

        if (amountClp <= 0 || amountClp > 999_999_999) {
            _uiState.update { it.copy(createError = "Ingresa un monto válido mayor a 0") }
            return
        }

        if (dueDayOfMonth < 1 || dueDayOfMonth > 31) {
            _uiState.update { it.copy(createError = "El día de vencimiento debe estar entre 1 y 31") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, createError = null) }
            when (val result = repository.createRecurringBill(
                householdId, sanitizedName, amountClp, dueDayOfMonth, notes,
                totalInstallments, paidInstallments, categoryId
            )) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(
                        isCreating       = false,
                        showCreateDialog = false
                    )}
                    loadAll()
                }
                is AppResult.Error -> _uiState.update { it.copy(
                    createError = result.message,
                    isCreating  = false
                )}
                is AppResult.Loading -> Unit
            }
        }
    }

    // ── Editar ───────────────────────────────────────────────────────────────

    fun onShowEditDialog(bill: RecurringBill) {
        _uiState.update { it.copy(billToEdit = bill, updateError = null) }
    }

    fun onDismissEditDialog() {
        _uiState.update { it.copy(billToEdit = null, updateError = null) }
    }

    fun updateBill(
        name: String?,
        amountClp: Long?,
        dueDayOfMonth: Int?,
        notes: String?,
        totalInstallments: Int?,
        paidInstallments: Int? = null,
        categoryId: String? = null
    ) {
        val bill = _uiState.value.billToEdit ?: return

        val sanitizedName = if (name != null) {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                _uiState.update { it.copy(updateError = "El nombre es obligatorio") }
                return
            }
            if (trimmedName.length > 100) {
                _uiState.update { it.copy(updateError = "El nombre es demasiado largo (máx. 100 caracteres)") }
                return
            }
            InputSanitizer.sanitizeText(trimmedName, 100)
        } else null

        if (amountClp != null && (amountClp <= 0 || amountClp > 999_999_999)) {
            _uiState.update { it.copy(updateError = "Ingresa un monto válido mayor a 0") }
            return
        }

        if (dueDayOfMonth != null && (dueDayOfMonth < 1 || dueDayOfMonth > 31)) {
            _uiState.update { it.copy(updateError = "El día de vencimiento debe estar entre 1 y 31") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, updateError = null) }
            when (val result = repository.updateRecurringBill(
                billId            = bill.id,
                name              = sanitizedName,
                amountClp         = amountClp,
                dueDayOfMonth     = dueDayOfMonth,
                notes             = notes,
                totalInstallments = totalInstallments,
                paidInstallments  = paidInstallments,
                categoryId        = categoryId
            )) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(billToEdit = null, isUpdating = false) }
                    loadAll()
                }
                is AppResult.Error -> _uiState.update { it.copy(
                    updateError = result.message,
                    isUpdating  = false
                )}
                is AppResult.Loading -> Unit
            }
        }
    }

    // ── Pagar con popup (monto editable) ────────────────────────────────────

    fun showPayDialog(bill: RecurringBillWithStatusDto) {
        _uiState.update { it.copy(billToPay = bill) }
    }

    fun dismissPayDialog() {
        _uiState.update { it.copy(billToPay = null) }
    }

    fun payBill(amountClp: Long, accountId: String?, notes: String?) {
        val bill = _uiState.value.billToPay ?: return
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPayingBill = true) }
            when (val result = repository.payBill(
                billId      = bill.id,
                householdId = householdId,
                amountClp   = amountClp,
                accountId   = accountId,
                notes       = notes
            )) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(
                        billToPay    = null,
                        isPayingBill = false
                    )}
                    loadAll()
                }
                is AppResult.Error -> _uiState.update { it.copy(
                    error        = result.message,
                    billToPay    = null,
                    isPayingBill = false
                )}
                is AppResult.Loading -> Unit
            }
        }
    }

    // ── Marcar como pagado (legacy, simple) ─────────────────────────────────

    fun confirmMarkAsPaid(bill: RecurringBill) {
        val withStatus = _uiState.value.billsWithStatus.find { it.id == bill.id }
        if (withStatus != null) {
            showPayDialog(withStatus)
        } else {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            viewModelScope.launch {
                when (val result = repository.markAsPaid(bill.id, today)) {
                    is AppResult.Success -> loadAll()
                    is AppResult.Error   -> _uiState.update { it.copy(error = result.message) }
                    is AppResult.Loading -> Unit
                }
            }
        }
    }

    fun dismissPayDialogLegacy() {
        _uiState.update { it.copy(billToPay = null) }
    }

    // ── Historial de pagos ──────────────────────────────────────────────────

    fun showHistory(bill: RecurringBillWithStatusDto) {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        _uiState.update { it.copy(showHistoryFor = bill, isLoadingHistory = true) }
        viewModelScope.launch {
            when (val result = repository.getBillHistory(bill.id, householdId)) {
                is AppResult.Success -> _uiState.update { it.copy(
                    paymentHistory   = result.data,
                    isLoadingHistory = false
                )}
                is AppResult.Error -> _uiState.update { it.copy(
                    isLoadingHistory = false,
                    error = result.message
                )}
                is AppResult.Loading -> Unit
            }
        }
    }

    fun dismissHistory() {
        _uiState.update { it.copy(showHistoryFor = null, paymentHistory = emptyList()) }
    }

    // ── Activar / desactivar ─────────────────────────────────────────────────

    fun toggleActive(bill: RecurringBill) {
        viewModelScope.launch {
            when (val result = repository.toggleActive(bill.id, !bill.isActive)) {
                is AppResult.Success -> loadAll()
                is AppResult.Error -> _uiState.update { it.copy(error = result.message) }
                is AppResult.Loading -> Unit
            }
        }
    }

    // ── Eliminar ─────────────────────────────────────────────────────────────

    fun deleteBill(bill: RecurringBill) {
        viewModelScope.launch {
            when (repository.deleteRecurringBill(bill.id)) {
                is AppResult.Success -> loadAll()
                is AppResult.Error -> { /* silencioso */ }
                is AppResult.Loading -> Unit
            }
        }
    }
}
