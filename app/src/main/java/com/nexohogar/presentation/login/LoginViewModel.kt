package com.nexohogar.presentation.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.domain.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * ViewModel de Login.
 * Solo depende de la interfaz AuthRepository (Domain) y AppResult.
 * No conoce DTOs ni implementaciones concretas de red.
 */
class LoginViewModel(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _loginState.value = LoginState.Error("Campos obligatorios")
            return
        }

        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            Log.d("LoginViewModel", "Iniciando intento de login para: $email")
            
            // Llamada al repositorio (Domain Interface)
            when (val result = repository.login(email, pass)) {
                is AppResult.Success -> {
                    Log.d("LoginViewModel", "Login exitoso, guardando sesión")
                    sessionManager.saveSession(result.data)
                    _loginState.value = LoginState.Success
                }
                is AppResult.Error -> {
                    Log.e("LoginViewModel", "Error en login: ${result.message}")
                    _loginState.value = LoginState.Error(result.message)
                }
                is AppResult.Loading -> {
                    // Estado ya manejado manualmente al inicio
                }
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
