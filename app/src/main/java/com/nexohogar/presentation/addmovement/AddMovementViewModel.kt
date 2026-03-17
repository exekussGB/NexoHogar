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
import com.nexohogar.presentation.addtransaction.TransactionType
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
    val isSuccess: Boolean = false
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
            when (state.type) {
                TransactionType.EXPENSE -> categories.filter { it.type == "expense" }
                TransactionType.INCOME -> categories.filter { it.type == "income" }
                TransactionType.TRANSFER -> emptyList()
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
            val householdId = tenantContext.getCurrentHouseholdId() ?: return@launch
            
            val accountsResult = transactionsRepository.getAccounts(householdId)
            val categoriesResult = categoriesRepository.getCategories(householdId)

            if (accountsResult is AppResult.Success && categoriesResult is AppResult.Success) {
                _uiState.update { 
                    it.copy(
                        accounts = accountsResult.data,
                        isLoading = false
                    )
                }
                _categories.value = categoriesResult.data
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Error al cargar datos iniciales") }
            }
        }
    }

    fun onTypeChange(type: TransactionType) {
        _uiState.update { 
            it.copy(
                type = type, 
                selectedCategory = null, 
                selectedToAccount = null 
            ) 
        }
    }

    fun onFromAccountSelected(account: Account) {
        _uiState.update { it.copy(selectedFromAccount = account) }
    }

    fun onToAccountSelected(account: Account) {
        _uiState.update { it.copy(selectedToAccount = account) }
    }

    fun onCategorySelected(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onAmountChange(amount: String) {
        _uiState.update { it.copy(amount = amount) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

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
            
            val result = if (state.type == TransactionType.TRANSFER) {
                if (state.selectedToAccount == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Seleccione cuenta destino") }
                    return@launch
                }
                if (state.selectedFromAccount.id == state.selectedToAccount.id) {
                    _uiState.update { it.copy(isLoading = false, error = "Cuentas deben ser distintas") }
                    return@launch
                }
                
                val request = CreateTransferRequest(
                    householdId = householdId,
                    fromAccountId = state.selectedFromAccount.id,
                    toAccountId = state.selectedToAccount.id,
                    amountClp = amountLong
                )
                transactionsRepository.createTransfer(request)
            } else {
                if (state.selectedCategory == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Seleccione categoría") }
                    return@launch
                }
                
                val request = CreateTransactionRequest(
                    pHouseholdId = householdId,
                    pType = state.type.name.lowercase(),
                    pAccountId = state.selectedFromAccount.id,
                    pAmountClp = amountLong,
                    pCategoryId = state.selectedCategory.id,
                    pDescription = state.description.ifBlank { null },
                    pTransactionDate = LocalDate.now().toString()
                )
                transactionsRepository.createTransaction(request)
            }

            when (result) {
                is AppResult.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
