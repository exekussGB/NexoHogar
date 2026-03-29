package com.nexohogar.presentation.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.repository.HouseholdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeleteHouseholdUiState(
    val showFirstDialog: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
    val householdName: String = "",
    val householdId: String = ""
)

class DeleteHouseholdViewModel(
    private val householdRepository: HouseholdRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeleteHouseholdUiState())
    val uiState: StateFlow<DeleteHouseholdUiState> = _uiState.asStateFlow()

    fun startDeleteFlow(householdId: String, householdName: String) {
        _uiState.value = _uiState.value.copy(
            showFirstDialog = true,
            householdId = householdId,
            householdName = householdName
        )
    }

    fun confirmFirstStep() {
        _uiState.value = _uiState.value.copy(
            showFirstDialog = false,
            showConfirmDialog = true
        )
    }

    fun cancelDelete() {
        _uiState.value = DeleteHouseholdUiState()
    }

    fun confirmDelete(typedName: String) {
        if (typedName.trim().equals(_uiState.value.householdName.trim(), ignoreCase = true).not()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "El nombre no coincide con el hogar"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, errorMessage = null)

            when (val result = householdRepository.deleteHousehold(
                _uiState.value.householdId,
                typedName.trim()
            )) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        isDeleted = true,
                        showConfirmDialog = false
                    )
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        errorMessage = result.message
                    )
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    class Factory(private val householdRepository: HouseholdRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeleteHouseholdViewModel(householdRepository) as T
        }
    }
}
