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

/**
 * ViewModel para gestionar el estado de la pantalla de cuentas.
 */
class AccountsViewModel(
    private val repository: AccountsRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountsUiState>(AccountsUiState.Loading)
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    /**
     * Carga los balances de las cuentas para el household actual.
     */
    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = AccountsUiState.Loading
            
            val householdId = tenantContext.getCurrentHouseholdId()
            if (householdId == null) {
                _uiState.value = AccountsUiState.Error("No se ha seleccionado un hogar.")
                return@launch
            }

            when (val result = repository.getAccountBalances(householdId)) {
                is AppResult.Success -> {
                    _uiState.value = AccountsUiState.Success(result.data)
                }
                is AppResult.Error -> {
                    _uiState.value = AccountsUiState.Error(result.message)
                }
                is AppResult.Loading -> { }
            }
        }
    }
}

sealed interface AccountsUiState {
    object Loading : AccountsUiState
    data class Success(val accounts: List<AccountBalance>) : AccountsUiState
    data class Error(val message: String) : AccountsUiState
}
