package com.nexohogar.presentation.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexohogar.domain.repository.AuthRepository
import com.nexohogar.core.AppResult
import com.nexohogar.presentation.ResetPasswordTokenHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ResetPasswordUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class ResetPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    private val accessToken: String = ResetPasswordTokenHolder.token ?: ""

    fun resetPassword(newPassword: String) {
        if (accessToken.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Token de recuperación no válido. Solicita un nuevo enlace."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.updatePassword(accessToken, newPassword)
            if (result is AppResult.Success<*>) {
                ResetPasswordTokenHolder.token = null  // Limpiar token usado
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = (result as? AppResult.Error)?.message ?: "Error desconocido"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ResetPasswordViewModel(authRepository) as T
        }
    }
}