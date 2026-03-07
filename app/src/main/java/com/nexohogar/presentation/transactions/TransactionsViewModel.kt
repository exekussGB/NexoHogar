package com.nexohogar.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.model.CreateTransactionRequest
import com.nexohogar.domain.model.Transaction
import com.nexohogar.domain.repository.TransactionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for managing the transactions screen state.
 */
class TransactionsViewModel(
    private val repository: TransactionsRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransactionsUiState>(TransactionsUiState.Loading)
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
    }

    /**
     * Fetches transactions for the current household.
     */
    fun loadTransactions() {
        viewModelScope.launch {
            _uiState.value = TransactionsUiState.Loading
            
            val householdId = tenantContext.getCurrentHouseholdId()
            if (householdId == null) {
                _uiState.value = TransactionsUiState.Error("No se ha seleccionado un hogar.")
                return@launch
            }

            when (val result = repository.getTransactions(householdId)) {
                is AppResult.Success -> {
                    _uiState.value = TransactionsUiState.Success(result.data)
                }
                is AppResult.Error -> {
                    _uiState.value = TransactionsUiState.Error(result.message)
                }
                is AppResult.Loading -> { }
            }
        }
    }

    /**
     * Creates a new transaction via Supabase RPC.
     */
    fun createTransaction(
        accountId: String,
        amount: Double,
        description: String?
    ) {
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

                when (repository.createTransaction(request)) {
                    is AppResult.Success -> {
                        loadTransactions()
                    }
                    is AppResult.Error -> {
                        _uiState.value = TransactionsUiState.Error("No se pudo crear el movimiento")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.value = TransactionsUiState.Error(e.message ?: "Error inesperado")
            }
        }
    }
}

sealed interface TransactionsUiState {
    object Loading : TransactionsUiState
    data class Success(val transactions: List<Transaction>) : TransactionsUiState
    data class Error(val message: String) : TransactionsUiState
}
