package com.nexohogar.presentation.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.model.CreateTransactionRequest
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.Category
import com.nexohogar.domain.repository.CategoriesRepository
import com.nexohogar.domain.repository.TransactionsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val paymentAccounts: List<Account> = emptyList(),
    val selectedPaymentAccount: Account? = null,
    val selectedCategory: Category? = null,
    val amount: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

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
            categories.filter { category ->
                when (state.type) {
                    TransactionType.EXPENSE -> category.type == "expense"
                    TransactionType.INCOME -> category.type == "income"
                }
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
        _uiState.update { it.copy(type = type, selectedCategory = null) }
    }

    fun onPaymentAccountSelected(account: Account) {
        _uiState.update { it.copy(selectedPaymentAccount = account) }
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
        val paymentAccount = currentState.selectedPaymentAccount ?: run {
            _uiState.update { it.copy(error = "Selecciona una cuenta de pago") }
            return
        }
        val category = currentState.selectedCategory ?: run {
            _uiState.update { it.copy(error = "Selecciona una categoría") }
            return
        }
        val amountValue = currentState.amount.toDoubleOrNull() ?: run {
            _uiState.update { it.copy(error = "Monto inválido") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                val householdId = tenantContext.requireHouseholdId()
                val request = CreateTransactionRequest(
                    householdId = householdId,
                    type = currentState.type.name.lowercase(),
                    accountId = paymentAccount.id,
                    amountClp = amountValue,
                    categoryId = category.id,
                    description = currentState.description,
                    transactionDate = LocalDate.now().toString()
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
