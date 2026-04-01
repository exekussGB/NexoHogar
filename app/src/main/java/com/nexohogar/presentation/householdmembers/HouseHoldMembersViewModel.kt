package com.nexohogar.presentation.householdmembers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.model.HouseholdMember
import com.nexohogar.domain.repository.HouseholdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HouseholdMembersUiState(
    val members: List<HouseholdMember> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isProcessing: Boolean = false,
    val actionMessage: String? = null,
    // ── NUEVO: Para eliminar miembros ─────────────────
    val currentUserId: String? = null,
    val currentUserRole: String = "",
    val memberToRemove: HouseholdMember? = null   // Diálogo de confirmación
)

class HouseholdMembersViewModel(
    private val householdRepository: HouseholdRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(HouseholdMembersUiState())
    val uiState: StateFlow<HouseholdMembersUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(currentUserId = tenantContext.getCurrentUserId()) }
        loadMembers()
    }

    fun loadMembers() {
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

            when (val result = householdRepository.getHouseholdMembers(householdId)) {
                is AppResult.Success -> {
                    val currentId = _uiState.value.currentUserId
                    val role = result.data
                        .firstOrNull { it.userId == currentId }
                        ?.role ?: ""

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        members = result.data,
                        currentUserRole = role,
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

    // ── Aceptar miembro pendiente ────────────────────────────────

    fun acceptMember(memberId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, actionMessage = null) }
            when (val result = householdRepository.acceptMember(memberId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(
                        isProcessing = false,
                        actionMessage = "Miembro aceptado ✓"
                    )}
                    loadMembers()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(
                        isProcessing = false,
                        error = result.message
                    )}
                }
                is AppResult.Loading -> {}
            }
        }
    }

    // ── Rechazar miembro pendiente ────────────────────────────────

    fun rejectMember(memberId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, actionMessage = null) }
            when (val result = householdRepository.rejectMember(memberId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(
                        isProcessing = false,
                        actionMessage = "Miembro rechazado"
                    )}
                    loadMembers()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(
                        isProcessing = false,
                        error = result.message
                    )}
                }
                is AppResult.Loading -> {}
            }
        }
    }

    // ── NUEVO: Eliminar miembro activo (solo super_user) ─────────

    /** Muestra diálogo de confirmación */
    fun requestRemoveMember(member: HouseholdMember) {
        _uiState.update { it.copy(memberToRemove = member) }
    }

    /** Cierra diálogo sin eliminar */
    fun cancelRemoveMember() {
        _uiState.update { it.copy(memberToRemove = null) }
    }

    /** Confirma eliminación */
    fun confirmRemoveMember() {
        val member = _uiState.value.memberToRemove ?: return
        val householdId = tenantContext.getCurrentHouseholdId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, memberToRemove = null, actionMessage = null) }
            when (val result = householdRepository.removeMember(householdId, member.userId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(
                        isProcessing = false,
                        actionMessage = "Miembro eliminado ✓"
                    )}
                    loadMembers()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(
                        isProcessing = false,
                        actionMessage = result.message
                    )}
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}
