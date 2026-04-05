package com.nexohogar.presentation.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.core.util.InputSanitizer
import com.nexohogar.domain.model.Household
import com.nexohogar.domain.repository.HouseholdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HouseholdUiState(
    val isLoading: Boolean = false,
    val households: List<Household> = emptyList(),
    val error: String? = null,

    // Sesión expirada
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
    val joinMessage: String? = null,

    // Apariencia (imagen / gradiente)
    val isUploadingImage: Boolean = false,
    val uploadError: String? = null
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
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, households = result.data)
                    }
                }
                is AppResult.Error -> {
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
        viewModelScope.launch {
            val userId = tenantContext.getCurrentUserId()
            if (userId != null) {
                when (val result = householdRepository.getHouseholdMembers(household.id)) {
                    is AppResult.Success -> {
                        val myRole = result.data.firstOrNull { it.userId == userId }?.role ?: "user"
                        tenantContext.setCurrentUserRole(myRole)
                    }
                    else -> tenantContext.setCurrentUserRole("user")
                }
            }
        }
    }

    // ── Apariencia: gradiente ────────────────────────────────────────────────

    fun updateGradient(householdId: String, gradientIndex: Int) {
        // Update locally immediately for instant feedback
        _uiState.update { state ->
            state.copy(
                households = state.households.map { h ->
                    if (h.id == householdId) h.copy(gradientIndex = gradientIndex, imageUri = null) else h
                }
            )
        }
        // Persist to Supabase
        viewModelScope.launch {
            householdRepository.updateHouseholdAppearance(
                householdId = householdId,
                imageUrl = "", // clear image when selecting gradient
                gradientIndex = gradientIndex
            )
        }
    }

    // ── Apariencia: subir imagen ─────────────────────────────────────────────

    fun uploadImage(householdId: String, imageBytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true, uploadError = null) }

            when (val uploadResult = householdRepository.uploadHouseholdImage(householdId, imageBytes, mimeType)) {
                is AppResult.Success -> {
                    val imageUrl = uploadResult.data
                    // Update the appearance in Supabase
                    householdRepository.updateHouseholdAppearance(
                        householdId = householdId,
                        imageUrl = imageUrl,
                        gradientIndex = null
                    )
                    // Update local state
                    _uiState.update { state ->
                        state.copy(
                            isUploadingImage = false,
                            households = state.households.map { h ->
                                if (h.id == householdId) h.copy(imageUri = imageUrl) else h
                            }
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isUploadingImage = false, uploadError = uploadResult.message) }
                }
                else -> _uiState.update { it.copy(isUploadingImage = false) }
            }
        }
    }

    fun clearUploadError() { _uiState.update { it.copy(uploadError = null) } }
}
