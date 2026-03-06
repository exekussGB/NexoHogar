package com.nexohogar.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.model.Transaction
import com.nexohogar.domain.repository.TransactionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
}

sealed interface TransactionsUiState {
    object Loading : TransactionsUiState
    data class Success(val transactions: List<Transaction>) : TransactionsUiState
    data class Error(val message: String) : TransactionsUiState
}
