package com.nexohogar.presentation.budget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexohogar.domain.model.BudgetConsumption
import com.nexohogar.domain.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBudgetDialog(
    categories: List<Category>,
    existingCategoryIds: Set<String>,
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (categoryId: String, amount: Double, memberId: String?) -> Unit
) {
    val availableCategories = categories.filter { it.id !in existingCategoryIds }

    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var amountText by remember { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val isValid = selectedCategory != null &&
            amountText.isNotBlank() &&
            (amountText.replace(".", "").toDoubleOrNull() ?: 0.0) > 0

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Nuevo presupuesto") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        if (availableCategories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Todas las categorías ya tienen presupuesto") },
                                onClick = { categoryDropdownExpanded = false },
                                enabled = false
                            )
                        } else {
                            availableCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategory = category
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        // Allow only digits and dots for Chilean formatting
                        val cleaned = newValue.filter { it.isDigit() }
                        amountText = cleaned
                    },
                    label = { Text("Monto (CLP)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.replace(".", "").toDoubleOrNull() ?: return@TextButton
                    selectedCategory?.let { cat ->
                        onCreate(cat.id, amount, null)
                    }
                },
                enabled = isValid && !isCreating
            ) {
                Text(if (isCreating) "Creando..." else "Crear")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EditBudgetDialog(
    budget: BudgetConsumption,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (newAmount: Double) -> Unit
) {
    var amountText by remember {
        mutableStateOf(budget.budgetedAmount.toLong().toString())
    }

    val isValid = amountText.isNotBlank() &&
            (amountText.replace(".", "").toDoubleOrNull() ?: 0.0) > 0

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = { Text("Editar presupuesto") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Category (read-only)
                OutlinedTextField(
                    value = budget.categoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        val cleaned = newValue.filter { it.isDigit() }
                        amountText = cleaned
                    },
                    label = { Text("Monto (CLP)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.replace(".", "").toDoubleOrNull() ?: return@TextButton
                    onUpdate(amount)
                },
                enabled = isValid && !isUpdating
            ) {
                Text(if (isUpdating) "Guardando..." else "Guardar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUpdating
            ) {
                Text("Cancelar")
            }
        }
    )
}