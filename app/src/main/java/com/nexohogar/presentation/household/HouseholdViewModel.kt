package com.nexohogar.presentation.household

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexohogar.core.result.AppResult
import com.nexohogar.data.local.SessionManager
import com.nexohogar.domain.model.Household
import com.nexohogar.domain.repository.HouseholdRepository
import kotlinx.coroutines.launch

/**
 * ViewModel de Hogares.
 * Solo depende de interfaces de dominio y modelos de dominio.
 */
class HouseholdViewModel(
    private val repository: HouseholdRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _householdState = MutableLiveData<HouseholdState>(HouseholdState.Loading)
    val householdState: LiveData<HouseholdState> = _householdState

    fun fetchHouseholds() {
        if (sessionManager.fetchAuthToken() == null) {
            _householdState.value = HouseholdState.Error("No hay sesión activa")
            return
        }

        _householdState.value = HouseholdState.Loading

        viewModelScope.launch {
            Log.d("HouseholdViewModel", "Iniciando fetch de households...")
            
            when (val result = repository.getHouseholds()) {
                is AppResult.Success -> {
                    Log.d("HouseholdViewModel", "Fetch exitoso: ${result.data.size} hogares")
                    _householdState.value = HouseholdState.Success(result.data)
                }
                is AppResult.Error -> {
                    Log.e("HouseholdViewModel", "Error: ${result.message}")
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
