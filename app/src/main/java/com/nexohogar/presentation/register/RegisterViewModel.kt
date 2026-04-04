package com.nexohogar.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.util.InputSanitizer
import com.nexohogar.core.util.PasswordValidator
import com.nexohogar.domain.repository.AuthRepository
import com.nexohogar.data.local.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * COH-04: Adaptado a AppResult (consistente con LoginViewModel).
 */
class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun register(name: String, email: String, password: String) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.isBlank() || trimmedEmail.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Todos los campos son obligatorios"
            )
            return
        }

        if (trimmedName.length < 2) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "El nombre debe tener al menos 2 caracteres"
            )
            return
        }

        if (trimmedName.length > 100) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "El nombre es demasiado largo"
            )
            return
        }

        val forbiddenChars = Regex("[<>{}\\[\\]\"'\\`;/\\\\]")
        if (forbiddenChars.containsMatchIn(trimmedName)) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "El nombre contiene caracteres no permitidos"
            )
            return
        }

        if (trimmedEmail.length > 254) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "El correo es demasiado largo"
            )
            return
        }

        if (password.length < 8) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "La contraseña debe tener al menos 8 caracteres"
            )
            return
        }

        if (!password.any { it.isUpperCase() }) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "La contraseña debe contener al menos una mayúscula"
            )
            return
        }

        if (!password.any { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "La contraseña debe contener al menos un dígito"
            )
            return
        }

        if (!password.any { !it.isLetterOrDigit() }) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "La contraseña debe contener al menos un carácter especial"
            )
            return
        }

        if (!PasswordValidator.validate(password).meetsMinimum) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "La contraseña no cumple los requisitos mínimos de seguridad"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.register(trimmedEmail, password, trimmedName)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                }
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is AppResult.Loading -> { }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
