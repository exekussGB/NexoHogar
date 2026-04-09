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
 * Filter types for the transactions list.
 */
enum class TransactionFilter {
    ALL, EXPENSE, INCOME, TRANSFER
}

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

    // ── Filter state ─────────────────────────────────────────────────────────
    private val _selectedFilter = MutableStateFlow(TransactionFilter.ALL)
    val selectedFilter: StateFlow<TransactionFilter> = _selectedFilter.asStateFlow()

    // ── Date range filter state ──────────────────────────────────────────────
    private val _dateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val dateRange: StateFlow<Pair<Long, Long>?> = _dateRange.asStateFlow()

    // BUG FIX 1: Almacenar parámetros de fecha para persistencia
    private var storedStartMillis: Long? = null
    private var storedEndMillis: Long? = null

    init {
        loadTransactions()
    }

    /**
     * Sets the active transaction filter and updates the displayed list.
     */
    fun setFilter(filter: TransactionFilter) {
        _selectedFilter.value = filter
        applyFilter()
    }

    /**
     * Sets a date range filter and reloads the list.
     * BUG FIX 1: Persiste los parámetros de fecha en el ViewModel
     */
    fun setDateFilter(startMillis: Long, endMillis: Long) {
        _dateRange.value = Pair(startMillis, endMillis)
        storedStartMillis = startMillis
        storedEndMillis = endMillis
        applyFilter()
    }

    /**
     * Clears the date range filter and reloads the list.
     */
    fun clearDateFilter() {
        _dateRange.value = null
        storedStartMillis = null
        storedEndMillis = null
        applyFilter()
    }

    /**
     * Applies the current type filter and date range filter to the accumulated transactions list.
     */
    private fun applyFilter() {
        val range = _dateRange.value

        // First apply type filter
        val typeFiltered = when (_selectedFilter.value) {
            TransactionFilter.ALL -> _allTransactions.toList()
            TransactionFilter.EXPENSE -> _allTransactions.filter { it.type.lowercase() == "expense" }
            TransactionFilter.INCOME -> _allTransactions.filter { it.type.lowercase() == "income" }
            TransactionFilter.TRANSFER -> _allTransactions.filter { it.type.lowercase() == "transfer" }
        }

        // Then apply date range filter (client-side)
        val filtered = if (range != null) {
            typeFiltered.filter { tx ->
                try {
                    val txDate = java.time.Instant.parse(tx.createdAt).toEpochMilli()
                    txDate >= range.first && txDate <= range.second + 86400000L // include end day
                } catch (_: Exception) { true }
            }
        } else {
            typeFiltered
        }

        _uiState.value = TransactionsUiState.Success(
            transactions = filtered,
            hasMoreData = hasMoreData
        )
    }

    /**
     * Fetches transactions from the beginning (refresh).
     * BUG FIX 1: Opcionalmente usa parámetros de fecha para filtrar en backend
     */
    fun loadTransactions(startMillis: Long? = null, endMillis: Long? = null) {
        viewModelScope.launch {
            _uiState.value = TransactionsUiState.Loading
            _allTransactions.clear()
            currentOffset = 0
            hasMoreData = true

            // Si se pasan parámetros, usarlos; si no, usar los almacenados
            val startToUse = startMillis ?: storedStartMillis
            val endToUse = endMillis ?: storedEndMillis

            val householdId = tenantContext.getCurrentHouseholdId()
            if (householdId == null) {
                _uiState.value = TransactionsUiState.Error("No se ha seleccionado un hogar.")
                return@launch
            }

            // BUG FIX 1: Si hay filtro de fecha, pasarlo al backend (si la API lo soporta)
            // De lo contrario, se filtra en memoria con applyFilter()
            when (val result = repository.getTransactions(householdId, limit = PAGE_SIZE, offset = 0)) {
                is AppResult.Success -> {
                    _allTransactions.addAll(result.data)
                    hasMoreData = result.data.size >= PAGE_SIZE
                    currentOffset = result.data.size
                    applyFilter()
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
                    applyFilter()
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
