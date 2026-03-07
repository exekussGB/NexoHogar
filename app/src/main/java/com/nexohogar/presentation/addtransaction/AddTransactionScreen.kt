package com.nexohogar.presentation.addtransaction

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexohogar.presentation.components.LoadingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    onNavigateBack: () -> Unit
) {
    var accountId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is AddTransactionUiState.Success) {
            Toast.makeText(context, "Movimiento creado con éxito", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        } else if (uiState is AddTransactionUiState.Error) {
            Toast.makeText(context, (uiState as AddTransactionUiState.Error).message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Movimiento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = accountId,
                    onValueChange = { accountId = it },
                    label = { Text("ID de Cuenta") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                        viewModel.createTransaction(accountId, amountValue, description)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState !is AddTransactionUiState.Loading
                ) {
                    Text("Crear movimiento")
                }
            }

            if (uiState is AddTransactionUiState.Loading) {
                LoadingOverlay()
            }
        }
    }
}
