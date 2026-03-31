package com.nexohogar.presentation.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.local.SessionManager
import com.nexohogar.domain.model.WishlistItem
import com.nexohogar.domain.repository.WishlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WishlistUiState(
    val items: List<WishlistItem>     = emptyList(),
    val isLoading: Boolean            = false,
    val error: String?                = null,

    // Crear
    val showCreateDialog: Boolean     = false,
    val isCreating: Boolean           = false,
    val createError: String?          = null,

    // Editar
    val itemToEdit: WishlistItem?     = null,
    val isEditing: Boolean            = false,
    val editError: String?            = null
)

class WishlistViewModel(
    private val repository: WishlistRepository,
    private val tenantContext: TenantContext,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WishlistUiState())
    val uiState: StateFlow<WishlistUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    // ── Carga ────────────────────────────────────────────────────────────────

    fun loadItems() {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getWishlistItems(householdId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(items = result.data, isLoading = false)
                }
                is AppResult.Error   -> _uiState.update {
                    it.copy(error = result.message, isLoading = false)
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    // ── Crear ────────────────────────────────────────────────────────────────

    fun onShowCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, createError = null) }
    }

    fun onDismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, createError = null) }
    }

    fun createItem(name: String, estimatedCost: Long, notes: String?, priority: Int) {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        val userId = sessionManager.fetchSession()?.userId ?: ""
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, createError = null) }
            when (val result = repository.createWishlistItem(householdId, name, estimatedCost, notes, priority, userId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isCreating = false, showCreateDialog = false) }
                    loadItems()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(createError = result.message, isCreating = false)
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    // ── Editar ───────────────────────────────────────────────────────────────

    fun onShowEditDialog(item: WishlistItem) {
        _uiState.update { it.copy(itemToEdit = item, editError = null) }
    }

    fun onDismissEditDialog() {
        _uiState.update { it.copy(itemToEdit = null, editError = null) }
    }

    fun updateItem(itemId: String, name: String, estimatedCost: Long, notes: String?, priority: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEditing = true, editError = null) }
            when (val result = repository.updateWishlistItem(itemId, name, estimatedCost, notes, priority)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isEditing = false, itemToEdit = null) }
                    loadItems()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(editError = result.message, isEditing = false)
                }
                is AppResult.Loading -> Unit
            }
        }
    }

    // ── Comprar ──────────────────────────────────────────────────────────────

    fun markAsPurchased(item: WishlistItem) {
        val userId = sessionManager.fetchSession()?.userId ?: ""
        viewModelScope.launch {
            when (repository.markAsPurchased(item.id, userId)) {
                is AppResult.Success -> loadItems()
                is AppResult.Error   -> _uiState.update { it.copy(error = "Error al marcar como comprado") }
                is AppResult.Loading -> Unit
            }
        }
    }

    // ── Eliminar ─────────────────────────────────────────────────────────────

    fun deleteItem(item: WishlistItem) {
        viewModelScope.launch {
            when (repository.deleteWishlistItem(item.id)) {
                is AppResult.Success -> loadItems()
                is AppResult.Error   -> _uiState.update { it.copy(error = "Error al eliminar") }
                is AppResult.Loading -> Unit
            }
        }
    }
}
