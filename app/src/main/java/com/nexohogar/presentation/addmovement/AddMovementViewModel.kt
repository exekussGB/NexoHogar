package com.nexohogar.presentation.addmovement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.remote.dto.CreateTransactionRequest
import com.nexohogar.data.remote.dto.CreateTransferRequest
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.Category
import com.nexohogar.domain.repository.CategoriesRepository
import com.nexohogar.domain.repository.TransactionsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    val isSavingCategory: Boolean = false
)

class AddMovementViewModel(
    private val transactionsRepository: TransactionsRepository,
    private val categoriesRepository: CategoriesRepository,
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

            if (accountsResult is AppResult.Success && categoriesResult is AppResult.Success) {
                val userAccounts = accountsResult.data.filter { account ->
                    !account.name.startsWith("__SYSTEM")
                }
                _uiState.update {
                    it.copy(accounts = userAccounts, isLoading = false)
                }
                _categories.value = categoriesResult.data
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Error al cargar datos iniciales") }
            }
        }
    }

    // ── Tipo de transacción ──────────────────────────────────────────────────
    fun onTypeChange(type: TransactionType) {
        _uiState.update {
            it.copy(type = type, selectedCategory = null, selectedToAccount = null)
        }
    }

    // ── Selección de cuentas y categoría ────────────────────────────────────
    fun onFromAccountSelected(account: Account)   { _uiState.update { it.copy(selectedFromAccount = account) } }
    fun onToAccountSelected(account: Account)     { _uiState.update { it.copy(selectedToAccount = account) } }
    fun onCategorySelected(category: Category)    { _uiState.update { it.copy(selectedCategory = category) } }
    fun onAmountChange(amount: String)            { _uiState.update { it.copy(amount = amount) } }
    fun onDescriptionChange(description: String)  { _uiState.update { it.copy(description = description) } }

    // ── Diálogo de nueva categoría ───────────────────────────────────────────
    fun onShowCreateCategoryDialog()              { _uiState.update { it.copy(showCreateCategoryDialog = true, newCategoryName = "") } }
    fun onDismissCreateCategoryDialog()           { _uiState.update { it.copy(showCreateCategoryDialog = false, newCategoryName = "") } }
    fun onNewCategoryNameChange(name: String)     { _uiState.update { it.copy(newCategoryName = name) } }

    fun createCategory() {
        val state = _uiState.value
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        val name = state.newCategoryName.trim()
        if (name.isBlank()) return

        val type = when (state.type) {
            TransactionType.INCOME  -> "income"
            else                    -> "expense"
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingCategory = true) }
            when (val result = categoriesRepository.createCategory(name, type, householdId)) {
                is AppResult.Success -> {
                    _categories.update { it + result.data }
                    _uiState.update {
                        it.copy(
                            isSavingCategory = false,
                            showCreateCategoryDialog = false,
                            newCategoryName = "",
                            selectedCategory = result.data
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
        val state = _uiState.value
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
                transactionsRepository.createTransfer(
                    CreateTransferRequest(
                        householdId     = householdId,
                        fromAccountId   = state.selectedFromAccount.id,
                        toAccountId     = state.selectedToAccount.id,
                        amountClp       = amountLong,
                        description     = state.description.ifBlank { null },
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
                        pHouseholdId    = householdId,
                        pType           = state.type.name.lowercase(),
                        pAccountId      = state.selectedFromAccount.id,
                        pAmountClp      = amountLong,
                        pCategoryId     = categoryId,
                        pDescription    = state.description.ifBlank { null },
                        pTransactionDate = LocalDate.now().toString()
                    )
                )
            }

            when (result) {
                is AppResult.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is AppResult.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}