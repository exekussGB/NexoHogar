package com.nexohogar.presentation.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.core.util.InputSanitizer
import com.nexohogar.domain.model.Household
import com.nexohogar.domain.repository.HouseholdRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HouseholdUiState(
    val isLoading: Boolean = false,
    val households: List<Household> = emptyList(),
    val error: String? = null,

    // Sesión expirada — se activa cuando el servidor devuelve 401
    // y el refresh también falla. La UI debe redirigir al login.
    val sessionExpired: Boolean = false,

    // Crear hogar
    val isCreating: Boolean = false,
    val createError: String? = null,
    val createSuccess: Boolean = false,

    // Unirse a hogar con código
    val showJoinDialog: Boolean = false,
    val joinCode: String = "",
    val isJoining: Boolean = false,
    val joinError: String? = null,
    val joinSuccess: Boolean = false,
    val joinMessage: String? = null
)

class HouseholdViewModel(
    private val householdRepository: HouseholdRepository,
    private val tenantContext: TenantContext
) : ViewModel() {

    private val _uiState = MutableStateFlow(HouseholdUiState())
    val uiState: StateFlow<HouseholdUiState> = _uiState.asStateFlow()

    private val _autoSelectEvent = MutableSharedFlow<String>() // household ID
    val autoSelectEvent: SharedFlow<String> = _autoSelectEvent.asSharedFlow()

    init {
        loadHouseholds()
    }

    fun loadHouseholds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = householdRepository.getHouseholds()) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, households = result.data)
                    }
                    // Auto-select if there's exactly one household
                    if (result.data.size == 1) {
                        _autoSelectEvent.emit(result.data[0].id)
                    }
                }
                is AppResult.Error -> {
                    // Only match the SPECIFIC synthetic message from AuthInterceptor.
                    // This prevents network errors (503) or other messages containing
                    // "401" from being misinterpreted as irrecoverable session expiry.
                    val isAuthError = result.message?.contains("Unauthorized - session expired") == true
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = if (isAuthError) null else result.message,
                            sessionExpired = isAuthError
                        )
                    }
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Llamar desde la UI después de haber navegado al login. */
    fun clearSessionExpired() {
        _uiState.update { it.copy(sessionExpired = false) }
    }

    fun createHousehold(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(createError = "El nombre no puede estar vacío") }
            return
        }
        if (trimmedName.length > 50) {
            _uiState.update { it.copy(createError = "El nombre es demasiado largo (máx. 50 caracteres)") }
            return
        }
        val sanitizedName = InputSanitizer.sanitizeText(trimmedName, 50)
        if (sanitizedName != trimmedName) {
            _uiState.update { it.copy(createError = "El nombre contiene caracteres no permitidos") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, createError = null) }
            when (val result = householdRepository.createHousehold(sanitizedName)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            createSuccess = true,
                            households = it.households + result.data
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

    fun clearCreateError() { _uiState.update { it.copy(createError = null) } }
    fun clearCreateSuccess() { _uiState.update { it.copy(createSuccess = false) } }

    // ── Unirse a hogar con código de invitación ──────────────────────────────

    fun onShowJoinDialog() { _uiState.update { it.copy(showJoinDialog = true, joinCode = "", joinError = null) } }
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
                            isJoining = false,
                            showJoinDialog = false,
                            joinSuccess = true,
                            joinMessage = if (result.data) "¡Te uniste exitosamente!" else "Solicitud enviada, espera aprobación"
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
