package com.nexohogar.presentation.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerifyOtpState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val accessToken: String? = null,
    val error: String? = null
)

/**
 * SEC-05: El access_token obtenido se expone via state para que la pantalla
 * lo pase como argumento de navegación, en lugar de usar el singleton eliminado.
 */
class VerifyOtpViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VerifyOtpState())
    val state: StateFlow<VerifyOtpState> = _state.asStateFlow()

    fun verifyOtp(email: String, code: String) {
        if (code.length != 8) {
            _state.update { it.copy(error = "El código debe tener 8 dígitos") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.verifyOtp(email, code)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            accessToken = result.data
                        )
                    }
                }
                is AppResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        fun Factory(authRepository: AuthRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return VerifyOtpViewModel(authRepository) as T
                }
            }
        }
    }
}
