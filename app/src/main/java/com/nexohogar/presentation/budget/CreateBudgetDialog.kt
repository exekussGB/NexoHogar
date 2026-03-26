package com.nexohogar.presentation.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CreateBudgetDialog(
    existingCategoryNames: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (categoryName: String, amount: Long) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Presupuesto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = {
                        categoryName = it
                        nameError = null
                    },
                    label = { Text("Categoría") },
                    placeholder = { Text("Ej: Alimentación, Transporte...") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { c -> c.isDigit() }
                        amountError = null
                    },
                    label = { Text("Monto mensual (CLP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val name = categoryName.trim()
                val amount = amountText.toLongOrNull()
                var valid = true
                if (name.isBlank()) {
                    nameError = "Ingresa un nombre"
                    valid = false
                } else if (existingCategoryNames.contains(name.lowercase())) {
                    nameError = "Ya existe un presupuesto para esta categoría"
                    valid = false
                }
                if (amount == null || amount <= 0) {
                    amountError = "Ingresa un monto válido"
                    valid = false
                }
                if (valid) onConfirm(name, amount!!)
            }) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun EditBudgetDialog(
    currentCategoryName: String,
    currentAmount: Long,
    onDismiss: () -> Unit,
    onConfirm: (newAmount: Long) -> Unit
) {
    var amountText by remember { mutableStateOf(currentAmount.toString()) }
    var amountError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Presupuesto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = currentCategoryName,
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { c -> c.isDigit() }
                        amountError = null
                    },
                    label = { Text("Monto mensual (CLP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toLongOrNull()
                if (amount == null || amount <= 0) {
                    amountError = "Ingresa un monto válido"
                } else {
                    onConfirm(amount)
                }
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
