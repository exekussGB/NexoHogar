package com.nexohogar.presentation.forgotpassword

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.repository.AuthRepository
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableLiveData<ForgotPasswordState>(ForgotPasswordState.Idle)
    val state: LiveData<ForgotPasswordState> = _state

    fun sendRecoveryEmail(email: String) {
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _state.value = ForgotPasswordState.Error("Ingresa un correo válido")
            return
        }
        _state.value = ForgotPasswordState.Loading
        viewModelScope.launch {
            when (val result = repository.forgotPassword(email.trim())) {
                is AppResult.Success -> _state.value = ForgotPasswordState.Success
                is AppResult.Error -> _state.value = ForgotPasswordState.Error(result.message)
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