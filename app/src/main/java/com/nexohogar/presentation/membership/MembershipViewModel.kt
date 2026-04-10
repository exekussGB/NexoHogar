package com.nexohogar.presentation.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.model.UserUsage
import com.nexohogar.domain.repository.MembershipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// FIX: Reescrito para que coincida con lo que MembershipScreen consume:
//   - viewModel.usage       → StateFlow<UserUsage?>
//   - viewModel.isLoading   → StateFlow<Boolean>
//   - viewModel.error       → StateFlow<String?>
//   - viewModel.loadUsage() → función de carga
//
// Se eliminó el sealed MembershipUiState y la dependencia a authRepository.
// Constructor reducido a 1 parámetro (repository).
// Se usa AppResult igual que el resto de ViewModels del proyecto.
class MembershipViewModel(
    private val repository: MembershipRepository
) : ViewModel() {

    private val _usage = MutableStateFlow<UserUsage?>(null)
    val usage: StateFlow<UserUsage?> = _usage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadUsage(householdId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val result = repository.getUserUsage(householdId)) {
                is AppResult.Success -> _usage.value = result.data
                is AppResult.Error   -> _error.value = result.message
                is AppResult.Loading -> Unit
            }
            _isLoading.value = false
        }
    }

    // ─── Helpers para pantallas con límites (Fase 4) ────────────────────────
    fun canAddProduct()       = _usage.value?.products?.isAtLimit?.not()    ?: true
    fun canAddInventoryItem() = _usage.value?.inventory?.isAtLimit?.not()   ?: true
    fun canAddWishlistItem()  = _usage.value?.wishlist?.isAtLimit?.not()    ?: true
    fun canAddSuggestion()    = _usage.value?.suggestions?.isAtLimit?.not() ?: true
    fun canAddRecurring()     = _usage.value?.recurring?.isAtLimit?.not()   ?: true
    fun canAddAccount()       = _usage.value?.accounts?.isAtLimit?.not()    ?: true
}
