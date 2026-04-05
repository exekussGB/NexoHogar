package com.nexohogar.presentation.transactiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.core.util.InputSanitizer
import com.nexohogar.domain.model.TransactionDetail
import com.nexohogar.domain.repository.TransactionDetailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TransactionDetailUiState {
    object Loading : TransactionDetailUiState
    data class Success(
        val detail: TransactionDetail,
        val isSuperUser: Boolean = false,
        val isEditing: Boolean = false,
        val editAmount: String = "",
        val editDescription: String = "",
        val editDate: String = "",
        val isSaving: Boolean = false,
        val editError: String? = null,
        val editSuccess: Boolean = false
    ) : TransactionDetailUiState
    data class Error(val message: String) : TransactionDetailUiState
}

class TransactionDetailViewModel(
    private val repository: TransactionDetailRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransactionDetailUiState>(TransactionDetailUiState.Loading)
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    fun loadTransactionDetail(transactionId: String) {
        viewModelScope.launch {
            _uiState.value = TransactionDetailUiState.Loading
            when (val result = repository.getTransactionDetail(transactionId)) {
                is AppResult.Success -> _uiState.value = TransactionDetailUiState.Success(
                    detail = result.data,
                    isSuperUser = tenantContext.isSuperUser()
                )
                is AppResult.Error   -> _uiState.value = TransactionDetailUiState.Error(result.message)
                is AppResult.Loading -> { /* no-op */ }
            }
        }
    }

    fun startEditing() {
        val current = _uiState.value
        if (current is TransactionDetailUiState.Success) {
            _uiState.value = current.copy(
                isEditing = true,
                editAmount = current.detail.amountClp.toString(),
                editDescription = current.detail.description ?: "",
                editDate = current.detail.transactionDate ?: "",
                editError = null,
                editSuccess = false
            )
        }
    }

    fun cancelEditing() {
        val current = _uiState.value
        if (current is TransactionDetailUiState.Success) {
            _uiState.value = current.copy(isEditing = false, editError = null)
        }
    }

    fun onEditAmountChange(amount: String) {
        val current = _uiState.value
        if (current is TransactionDetailUiState.Success) {
            if (amount.isEmpty() || amount.all { it.isDigit() }) {
                _uiState.value = current.copy(editAmount = amount)
            }
        }
    }

    fun onEditDescriptionChange(description: String) {
        val current = _uiState.value
        if (current is TransactionDetailUiState.Success) {
            _uiState.value = current.copy(editDescription = description)
        }
    }

    fun onEditDateChange(date: String) {
        val current = _uiState.value
        if (current is TransactionDetailUiState.Success) {
            _uiState.value = current.copy(editDate = date)
        }
    }

    fun saveEdit() {
        val current = _uiState.value
        if (current !is TransactionDetailUiState.Success) return

        val detail = current.detail

        val newAmount = current.editAmount.toLongOrNull()
        if (newAmount == null || newAmount <= 0) {
            _uiState.value = current.copy(editError = "Ingresa un monto valido mayor a 0")
            return
        }

        val newDescription = current.editDescription.trim()
        if (newDescription.isBlank()) {
            _uiState.value = current.copy(editError = "La descripcion es obligatoria")
            return
        }

        val sanitizedDesc = InputSanitizer.sanitizeText(newDescription, 200)

        val amountChanged = newAmount != detail.amountClp
        val descChanged = sanitizedDesc != (detail.description ?: "")
        val dateChanged = current.editDate != (detail.transactionDate ?: "")

        if (!amountChanged && !descChanged && !dateChanged) {
            _uiState.value = current.copy(isEditing = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true, editError = null)

            when (val result = repository.updateTransaction(
                transactionId   = detail.id,
                amountClp       = if (amountChanged) newAmount else null,
                description     = if (descChanged) sanitizedDesc else null,
                transactionDate = if (dateChanged) current.editDate else null
            )) {
                is AppResult.Success -> {
                    loadTransactionDetail(detail.id)
                }
                is AppResult.Error -> {
                    _uiState.value = current.copy(
                        isSaving = false,
                        editError = result.message
                    )
                }
                else -> {
                    _uiState.value = current.copy(isSaving = false)
                }
            }
        }
    }

    fun clearEditError() {
        val current = _uiState.value
        if (current is TransactionDetailUiState.Success) {
            _uiState.value = current.copy(editError = null)
        }
    }
}
