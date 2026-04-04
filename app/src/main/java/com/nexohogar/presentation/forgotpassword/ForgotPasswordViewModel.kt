package com.nexohogar.presentation.forgotpassword

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.util.AppLogger
import com.nexohogar.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * COH-05: Migrado de MutableLiveData a MutableStateFlow.
 * SEC-04: Logs via AppLogger.
 */
class ForgotPasswordViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    private var lastSendTime = 0L

    fun sendRecoveryEmail(email: String) {
        if (System.currentTimeMillis() - lastSendTime < 60_000) {
            _state.value = ForgotPasswordState.Error("Espera un momento antes de reenviar")
            return
        }

        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _state.value = ForgotPasswordState.Error("Ingresa un correo válido")
            return
        }

        lastSendTime = System.currentTimeMillis()
        _state.value = ForgotPasswordState.Loading
        viewModelScope.launch {
            AppLogger.d("ForgotPasswordVM", "Enviando correo de recuperación")
            when (val result = repository.forgotPassword(trimmedEmail)) {
                is AppResult.Success -> _state.value = ForgotPasswordState.Success
                is AppResult.Error -> {
                    AppLogger.e("ForgotPasswordVM", "Error: ${result.message}")
                    _state.value = ForgotPasswordState.Error(result.message)
                }
                is AppResult.Loading -> {}
            }
        }
    }
}

sealed class ForgotPasswordState {
    object Idle : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    object Success : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}
