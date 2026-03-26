package com.nexohogar.presentation.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.nexohogar.domain.model.Budget
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clpFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Presupuestos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Filled.Add, contentDescription = "Nuevo presupuesto")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.budgets.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Sin presupuestos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Crea uno para controlar tus gastos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.budgets, key = { it.id }) { budget ->
                        BudgetCard(
                            budget = budget,
                            clpFormat = clpFormat,
                            onDelete = { viewModel.showDeleteConfirm(budget.id) }
                        )
                    }
                }
            }

            if (uiState.error != null) {
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    Text(uiState.error ?: "")
                }
            }
        }
    }

    // ── Create Budget Dialog ─────────────────────────────────────────────────
    if (uiState.showCreateDialog) {
        CreateBudgetDialog(
            categories        = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            amount            = uiState.newAmount,
            isCreating        = uiState.isCreating,
            existingBudgetCategoryIds = uiState.budgets.map { it.categoryId }.toSet(),
            onCategorySelected = viewModel::onCategorySelected,
            onAmountChange    = viewModel::onAmountChange,
            onConfirm         = viewModel::createBudget,
            onDismiss         = viewModel::dismissCreateDialog
        )
    }

    // ── Delete Confirmation ──────────────────────────────────────────────────
    if (uiState.showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = { Text("Eliminar presupuesto") },
            text = { Text("¿Estás seguro de que quieres eliminar este presupuesto?") },
            confirmButton = {
                TextButton(onClick = viewModel::deleteBudget, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Eliminar")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissDeleteConfirm) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun BudgetCard(budget: Budget, clpFormat: NumberFormat, onDelete: () -> Unit) {
    val percentage = budget.percentage
    val isOverBudget = percentage > 100

    // Color-coded alerts: 50% azul, 80% amarillo, 100% rojo, >100% rojo intenso
    val progressColor = when {
        percentage > 100 -> Color(0xFFB71C1C) // rojo intenso
        percentage >= 100 -> Color(0xFFE53935) // rojo
        percentage >= 80 -> Color(0xFFFFA726) // amarillo suave
        percentage >= 50 -> Color(0xFF42A5F5) // azul suave
        else -> Color(0xFF66BB6A) // verde
    }

    val trackColor = progressColor.copy(alpha = 0.15f)

    val percentageText = if (isOverBudget) {
        "-${"%.0f".format(percentage - 100)}%"
    } else {
        "${"%.0f".format(percentage)}%"
    }

    val percentageColor = when {
        percentage > 100 -> Color(0xFFB71C1C)
        percentage >= 100 -> Color(0xFFE53935)
        percentage >= 80 -> Color(0xFFF57F17)
        percentage >= 50 -> Color(0xFF1565C0)
        else -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        budget.categoryName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${clpFormat.format(budget.spentClp)} / ${clpFormat.format(budget.amountClp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = percentageText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = percentageColor
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (percentage.toFloat() / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = progressColor,
                trackColor = trackColor
            )

            if (isOverBudget) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "⚠️ Presupuesto excedido por ${clpFormat.format(budget.spentClp - budget.amountClp)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB71C1C)
                )
            } else if (percentage >= 80) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "⚠️ Cerca del límite",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF57F17)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBudgetDialog(
    categories: List<com.nexohogar.domain.model.Category>,
    selectedCategoryId: String?,
    amount: String,
    isCreating: Boolean,
    existingBudgetCategoryIds: Set<String>,
    onCategorySelected: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val availableCategories = categories.filter { it.id !in existingBudgetCategoryIds }
    var expanded by remember { mutableStateOf(false) }
    val selectedName = availableCategories.find { it.id == selectedCategoryId }?.name ?: "Seleccionar categoría"

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Nuevo Presupuesto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (availableCategories.isEmpty()) {
                    Text("Todas las categorías ya tienen presupuesto", color = MaterialTheme.colorScheme.error)
                } else {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = selectedName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            availableCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        onCategorySelected(cat.id)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text("Monto máximo (CLP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isCreating && selectedCategoryId != null && (amount.toLongOrNull() ?: 0) > 0
            ) {
                if (isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Crear")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancelar") } }
    )
}
