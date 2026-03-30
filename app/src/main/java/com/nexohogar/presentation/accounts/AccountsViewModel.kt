package com.nexohogar.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.model.Transaction
import com.nexohogar.domain.repository.AccountsRepository
import com.nexohogar.domain.repository.TransactionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountsUiState(
    val sharedAccounts: List<AccountBalance> = emptyList(),
    val personalAccounts: List<AccountBalance> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val newAccountName: String = "",
    val newAccountSubtype: String = "cash",
    val newAccountIsShared: Boolean = true,
    val isCreating: Boolean = false,
    val showDeleteConfirm: String? = null, // accountId to delete
    val currentUserId: String? = null,
    val selectedAccount: AccountBalance? = null,
    val selectedAccountTransactions: List<Transaction> = emptyList(),
    val isLoadingTransactions: Boolean = false
)

class AccountsViewModel(
    private val accountsRepository: AccountsRepository,
    private val transactionsRepository: TransactionsRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        val userId = tenantContext.getCurrentUserId()
        _uiState.update { it.copy(isLoading = true, error = null, currentUserId = userId) }

        viewModelScope.launch {
            when (val result = accountsRepository.getAccountBalances(householdId)) {
                is AppResult.Success -> {
                    val shared = result.data.filter { it.isShared }
                    val personal = result.data.filter { !it.isShared && it.ownerUserId == userId }
                    _uiState.update {
                        it.copy(
                            sharedAccounts   = shared,
                            personalAccounts = personal,
                            isLoading        = false
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── ✅ NUEVO: Seleccionar cuenta y cargar sus movimientos ──────────────
    fun selectAccount(account: AccountBalance) {
        _uiState.update {
            it.copy(
                selectedAccount = account,
                selectedAccountTransactions = emptyList(),
                isLoadingTransactions = true
            )
        }
        loadAccountTransactions(account.accountId)
    }

    fun dismissAccountDetail() {
        _uiState.update {
            it.copy(
                selectedAccount = null,
                selectedAccountTransactions = emptyList(),
                isLoadingTransactions = false
            )
        }
    }

    private fun loadAccountTransactions(accountId: String) {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return

        viewModelScope.launch {
            when (val result = transactionsRepository.getTransactionsByAccount(
                householdId = householdId,
                accountId = accountId,
                limit = 10 // Últimos 10 movimientos
            )) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedAccountTransactions = result.data,
                            isLoadingTransactions = false
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(isLoadingTransactions = false)
                    }
                }
                else -> {
                    _uiState.update { it.copy(isLoadingTransactions = false) }
                }
            }
        }
    }

    // ── Create Dialog ────────────────────────────────────────────────────────
    fun showCreateDialog()  { _uiState.update { it.copy(showCreateDialog = true, newAccountName = "", newAccountSubtype = "cash", newAccountIsShared = true) } }
    fun dismissCreateDialog() { _uiState.update { it.copy(showCreateDialog = false) } }
    fun onNameChange(name: String) { _uiState.update { it.copy(newAccountName = name) } }
    fun onSubtypeChange(subtype: String) { _uiState.update { it.copy(newAccountSubtype = subtype) } }
    fun onIsSharedChange(shared: Boolean) { _uiState.update { it.copy(newAccountIsShared = shared) } }

    fun createAccount() {
        val state = _uiState.value
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        val name = state.newAccountName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "El nombre es obligatorio") }
            return
        }

        val accountType = when (state.newAccountSubtype) {
            "credit_card" -> "LIABILITY"
            else -> "ASSET"
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            when (val result = accountsRepository.createAccount(
                householdId    = householdId,
                name           = name,
                accountType    = accountType,
                accountSubtype = state.newAccountSubtype,
                isShared       = state.newAccountIsShared,
                ownerUserId    = if (!state.newAccountIsShared) state.currentUserId else null
            )) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isCreating = false, showCreateDialog = false) }
                    loadAccounts()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isCreating = false, error = result.message) }
                }
                else -> _uiState.update { it.copy(isCreating = false) }
            }
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────
    fun showDeleteConfirm(accountId: String) { _uiState.update { it.copy(showDeleteConfirm = accountId) } }
    fun dismissDeleteConfirm() { _uiState.update { it.copy(showDeleteConfirm = null) } }

    fun deleteAccount() {
        val accountId = _uiState.value.showDeleteConfirm ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDeleteConfirm = null) }
            when (val result = accountsRepository.deleteAccount(accountId)) {
                is AppResult.Success -> loadAccounts()
                is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}
