package com.nexohogar.presentation.transactiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.TransactionDetail
import com.nexohogar.domain.repository.TransactionDetailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TransactionDetailUiState {
    object Loading : TransactionDetailUiState
    data class Success(val detail: TransactionDetail) : TransactionDetailUiState
    data class Error(val message: String) : TransactionDetailUiState
}

class TransactionDetailViewModel(
    private val repository: TransactionDetailRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransactionDetailUiState>(TransactionDetailUiState.Loading)
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    fun loadTransactionDetail(transactionId: String) {
        viewModelScope.launch {
            _uiState.value = TransactionDetailUiState.Loading
            when (val result = repository.getTransactionDetail(transactionId)) {
                is AppResult.Success -> _uiState.value = TransactionDetailUiState.Success(result.data)
                is AppResult.Error -> _uiState.value = TransactionDetailUiState.Error(result.message)
                is AppResult.Loading -> { }
            }
        }
    }
}