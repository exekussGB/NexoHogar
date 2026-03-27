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
    val pendingMembers: List<HouseholdMember> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isCurrentUserSuperUser: Boolean = false,
    val currentUserId: String? = null,
    val memberToRemove: HouseholdMember? = null,
    val isRemoving: Boolean = false,
    val actionMessage: String? = null
)

class HouseholdMembersViewModel(
    private val householdRepository: HouseholdRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(HouseholdMembersUiState())
    val uiState: StateFlow<HouseholdMembersUiState> = _uiState.asStateFlow()

    init {
        loadMembers()
    }

    fun loadMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val householdId = tenantContext.getCurrentHouseholdId()
            val currentUserId = tenantContext.getCurrentUserId()

            if (householdId == null) {
                _uiState.update { it.copy(isLoading = false, error = "No se ha seleccionado un hogar.") }
                return@launch
            }

            when (val result = householdRepository.getHouseholdMembers(householdId)) {
                is AppResult.Success -> {
                    val isSuperUser = result.data.any {
                        it.userId == currentUserId &&
                        (it.role.lowercase() == "super_user" || it.role.lowercase() == "admin")
                    }

                    // Solo cargar pendientes si es super_user
                    val pending = if (isSuperUser) {
                        when (val pendingResult = householdRepository.getPendingMembers(householdId)) {
                            is AppResult.Success -> pendingResult.data
                            else -> emptyList()
                        }
                    } else emptyList()

                    _uiState.update {
                        it.copy(
                            isLoading            = false,
                            members              = result.data.filter { m -> m.isActive },
                            pendingMembers       = pending,
                            isCurrentUserSuperUser = isSuperUser,
                            currentUserId        = currentUserId,
                            error                = null
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is AppResult.Loading -> { }
            }
        }
    }

    fun acceptMember(member: HouseholdMember) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRemoving = true) }
            when (val result = householdRepository.acceptMember(member.userId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(
                        isRemoving = false,
                        actionMessage = "${member.label()} fue aceptado en el hogar"
                    )}
                    loadMembers()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isRemoving = false, error = result.message) }
                }
                else -> _uiState.update { it.copy(isRemoving = false) }
            }
        }
    }

    fun rejectMember(member: HouseholdMember) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRemoving = true) }
            when (val result = householdRepository.rejectMember(member.userId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(
                        isRemoving = false,
                        actionMessage = "${member.label()} fue rechazado"
                    )}
                    loadMembers()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isRemoving = false, error = result.message) }
                }
                else -> _uiState.update { it.copy(isRemoving = false) }
            }
        }
    }

    fun showRemoveConfirm(member: HouseholdMember) {
        _uiState.update { it.copy(memberToRemove = member) }
    }

    fun dismissRemoveConfirm() {
        _uiState.update { it.copy(memberToRemove = null) }
    }

    fun removeMember() {
        val member = _uiState.value.memberToRemove ?: return
        val householdId = tenantContext.getCurrentHouseholdId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isRemoving = true, memberToRemove = null) }

            when (val result = householdRepository.removeHouseholdMember(householdId, member.userId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isRemoving = false) }
                    loadMembers()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isRemoving = false, error = result.message) }
                }
                else -> _uiState.update { it.copy(isRemoving = false) }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun clearActionMessage() { _uiState.update { it.copy(actionMessage = null) } }
}
