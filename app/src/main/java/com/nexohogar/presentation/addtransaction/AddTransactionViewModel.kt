package com.nexohogar.presentation.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.model.CreateTransactionRequest
import com.nexohogar.domain.repository.TransactionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for the Add Transaction screen.
 */
class AddTransactionViewModel(
    private val repository: TransactionsRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddTransactionUiState>(AddTransactionUiState.Idle)
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    fun createTransaction(
        accountId: String,
        amount: Double,
        description: String?
    ) {
        if (accountId.isBlank() || amount <= 0) {
            _uiState.value = AddTransactionUiState.Error("Datos inválidos")
            return
        }

        _uiState.value = AddTransactionUiState.Loading
        viewModelScope.launch {
            try {
                val householdId = tenantContext.requireHouseholdId()
                val request = CreateTransactionRequest(
                    p_household_id = householdId,
                    p_account_id = accountId,
                    p_amount = amount,
                    p_type = "expense",
                    p_description = description,
                    p_transaction_date = LocalDate.now().toString()
                )

                when (val result = repository.createTransaction(request)) {
                    is AppResult.Success -> {
                        _uiState.value = AddTransactionUiState.Success
                    }
                    is AppResult.Error -> {
                        _uiState.value = AddTransactionUiState.Error(result.message)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.value = AddTransactionUiState.Error(e.message ?: "Error inesperado")
            }
        }
    }
}

sealed interface AddTransactionUiState {
    object Idle : AddTransactionUiState
    object Loading : AddTransactionUiState
    object Success : AddTransactionUiState
    data class Error(val message: String) : AddTransactionUiState
}
