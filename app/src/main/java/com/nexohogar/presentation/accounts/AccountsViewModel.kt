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
import kotlinx.coroutines.launch

data class AccountsUiState(
    val accounts: List<AccountBalance> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val isCreating: Boolean = false,
    val createError: String? = null
)

class AccountsViewModel(
    private val repository: AccountsRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val householdId = tenantContext.getCurrentHouseholdId()
            if (householdId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No se ha seleccionado un hogar."
                )
                return@launch
            }

            when (val result = repository.getAccountBalances(householdId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        accounts = result.data,
                        error = null
                    )
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is AppResult.Loading -> { }
            }
        }
    }

    fun onShowCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true, createError = null)
    }

    fun onDismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false, createError = null)
    }

    fun createAccount(name: String, accountType: String, accountSubtype: String) {
        viewModelScope.launch {
            val householdId = tenantContext.getCurrentHouseholdId()
            if (householdId == null) {
                _uiState.value = _uiState.value.copy(
                    createError = "No se ha seleccionado un hogar."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isCreating = true, createError = null)

            when (val result = repository.createAccount(householdId, name, accountType, accountSubtype)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        showCreateDialog = false,
                        createError = null
                    )
                    loadAccounts()
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        createError = result.message
                    )
                }
                is AppResult.Loading -> { }
            }
        }
    }
}