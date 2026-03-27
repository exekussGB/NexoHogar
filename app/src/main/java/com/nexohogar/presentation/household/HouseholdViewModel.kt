package com.nexohogar.presentation.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.domain.model.Household
import com.nexohogar.domain.repository.HouseholdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HouseholdUiState(
    val isLoading: Boolean           = false,
    val households: List<Household>  = emptyList(),
    val error: String?               = null,

    // Crear hogar
    val isCreating: Boolean          = false,
    val createError: String?         = null,
    val createSuccess: Boolean       = false,

    // Unirse a hogar con código
    val showJoinDialog: Boolean      = false,
    val joinCode: String             = "",
    val isJoining: Boolean           = false,
    val joinError: String?           = null,
    val joinSuccess: Boolean         = false,
    val joinMessage: String?         = null  // Mensaje del servidor (ej: "Solicitud enviada...")
)

class HouseholdViewModel(
    private val householdRepository: HouseholdRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(HouseholdUiState())
    val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

    init {
        loadHouseholds()
    }

    fun loadHouseholds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = householdRepository.getHouseholds()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isLoading = false, households = result.data)
                }
                is AppResult.Error   -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun createHousehold(name: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(createError = "El nombre no puede estar vacío") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, createError = null) }
            when (val result = householdRepository.createHousehold(name.trim())) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreating    = false,
                            createSuccess = true,
                            households    = it.households + result.data
                        )
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(isCreating = false, createError = result.message)
                }
                else -> _uiState.update { it.copy(isCreating = false) }
            }
        }
    }

    fun clearCreateError()   { _uiState.update { it.copy(createError = null) } }
    fun clearCreateSuccess() { _uiState.update { it.copy(createSuccess = false) } }

    // ── Unirse a hogar con código de invitación ──────────────────────────────

    fun onShowJoinDialog()  { _uiState.update { it.copy(showJoinDialog = true, joinCode = "", joinError = null) } }
    fun onDismissJoinDialog() { _uiState.update { it.copy(showJoinDialog = false, joinCode = "", joinError = null) } }
    fun onJoinCodeChange(code: String) { _uiState.update { it.copy(joinCode = code) } }

    fun joinHousehold() {
        val code = _uiState.value.joinCode.trim()
        if (code.isBlank()) {
            _uiState.update { it.copy(joinError = "Ingresa el código de invitación") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, joinError = null) }
            when (val result = householdRepository.joinHouseholdByCode(code)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isJoining      = false,
                            showJoinDialog = false,
                            joinSuccess    = true,
                            joinMessage    = result.data
                        )
                    }
                    // Recargar hogares por si fue aceptado automáticamente
                    loadHouseholds()
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(isJoining = false, joinError = result.message)
                }
                else -> _uiState.update { it.copy(isJoining = false) }
            }
        }
    }

    fun clearJoinSuccess() { _uiState.update { it.copy(joinSuccess = false, joinMessage = null) } }

    fun selectHousehold(household: Household) {
        tenantContext.setHouseholdId(household.id)
    }
}
