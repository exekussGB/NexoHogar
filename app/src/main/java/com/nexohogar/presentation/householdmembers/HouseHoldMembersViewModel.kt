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
import kotlinx.coroutines.launch

data class HouseholdMembersUiState(
    val members: List<HouseholdMember> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
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
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        members   = result.data,
                        error     = null
                    )
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error     = result.message
                    )
                }
                is AppResult.Loading -> { }
            }
        }
    }
}
