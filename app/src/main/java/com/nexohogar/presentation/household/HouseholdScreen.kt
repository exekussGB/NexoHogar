package com.nexohogar.presentation.household

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexohogar.domain.model.Household
import com.nexohogar.presentation.components.LoadingOverlay

/**
 * UI de la pantalla de Selección de Household usando Jetpack Compose.
 * Utiliza modelos de Dominio para asegurar el desacoplamiento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdScreen(
    viewModel: HouseholdViewModel,
    onHouseholdSelected: () -> Unit
) {
    val state by viewModel.householdState.collectAsStateWithLifecycle()
    
    // Log para diagnosticar el estado recibido en la UI
    Log.d("HF_UI", "State actual: $state")
    if (state is HouseholdState.Success) {
        Log.d("HF_UI", "Households recibidos: ${(state as HouseholdState.Success).households}")
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Selecciona tu Hogar") })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                is HouseholdState.Loading -> {
                    LoadingOverlay()
                }
                is HouseholdState.Success -> {
                    val households = (state as HouseholdState.Success).households
                    if (households.isEmpty()) {
                        EmptyState()
                    } else {
                        HouseholdList(households) { household ->
                            viewModel.selectHousehold(household.id)
                            onHouseholdSelected()
                        }
                    }
                }
                is HouseholdState.Error -> {
                    ErrorState((state as HouseholdState.Error).message) {
                        viewModel.fetchHouseholds()
                    }
                }
            }
        }
    }
}

@Composable
fun HouseholdList(
    households: List<Household>,
    onItemClick: (Household) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(households) { household ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { onItemClick(household) },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = household.name,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = household.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No se encontraron hogares asociados.")
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}
