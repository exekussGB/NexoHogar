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
    val isLoading: Boolean            = false,
    val households: List<Household>   = emptyList(),
    val error: String?                = null,
    val isCreating: Boolean           = false,
    val createError: String?          = null,
    val createSuccess: Boolean        = false
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

    fun clearCreateError() {
        _uiState.update { it.copy(createError = null) }
    }

    fun clearCreateSuccess() {
        _uiState.update { it.copy(createSuccess = false) }
    }

    fun selectHousehold(household: Household) {
        tenantContext.setHouseholdId(household.id)
    }
}