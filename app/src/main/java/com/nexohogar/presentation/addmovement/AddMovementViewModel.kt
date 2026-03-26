package com.nexohogar.presentation.addmovement

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.remote.dto.CreateTransactionRequest
import com.nexohogar.data.remote.dto.CreateTransferRequest
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.Category
import com.nexohogar.domain.model.RecurringBill
import com.nexohogar.domain.repository.CategoriesRepository
import com.nexohogar.domain.repository.RecurringBillsRepository
import com.nexohogar.domain.repository.TransactionsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

data class AddMovementUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val accounts: List<Account> = emptyList(),
    val selectedFromAccount: Account? = null,
    val selectedToAccount: Account? = null,
    val selectedCategory: Category? = null,
    val amount: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,

    // Estado del diálogo de nueva categoría
    val showCreateCategoryDialog: Boolean = false,
    val newCategoryName: String = "",
    val isSavingCategory: Boolean = false,

    // ── Enlace con cuenta recurrente ─────────────────────────────────────────
    // Lista de cuentas recurrentes pendientes (solo tipo EXPENSE y no pagadas aún)
    val recurringBills: List<RecurringBill> = emptyList(),
    // Cuenta recurrente que el usuario seleccionó para vincular con este gasto
    val linkedRecurringBill: RecurringBill? = null
)

class AddMovementViewModel(
    private val transactionsRepository: TransactionsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val recurringBillsRepository: RecurringBillsRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMovementUiState())
    val uiState: StateFlow<AddMovementUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())

    val filteredCategories: StateFlow<List<Category>> =
        combine(_categories, _uiState) { categories, state ->
            when (state.type.name) {
                "EXPENSE" -> categories.filter { it.type == "expense" }
                "INCOME"  -> categories.filter { it.type == "income" }
                else      -> emptyList()
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val householdId = tenantContext.getCurrentHouseholdId()
            if (householdId == null) {
                _uiState.update { it.copy(isLoading = false, error = "No hay hogar seleccionado") }
                return@launch
            }

            val accountsResult   = transactionsRepository.getAccounts(householdId)
            val categoriesResult = categoriesRepository.getCategories(householdId)
            val billsResult      = recurringBillsRepository.getRecurringBills(householdId)

            val userAccounts = if (accountsResult is AppResult.Success) {
                accountsResult.data.filter { !it.name.startsWith("__SYSTEM") }
            } else emptyList()

            val categories = if (categoriesResult is AppResult.Success) {
                categoriesResult.data
            } else emptyList()

            // Solo mostramos cuentas recurrentes activas que aún no fueron pagadas este mes
            val pendingBills = if (billsResult is AppResult.Success) {
                billsResult.data.filter { bill ->
                    bill.isActive && bill.daysUntilDue() != Int.MAX_VALUE
                }
            } else emptyList()

            if (accountsResult is AppResult.Error && categoriesResult is AppResult.Error) {
                _uiState.update { it.copy(isLoading = false, error = "Error al cargar datos iniciales") }
            } else {
                _uiState.update {
                    it.copy(
                        accounts       = userAccounts,
                        recurringBills = pendingBills,
                        isLoading      = false
                    )
                }
                _categories.value = categories
            }
        }
    }

    // ── Tipo de transacción ──────────────────────────────────────────────────
    fun onTypeChange(type: TransactionType) {
        _uiState.update {
            it.copy(
                type                = type,
                selectedCategory    = null,
                selectedToAccount   = null,
                linkedRecurringBill = null   // limpiar vínculo al cambiar de tipo
            )
        }
    }

    // ── Selección de cuentas, categoría y movimientos recurrentes ────────────
    fun onFromAccountSelected(account: Account)  { _uiState.update { it.copy(selectedFromAccount = account) } }
    fun onToAccountSelected(account: Account)    { _uiState.update { it.copy(selectedToAccount = account) } }
    fun onCategorySelected(category: Category)   { _uiState.update { it.copy(selectedCategory = category) } }
    fun onAmountChange(amount: String)           { _uiState.update { it.copy(amount = amount) } }
    fun onDescriptionChange(description: String) { _uiState.update { it.copy(description = description) } }

    /**
     * Vincula una cuenta recurrente a este gasto.
     * Si la descripción está vacía, la rellena automáticamente con el nombre de la cuenta.
     */
    fun onRecurringBillSelected(bill: RecurringBill?) {
        _uiState.update { state ->
            val newDescription = if (bill != null && state.description.isBlank()) {
                bill.name
            } else {
                state.description
            }
            state.copy(
                linkedRecurringBill = bill,
                description         = newDescription
            )
        }
    }

    // ── Diálogo de nueva categoría ───────────────────────────────────────────
    fun onShowCreateCategoryDialog()          { _uiState.update { it.copy(showCreateCategoryDialog = true, newCategoryName = "") } }
    fun onDismissCreateCategoryDialog()       { _uiState.update { it.copy(showCreateCategoryDialog = false, newCategoryName = "") } }
    fun onNewCategoryNameChange(name: String) { _uiState.update { it.copy(newCategoryName = name) } }

    fun createCategory() {
        val state       = _uiState.value
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        val name        = state.newCategoryName.trim()
        if (name.isBlank()) return

        val type = when (state.type) {
            TransactionType.INCOME -> "income"
            else                   -> "expense"
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingCategory = true) }
            when (val result = categoriesRepository.createCategory(name, type, householdId)) {
                is AppResult.Success -> {
                    _categories.update { it + result.data }
                    _uiState.update {
                        it.copy(
                            isSavingCategory       = false,
                            showCreateCategoryDialog = false,
                            newCategoryName        = "",
                            selectedCategory       = result.data
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSavingCategory = false, error = result.message) }
                }
                else -> _uiState.update { it.copy(isSavingCategory = false) }
            }
        }
    }

    // ── Guardar transacción ──────────────────────────────────────────────────
    fun saveTransaction() {
        val state       = _uiState.value
        val householdId = tenantContext.getCurrentHouseholdId() ?: return

        val amountLong = state.amount.toLongOrNull()
        if (state.selectedFromAccount == null || amountLong == null) {
            _uiState.update { it.copy(error = "Datos incompletos") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = if (state.type.name == "TRANSFER") {
                if (state.selectedToAccount == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Seleccione cuenta destino") }
                    return@launch
                }
                if (state.selectedFromAccount.id == state.selectedToAccount.id) {
                    _uiState.update { it.copy(isLoading = false, error = "Las cuentas deben ser distintas") }
                    return@launch
                }
                Log.d("TRANSFER_DEBUG", "Creating transfer: from=${state.selectedFromAccount.id}, to=${state.selectedToAccount.id}, amount=$amountLong")
                transactionsRepository.createTransfer(
                    CreateTransferRequest(
                        householdId   = householdId,
                        fromAccountId = state.selectedFromAccount.id,
                        toAccountId   = state.selectedToAccount.id,
                        amountClp     = amountLong,
                        description   = state.description.ifBlank { null },
                        transactionDate = LocalDate.now().toString()
                    )
                )
            } else {
                if (state.selectedCategory == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Seleccione categoría") }
                    return@launch
                }
                val categoryId = state.selectedCategory.id.takeIf { it.isNotBlank() }
                if (categoryId == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Categoría sin ID válido") }
                    return@launch
                }
                transactionsRepository.createTransaction(
                    CreateTransactionRequest(
                        pHouseholdId     = householdId,
                        pType            = state.type.name.lowercase(),
                        pAccountId       = state.selectedFromAccount.id,
                        pAmountClp       = amountLong,
                        pCategoryId      = categoryId,
                        pDescription     = state.description.ifBlank { null },
                        pTransactionDate = LocalDate.now().toString()
                    )
                )
            }

            when (result) {
                is AppResult.Success -> {
                    // Si hay una cuenta recurrente vinculada, marcarla como pagada
                    val linkedBill = state.linkedRecurringBill
                    if (linkedBill != null) {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        recurringBillsRepository.markAsPaid(linkedBill.id, today)
                        // Actualizamos la lista local quitando la cuenta ya pagada
                        _uiState.update { current ->
                            current.copy(
                                recurringBills      = current.recurringBills.filter { it.id != linkedBill.id },
                                linkedRecurringBill = null
                            )
                        }
                    }
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is AppResult.Error -> {
                    Log.e("TRANSFER_DEBUG", "Transfer failed: ${result.message}")
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
