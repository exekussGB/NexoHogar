package com.nexohogar.presentation.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.util.PasswordValidator
import com.nexohogar.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ResetPasswordUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * SEC-05: El token se recibe como parámetro del constructor en lugar de
 * leerlo del singleton ResetPasswordTokenHolder (eliminado).
 */
class ResetPasswordViewModel(
    private val authRepository: AuthRepository,
    private val accessToken: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun resetPassword(newPassword: String) {
        if (accessToken.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Token de recuperación no válido. Solicita un nuevo código."
            )
            return
        }

        if (newPassword.length < 8) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "La contraseña debe tener al menos 8 caracteres"
            )
            return
        }

        if (newPassword.length > 128) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "La contraseña es demasiado larga"
            )
            return
        }

        if (!newPassword.any { it.isUpperCase() }) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "La contraseña debe contener al menos una mayúscula"
            )
            return
        }

        if (!newPassword.any { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "La contraseña debe contener al menos un dígito"
            )
            return
        }

        if (!newPassword.any { !it.isLetterOrDigit() }) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "La contraseña debe contener al menos un carácter especial"
            )
            return
        }

        if (!PasswordValidator.validate(newPassword).meetsMinimum) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "La contraseña no cumple los requisitos mínimos de seguridad"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.updatePassword(accessToken, newPassword)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
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

    class Factory(
        private val authRepository: AuthRepository,
        private val accessToken: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ResetPasswordViewModel(authRepository, accessToken) as T
        }
    }
}
