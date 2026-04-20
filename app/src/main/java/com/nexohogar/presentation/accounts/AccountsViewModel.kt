package com.nexohogar.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.core.util.InputSanitizer
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.model.Transaction
import com.nexohogar.domain.repository.AccountsRepository
import com.nexohogar.domain.repository.TransactionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

data class AccountsUiState(
    val sharedAccounts: List<AccountBalance> = emptyList(),
    val personalAccounts: List<AccountBalance> = emptyList(),
    val savingsAccounts: List<AccountBalance> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val newAccountName: String = "",
    val newAccountSubtype: String = "cash",
    val newAccountIsShared: Boolean = true,
    val newAccountIsSavings: Boolean = false,
    val isCreating: Boolean = false,
    val newAccountHasInitialBalance: Boolean = false,
    val newAccountInitialBalance: String = "",
    val newAccountIcon: String? = null,              // 🆕 Custom icon for create dialog
    val newAccountCreditLimit: String = "",          // 🆕 Feature 4
    val showDeleteConfirm: String? = null, // accountId to delete
    val currentUserId: String? = null,
    val selectedAccount: AccountBalance? = null,
    val selectedAccountTransactions: List<Transaction> = emptyList(),
    val isLoadingTransactions: Boolean = false,
    // Phase 3: grouped accounts by subtype
    val accountsBySubtype: Map<String, List<AccountBalance>> = emptyMap(),
    // Phase 3: super user flag for edit swipe
    val isSuperUser: Boolean = false,
    // Phase 3: edit dialog
    val showEditDialog: String? = null, // accountId to edit
    // Phase 3: edit dialog form state
    val editAccountName: String = "",
    val editAccountIsSavings: Boolean = false,
    val editAccountIsShared: Boolean = true,
    val editAccountIcon: String? = null,             // 🆕 Custom icon for edit dialog
    val editAccountCreditLimit: String = "",         // 🆕 Feature 4
    val isSavingEdit: Boolean = false
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
        checkSuperUser()
    }

    private fun checkSuperUser() {
        val role = tenantContext.getCurrentUserRole()
        _uiState.update { it.copy(isSuperUser = role == "super_user" || role == "admin") }
    }

    fun loadAccounts() {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        val userId = tenantContext.getCurrentUserId()
        _uiState.update { it.copy(isLoading = true, error = null, currentUserId = userId) }

        viewModelScope.launch {
            when (val result = accountsRepository.getAccountBalances(householdId)) {
                is AppResult.Success -> {
                    // BUG FIX 3: Filtrar cuentas personales para que solo se muestren al propietario
                    val allAccounts = result.data

                    // Filtro: Cuentas compartidas visibles para todos
                    val shared = allAccounts.filter { it.isShared && !it.isSavings }

                    // Filtro: Cuentas personales solo visibles para el propietario
                    val personal = allAccounts.filter { account ->
                        !account.isShared &&
                                !account.isSavings &&
                                account.ownerUserId == userId  // ✅ Solo mostrar si el usuario actual es el propietario
                    }

                    // Ahorros visibles para todos
                    val savings = allAccounts.filter { it.isSavings }

                    // Group all accounts by subtype for section display
                    // BUG FIX 3: También filtrar en la agrupación por subtipo
                    val bySubtype = allAccounts
                        .filter { account ->
                            // Incluir si es compartida O si es personal pero del usuario actual
                            account.isShared || account.ownerUserId == userId
                        }
                        .groupBy { it.accountType }

                    _uiState.update {
                        it.copy(
                            sharedAccounts   = shared,
                            personalAccounts = personal,
                            savingsAccounts  = savings,
                            accountsBySubtype = bySubtype,
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

    // ── Seleccionar cuenta y cargar sus movimientos ──────────────────
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
                limit = 10
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
    fun showCreateDialog()  { _uiState.update { it.copy(showCreateDialog = true, newAccountName = "", newAccountSubtype = "cash", newAccountIsShared = true, newAccountIsSavings = false, newAccountHasInitialBalance = false, newAccountInitialBalance = "", newAccountIcon = null, newAccountCreditLimit = "") } }
    fun dismissCreateDialog() { _uiState.update { it.copy(showCreateDialog = false) } }
    fun onNameChange(name: String) { _uiState.update { it.copy(newAccountName = name) } }
    fun onSubtypeChange(subtype: String) { _uiState.update { it.copy(newAccountSubtype = subtype) } }
    fun onIsSharedChange(shared: Boolean) { _uiState.update { it.copy(newAccountIsShared = shared) } }
    fun onIsSavingsChange(savings: Boolean) { _uiState.update { it.copy(newAccountIsSavings = savings) } }
    fun onHasInitialBalanceChange(has: Boolean) { _uiState.update { it.copy(newAccountHasInitialBalance = has, newAccountInitialBalance = if (!has) "" else it.newAccountInitialBalance) } }
    fun onInitialBalanceChange(amount: String) { _uiState.update { it.copy(newAccountInitialBalance = amount) } }
    fun onIconChange(icon: String?) { _uiState.update { it.copy(newAccountIcon = icon) } }                    // 🆕 Custom icon
    fun onCreditLimitChange(limit: String) { _uiState.update { it.copy(newAccountCreditLimit = limit) } }    // 🆕 Feature 4

    fun createAccount() {
        val state = _uiState.value
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        val name = state.newAccountName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        if (name.length > 50) {
            _uiState.update { it.copy(error = "El nombre es demasiado largo (máx. 50 caracteres)") }
            return
        }
        val sanitizedName = InputSanitizer.sanitizeText(name, 50)

        val initialBalance = if (state.newAccountHasInitialBalance) {
            val parsed = state.newAccountInitialBalance.toDoubleOrNull()
            if (parsed == null) {
                _uiState.update { it.copy(error = "Ingresa un saldo inicial válido") }
                return
            }
            if (abs(parsed) > 999_999_999) {
                _uiState.update { it.copy(error = "Ingresa un saldo inicial válido") }
                return
            }
            parsed
        } else {
            null
        }

        val creditLimit = state.newAccountCreditLimit.toLongOrNull()

        val accountType = when (state.newAccountSubtype) {
            "credit_card" -> "LIABILITY"
            else -> "ASSET"
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            when (val result = accountsRepository.createAccount(
                householdId    = householdId,
                name           = sanitizedName,
                accountType    = accountType,
                accountSubtype = state.newAccountSubtype,
                isShared       = state.newAccountIsShared,
                ownerUserId    = if (!state.newAccountIsShared) state.currentUserId else null,
                initialBalanceCLP = initialBalance,
                isSavings      = state.newAccountIsSavings,
                icon           = state.newAccountIcon,           // 🆕 Custom icon
                creditLimit    = creditLimit                      // 🆕 Feature 4
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

    // ── Edit account ────────────────────────────────────────────────────────
    fun showEditDialog(accountId: String) {
        val all = _uiState.value.sharedAccounts +
                _uiState.value.personalAccounts +
                _uiState.value.savingsAccounts
        val account = all.find { it.accountId == accountId }
        _uiState.update {
            it.copy(
                showEditDialog       = accountId,
                editAccountName      = account?.accountName ?: "",
                editAccountIsSavings = account?.isSavings  ?: false,
                editAccountIsShared  = account?.isShared   ?: true,
                editAccountIcon      = account?.icon,                // 🆕 Custom icon
                editAccountCreditLimit = account?.creditLimit?.toString() ?: "", // 🆕 Feature 4
                error                = null
            )
        }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(showEditDialog = null, error = null) }
    }

    fun onEditNameChange(name: String) {
        _uiState.update { it.copy(editAccountName = name) }
    }

    fun onEditIsSavingsChange(isSavings: Boolean) {
        _uiState.update { it.copy(editAccountIsSavings = isSavings) }
    }

    fun onEditIsSharedChange(isShared: Boolean) {
        _uiState.update { it.copy(editAccountIsShared = isShared) }
    }

    fun onEditIconChange(icon: String?) {                            // 🆕 Custom icon
        _uiState.update { it.copy(editAccountIcon = icon) }
    }

    fun onEditCreditLimitChange(limit: String) {                     // 🆕 Feature 4
        _uiState.update { it.copy(editAccountCreditLimit = limit) }
    }

    fun saveEditAccount() {
        val state = _uiState.value
        val accountId = state.showEditDialog ?: return
        val name = InputSanitizer.sanitizeText(state.editAccountName.trim(), 50)
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        val creditLimit = state.editAccountCreditLimit.toLongOrNull()

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingEdit = true, error = null) }
            when (val result = accountsRepository.updateAccount(
                accountId = accountId,
                name      = name,
                isSavings = state.editAccountIsSavings,
                isShared  = state.editAccountIsShared,
                icon      = state.editAccountIcon,              // 🆕 Custom icon
                creditLimit = creditLimit                       // 🆕 Feature 4
            )) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSavingEdit = false, showEditDialog = null) }
                    loadAccounts()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isSavingEdit = false, error = result.message) }
                }
                else -> _uiState.update { it.copy(isSavingEdit = false) }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}
