package com.nexohogar.presentation.transactiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.TransactionEntry
import com.nexohogar.domain.repository.TransactionDetailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the transaction detail screen state.
 */
class TransactionDetailViewModel(
    private val repository: TransactionDetailRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransactionDetailUiState>(TransactionDetailUiState.Loading)
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    /**
     * Fetches details for a specific transaction.
     */
    fun loadTransactionDetails(transactionId: String) {
        viewModelScope.launch {
            _uiState.value = TransactionDetailUiState.Loading
            
            when (val result = repository.getTransactionEntries(transactionId)) {
                is AppResult.Success -> {
                    _uiState.value = TransactionDetailUiState.Success(result.data)
                }
                is AppResult.Error -> {
                    _uiState.value = TransactionDetailUiState.Error(result.message)
                }
                is AppResult.Loading -> { }
            }
        }
    }
}

sealed interface TransactionDetailUiState {
    object Loading : TransactionDetailUiState
    data class Success(val entries: List<TransactionEntry>) : TransactionDetailUiState
    data class Error(val message: String) : TransactionDetailUiState
}
