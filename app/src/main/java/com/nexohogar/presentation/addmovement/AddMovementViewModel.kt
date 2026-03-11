package com.nexohogar.presentation.addmovement

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

data class AddMovementUiState(
    val type: MovementType = MovementType.EXPENSE,
    val paymentAccounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccount: Account? = null,
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

    init {
        loadInitialData()
    }

    fun setMovementType(type: MovementType) {
        _uiState.update { it.copy(type = type, selectedCategory = null, selectedToAccount = null) }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val householdId = tenantContext.getCurrentHouseholdId() ?: return@launch
            
            // Cargar cuentas
            val accountsResult = transactionsRepository.getAccounts(householdId)
            // Cargar categorías
            val categoriesResult = categoriesRepository.getCategories(householdId)

            if (accountsResult is AppResult.Success && categoriesResult is AppResult.Success) {
                _uiState.update { 
                    it.copy(
                        paymentAccounts = accountsResult.data.filter { a -> a.type == "ASSET" || a.type == "LIABILITY" },
                        categories = categoriesResult.data,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Error al cargar datos") }
            }
        }
    }

    fun onAccountSelected(account: Account) { _uiState.update { it.copy(selectedAccount = account) } }
    fun onToAccountSelected(account: Account) { _uiState.update { it.copy(selectedToAccount = account) } }
    fun onCategorySelected(category: Category) { _uiState.update { it.copy(selectedCategory = category) } }
    fun onAmountChange(amount: String) { _uiState.update { it.copy(amount = amount) } }
    fun onDescriptionChange(desc: String) { _uiState.update { it.copy(description = desc) } }

    fun saveMovement() {
        val state = _uiState.value
        val amountValue = state.amount.toDoubleOrNull() ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val request = CreateTransactionRequest(
                householdId = tenantContext.requireHouseholdId(),
                type = state.type.name.lowercase(),
                accountId = state.selectedAccount?.id ?: "",
                toAccountId = state.selectedToAccount?.id,
                amountClp = amountValue,
                categoryId = state.selectedCategory?.id,
                description = state.description,
                transactionDate = LocalDate.now().toString()
            )

            when (transactionsRepository.createTransaction(request)) {
                is AppResult.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = "Error al guardar") }
                else -> {}
            }
        }
    }
}
