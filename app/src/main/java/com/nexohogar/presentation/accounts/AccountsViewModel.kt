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
    val createError: String? = null,
    // Nuevo: estado de eliminación
    val accountToDelete: AccountBalance? = null,
    val isDeleting: Boolean = false,
    val deleteError: String? = null
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

            // Usar saldos calculados en vez del campo estático
            when (val result = repository.getCalculatedBalances(householdId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        accounts = result.data,
                        error = null
                    )
                }
                is AppResult.Error -> {
                    // Fallback al método anterior si el RPC no existe aún
                    when (val fallback = repository.getAccountBalances(householdId)) {
                        is AppResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                accounts = fallback.data,
                                error = null
                            )
                        }
                        is AppResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = fallback.message
                            )
                        }
                        is AppResult.Loading -> { }
                    }
                }
                is AppResult.Loading -> { }
            }
        }
    }

    // ── Crear cuenta ──

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

    // ── Eliminar cuenta ──

    fun onRequestDelete(account: AccountBalance) {
        _uiState.value = _uiState.value.copy(
            accountToDelete = account,
            deleteError = null
        )
    }

    fun onDismissDelete() {
        _uiState.value = _uiState.value.copy(
            accountToDelete = null,
            deleteError = null
        )
    }

    fun confirmDelete() {
        val account = _uiState.value.accountToDelete ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, deleteError = null)

            when (val result = repository.deleteAccount(account.accountId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        accountToDelete = null,
                        deleteError = null
                    )
                    loadAccounts()
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        deleteError = result.message
                    )
                }
                is AppResult.Loading -> { }
            }
        }
    }
}