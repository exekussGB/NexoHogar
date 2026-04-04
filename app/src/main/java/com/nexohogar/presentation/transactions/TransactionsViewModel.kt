package com.nexohogar.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.remote.dto.CreateTransactionRequest
import com.nexohogar.domain.model.Transaction
import com.nexohogar.domain.repository.TransactionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for managing the transactions screen state with pagination support.
 */
class TransactionsViewModel(
    private val repository: TransactionsRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val PAGE_SIZE = 30

    private val _uiState = MutableStateFlow<TransactionsUiState>(TransactionsUiState.Loading)
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    // Lista acumulada de transacciones (para paginación)
    private val _allTransactions = mutableListOf<Transaction>()
    private var currentOffset = 0
    private var isLoadingMoreFlag = false
    private var hasMoreData = true

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    init {
        loadTransactions()
    }

    /**
     * Fetches transactions from the beginning (refresh).
     */
    fun loadTransactions() {
        viewModelScope.launch {
            _uiState.value = TransactionsUiState.Loading
            _allTransactions.clear()
            currentOffset = 0
            hasMoreData = true

            val householdId = tenantContext.getCurrentHouseholdId()
            if (householdId == null) {
                _uiState.value = TransactionsUiState.Error("No se ha seleccionado un hogar.")
                return@launch
            }

            when (val result = repository.getTransactions(householdId, limit = PAGE_SIZE, offset = 0)) {
                is AppResult.Success -> {
                    _allTransactions.addAll(result.data)
                    hasMoreData = result.data.size >= PAGE_SIZE
                    currentOffset = result.data.size
                    _uiState.value = TransactionsUiState.Success(
                        transactions = _allTransactions.toList(),
                        hasMoreData = hasMoreData
                    )
                }
                is AppResult.Error -> {
                    _uiState.value = TransactionsUiState.Error(result.message)
                }
                is AppResult.Loading -> { }
            }
        }
    }

    /**
     * Loads the next page of transactions (infinite scroll).
     */
    fun loadMoreTransactions() {
        if (isLoadingMoreFlag || !hasMoreData) return
        val state = _uiState.value
        if (state !is TransactionsUiState.Success) return

        viewModelScope.launch {
            isLoadingMoreFlag = true
            _isLoadingMore.value = true

            val householdId = tenantContext.getCurrentHouseholdId() ?: run {
                isLoadingMoreFlag = false
                _isLoadingMore.value = false
                return@launch
            }

            when (val result = repository.getTransactions(householdId, limit = PAGE_SIZE, offset = currentOffset)) {
                is AppResult.Success -> {
                    _allTransactions.addAll(result.data)
                    hasMoreData = result.data.size >= PAGE_SIZE
                    currentOffset += result.data.size
                    _uiState.value = TransactionsUiState.Success(
                        transactions = _allTransactions.toList(),
                        hasMoreData = hasMoreData
                    )
                }
                is AppResult.Error -> { /* silently ignore, keep current state */ }
                else -> {}
            }
            isLoadingMoreFlag = false
            _isLoadingMore.value = false
        }
    }

    /**
     * Creates a new transaction via Supabase RPC.
     */
    fun createTransaction(
        accountId: String?,
        amount: Long,
        description: String?
    ) {
        if (accountId.isNullOrBlank()) {
            _uiState.value = TransactionsUiState.Error("Validation error: accountId cannot be null")
            return
        }

        viewModelScope.launch {
            try {
                val householdId = tenantContext.requireHouseholdId()
                val request = CreateTransactionRequest(
                    pHouseholdId = householdId,
                    pType = "expense",
                    pAccountId = accountId,
                    pAmountClp = amount,
                    pCategoryId = null,
                    pDescription = description ?: "",
                    pTransactionDate = LocalDate.now().toString()
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
    data class Success(
        val transactions: List<Transaction>,
        val hasMoreData: Boolean = true
    ) : TransactionsUiState
    data class Error(val message: String) : TransactionsUiState
}
