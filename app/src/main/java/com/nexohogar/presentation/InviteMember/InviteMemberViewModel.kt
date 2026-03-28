package com.nexohogar.presentation.invitemember

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.repository.HouseholdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InviteMemberUiState(
    // Sección "Tu código"
    val inviteCode: String?      = null,
    val isLoadingCode: Boolean   = false,
    val codeError: String?       = null,

    // Sección "Unirse"
    val joinInput: String        = "",
    val isJoining: Boolean       = false,
    val joinSuccess: Boolean     = false,
    val joinError: String?       = null
)

class InviteMemberViewModel(
    private val householdRepository: HouseholdRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteMemberUiState())
    val uiState: StateFlow<InviteMemberUiState> = _uiState.asStateFlow()

    init {
        loadInviteCode()
    }

    fun loadInviteCode() {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCode = true, codeError = null)
            when (val result = householdRepository.getOrCreateInviteCode(householdId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    inviteCode    = result.data,
                    isLoadingCode = false
                )
                is AppResult.Error   -> _uiState.value = _uiState.value.copy(
                    codeError     = result.message,
                    isLoadingCode = false
                )
                is AppResult.Loading -> Unit
            }
        }
    }

    fun regenerateInviteCode() {
        val householdId = tenantContext.getCurrentHouseholdId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCode = true, codeError = null)
            when (val result = householdRepository.regenerateInviteCode(householdId)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    inviteCode    = result.data,
                    isLoadingCode = false
                )
                is AppResult.Error   -> _uiState.value = _uiState.value.copy(
                    codeError     = result.message,
                    isLoadingCode = false
                )
                is AppResult.Loading -> Unit
            }
        }
    }

    fun onJoinInputChange(value: String) {
        _uiState.value = _uiState.value.copy(
            joinInput   = value.uppercase().take(8),
            joinError   = null,
            joinSuccess = false
        )
    }

    fun joinHousehold() {
        val code = _uiState.value.joinInput.trim()
        if (code.length < 6) {
            _uiState.value = _uiState.value.copy(joinError = "El código debe tener al menos 6 caracteres")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isJoining = true, joinError = null)
            when (val result = householdRepository.joinHouseholdByCode(code)) {
                is AppResult.Success -> _uiState.value = _uiState.value.copy(
                    isJoining   = false,
                    joinSuccess = true,
                    joinInput   = ""
                )
                is AppResult.Error   -> _uiState.value = _uiState.value.copy(
                    isJoining = false,
                    joinError = result.message
                )
                is AppResult.Loading -> Unit
            }
        }
    }

    fun dismissJoinSuccess() {
        _uiState.value = _uiState.value.copy(joinSuccess = false)
    }
}