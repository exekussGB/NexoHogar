package com.nexohogar.presentation.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.model.CreateTransactionRequest
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.repository.TransactionsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val paymentAccounts: List<Account> = emptyList(),
    val categories: List<Account> = emptyList(),
    val selectedPaymentAccount: Account? = null,
    val selectedCategory: Account? = null,
    val amount: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class AddTransactionViewModel(
    private val repository: TransactionsRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
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
                    val categories = accounts.filter {
                        it.type == "EXPENSE" || it.type == "INCOME"
                    }
                    _uiState.update {
                        it.copy(
                            paymentAccounts = paymentAccounts,
                            categories = categories,
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

    fun onTypeChange(type: TransactionType) {
        _uiState.update { it.copy(type = type) }
    }

    fun onPaymentAccountSelected(account: Account) {
        _uiState.update { it.copy(selectedPaymentAccount = account) }
    }

    fun onCategorySelected(category: Account) {
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
