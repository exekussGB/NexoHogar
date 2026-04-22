package com.nexohogar.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.util.AppLogger
import com.nexohogar.core.util.InputSanitizer
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
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank() || pass.isBlank()) {
            _loginState.value = LoginState.Error("Campos obligatorios")
            return
        }

        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            when (val result = repository.login(trimmedEmail, pass)) {
                is AppResult.Success -> {
                    sessionManager.saveSession(result.data)
                    _loginState.value = LoginState.Success
                }
                is AppResult.Error -> {
                    _loginState.value = LoginState.Error(result.message)
                }
                else -> {}
            }
        }
    }

    fun continueAsGuest() {
        val guestSession = com.nexohogar.domain.model.UserSession(
            accessToken = "guest_token",
            refreshToken = "guest_refresh",
            userId = "guest_${java.util.UUID.randomUUID()}",
            email = "invitado@nexohogar.local",
            expiresAt = Long.MAX_VALUE,
            isGuest = true
        )
        sessionManager.saveSession(guestSession)
        sessionManager.saveSelectedHouseholdId("guest_household_${java.util.UUID.randomUUID()}")
        _loginState.value = LoginState.Success
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
