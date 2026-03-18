package com.nexohogar.presentation.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.remote.dto.CreateTransactionRequest
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.Category
import com.nexohogar.domain.repository.CategoriesRepository
import com.nexohogar.domain.repository.TransactionsRepository
import com.nexohogar.presentation.addmovement.TransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@Deprecated("Use AddMovementViewModel instead")
data class AddTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val paymentAccounts: List<Account> = emptyList(),
    val selectedPaymentAccount: Account? = null,
    val selectedToAccount: Account? = null,
    val selectedCategory: Category? = null,
    val amount: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@Deprecated("Use AddMovementViewModel instead")
class AddTransactionViewModel(
    private val repository: TransactionsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

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
        loadCategories()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val householdId = tenantContext.getCurrentHouseholdId() ?: run {
                _uiState.update { it.copy(error = "No se ha seleccionado un hogar.", isLoading = false) }
                return@launch
            }
            
            when (val result = repository.getAccounts(householdId)) {
                is AppResult.Success -> {
                    val accounts = result.data
                    val paymentAccounts = accounts.filter {
                        it.type == "ASSET" || it.type == "LIABILITY"
                    }
                    _uiState.update {
                        it.copy(
                            paymentAccounts = paymentAccounts,
                            isLoading = false
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
                else -> {}
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val householdId = tenantContext.getCurrentHouseholdId() ?: return@launch
            when (val result = categoriesRepository.getCategories(householdId)) {
                is AppResult.Success -> {
                    _categories.value = result.data
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun onTypeChange(type: TransactionType) {
        // Al cambiar el tipo, reseteamos la categoría seleccionada para evitar inconsistencias
        _uiState.update { it.copy(type = type, selectedCategory = null, selectedToAccount = null) }
    }

    fun onPaymentAccountSelected(account: Account) {
        _uiState.update { it.copy(selectedPaymentAccount = account) }
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
        val currentState = _uiState.value
        
        // Validaciones comunes
        val paymentAccount = currentState.selectedPaymentAccount ?: run {
            _uiState.update { it.copy(error = "Selecciona una cuenta") }
            return
        }
        val amountValue = currentState.amount.toLongOrNull() ?: run {
            _uiState.update { it.copy(error = "Monto inválido") }
            return
        }

        // Validaciones específicas
        if (currentState.type == TransactionType.TRANSFER) {
            if (currentState.selectedToAccount == null) {
                _uiState.update { it.copy(error = "Selecciona cuenta destino") }
                return
            }
            if (currentState.selectedPaymentAccount.id == currentState.selectedToAccount.id) {
                _uiState.update { it.copy(error = "Las cuentas deben ser diferentes") }
                return
            }
        } else {
            if (currentState.selectedCategory == null) {
                _uiState.update { it.copy(error = "Selecciona una categoría") }
                return
            }
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                val householdId = tenantContext.requireHouseholdId()
                val request = CreateTransactionRequest(
                    pHouseholdId = householdId,
                    pType = currentState.type.name.lowercase(),
                    pAccountId = paymentAccount.id,
                    pAmountClp = amountValue,
                    pCategoryId = currentState.selectedCategory?.id,
                    pDescription = currentState.description,
                    pTransactionDate = LocalDate.now().toString()
                )

                when (val result = repository.createTransaction(request)) {
                    is AppResult.Success -> {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    }
                    is AppResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error inesperado") }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
