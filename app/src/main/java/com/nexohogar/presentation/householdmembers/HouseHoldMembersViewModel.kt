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
    val isCurrentUserAdmin: Boolean = false,
    val currentUserId: String? = null,
    val memberToRemove: HouseholdMember? = null,
    val isRemoving: Boolean = false
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
                    val isAdmin = result.data.any {
                        it.userId == currentUserId && it.role.lowercase() == "admin"
                    }
                    _uiState.update {
                        it.copy(
                            isLoading         = false,
                            members           = result.data,
                            isCurrentUserAdmin = isAdmin,
                            currentUserId     = currentUserId,
                            error             = null
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
}
