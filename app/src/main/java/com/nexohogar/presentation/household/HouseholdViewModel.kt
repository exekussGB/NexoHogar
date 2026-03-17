package com.nexohogar.presentation.household

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.domain.model.Household
import com.nexohogar.domain.repository.HouseholdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de Hogares.
 * Solo depende de interfaces de dominio y modelos de dominio.
 */
class HouseholdViewModel(
    private val repository: HouseholdRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _householdState = MutableStateFlow<HouseholdState>(HouseholdState.Loading)
    val householdState: StateFlow<HouseholdState> = _householdState

    init {
        fetchHouseholds()
    }

    fun fetchHouseholds() {
        Log.d("HF_DEBUG", "fetchHouseholds() llamado")
        Log.d("HF_DEBUG", "token = ${sessionManager.fetchAuthToken()}")

        if (sessionManager.fetchAuthToken() == null) {
            _householdState.value = HouseholdState.Error("No hay sesión activa")
            return
        }

        _householdState.value = HouseholdState.Loading

        viewModelScope.launch {
            Log.d("HF_DEBUG", "Llamando repository.getHouseholds()")
            
            when (val result = repository.getHouseholds()) {
                is AppResult.Success -> {
                    Log.d("HF_DEBUG", "Resultado repository: $result")
                    _householdState.value = HouseholdState.Success(result.data)
                }
                is AppResult.Error -> {
                    Log.d("HF_DEBUG", "Resultado repository: $result")
                    _householdState.value = HouseholdState.Error(result.message)
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun selectHousehold(id: String) {
        repository.selectHousehold(id)
    }
}

sealed class HouseholdState {
    object Loading : HouseholdState()
    data class Success(val households: List<Household>) : HouseholdState()
    data class Error(val message: String) : HouseholdState()
}
