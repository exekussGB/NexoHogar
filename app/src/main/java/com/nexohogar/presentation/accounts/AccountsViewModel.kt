package com.nexohogar.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.repository.AccountsRepository
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
    val currentUserId: String? = null
)

class AccountsViewModel(
    private val accountsRepository: AccountsRepository,
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
