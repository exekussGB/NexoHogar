package com.nexohogar.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.util.AppLogger
import com.nexohogar.data.local.SessionManager
import com.nexohogar.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de Login.
 * SEC-04: Logs condicionados a DEBUG via AppLogger.
 * COH-05: Migrado de MutableLiveData a MutableStateFlow.
 */
class LoginViewModel(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _loginState.value = LoginState.Error("Campos obligatorios")
            return
        }

        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            AppLogger.d("LoginViewModel", "Iniciando intento de login")

            when (val result = repository.login(email, pass)) {
                is AppResult.Success -> {
                    AppLogger.d("LoginViewModel", "Login exitoso, guardando sesión")
                    sessionManager.saveSession(result.data)
                    _loginState.value = LoginState.Success
                }
                is AppResult.Error -> {
                    AppLogger.e("LoginViewModel", "Error en login: ${result.message}")
                    _loginState.value = LoginState.Error(result.message)
                }
                is AppResult.Loading -> { }
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
