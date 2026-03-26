package com.nexohogar.presentation.budget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.domain.model.BudgetItem
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.util.*

// Semáforo colors
private val SemaforoBlue   = Color(0xFF42A5F5)
private val SemaforoGreen  = Color(0xFF66BB6A)
private val SemaforoYellow = Color(0xFFFFA726)
private val SemaforoRed    = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clpFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetItem?>(null) }
    var deletingBudget by remember { mutableStateOf<BudgetItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Presupuestos", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Control mensual",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo presupuesto")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> LoadingOverlay()

                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error ?: "Error desconocido",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = viewModel::load) { Text("Reintentar") }
                        }
                    }
                }

                uiState.budgets.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Sin presupuestos",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Toca + para crear uno",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        // Summary card
                        item {
                            val totalBudgeted = uiState.budgets.sumOf { it.budgetedAmount }
                            val totalSpent = uiState.budgets.sumOf { it.spentAmount }
                            val overallPct = if (totalBudgeted > 0) (totalSpent * 100.0 / totalBudgeted) else 0.0
                            val summaryColor = getSemaforoColor(overallPct)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = summaryColor.copy(alpha = 0.12f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Resumen general",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                            Text(
                                                text = "${clpFormat.format(totalSpent.toLong())} / ${clpFormat.format(totalBudgeted.toLong())}",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = summaryColor
                                            )
                                        }
                                        SemaforoBadge(overallPct)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { (overallPct / 100.0).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = summaryColor,
                                        trackColor = summaryColor.copy(alpha = 0.2f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        // Budget items
                        items(uiState.budgets, key = { it.budgetId }) { budget ->
                            val pct = if (budget.budgetedAmount > 0)
                                (budget.spentAmount * 100.0 / budget.budgetedAmount)
                            else 0.0
                            val remaining = budget.budgetedAmount - budget.spentAmount
                            val color = getSemaforoColor(pct)
                            val isExceeded = remaining < 0

                            val bgColor by animateColorAsState(
                                targetValue = if (isExceeded) SemaforoRed.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surface,
                                label = "bg"
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { editingBudget = budget },
                                        onLongClick = { deletingBudget = budget }
                                    ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                colors = CardDefaults.cardColors(containerColor = bgColor)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = budget.categoryName,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "${clpFormat.format(budget.spentAmount)} / ${clpFormat.format(budget.budgetedAmount)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (isExceeded) {
                                                Text(
                                                    text = "Excedido: ${clpFormat.format(-remaining)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SemaforoRed
                                                )
                                            } else {
                                                Text(
                                                    text = "Disponible: ${clpFormat.format(remaining)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = color
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            SemaforoBadge(pct)
                                            Spacer(Modifier.height(4.dp))
                                            IconButton(
                                                onClick = { deletingBudget = budget },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Eliminar",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = color,
                                        trackColor = color.copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dialogs
            if (showCreateDialog) {
                val existingNames = uiState.budgets.map { it.categoryName.lowercase() }.toSet()
                CreateBudgetDialog(
                    existingCategoryNames = existingNames,
                    onDismiss = { showCreateDialog = false },
                    onConfirm = { name, amount ->
                        viewModel.createBudget(name, amount)
                        showCreateDialog = false
                    }
                )
            }

            editingBudget?.let { budget ->
                EditBudgetDialog(
                    currentCategoryName = budget.categoryName,
                    currentAmount = budget.budgetedAmount,
                    onDismiss = { editingBudget = null },
                    onConfirm = { newAmount ->
                        viewModel.updateBudget(budget.budgetId, newAmount)
                        editingBudget = null
                    }
                )
            }

            deletingBudget?.let { budget ->
                AlertDialog(
                    onDismissRequest = { deletingBudget = null },
                    title = { Text("Eliminar presupuesto") },
                    text = { Text("¿Eliminar el presupuesto de \"${budget.categoryName}\"?") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteBudget(budget.budgetId)
                            deletingBudget = null
                        }) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deletingBudget = null }) { Text("Cancelar") }
                    }
                )
            }
        }
    }
}

@Composable
private fun SemaforoBadge(percentage: Double) {
    val color = getSemaforoColor(percentage)
    val label = when {
        percentage < 50  -> "${String.format("%.0f", percentage)}%"
        percentage < 80  -> "${String.format("%.0f", percentage)}% consumido"
        percentage < 100 -> "Atención: ${String.format("%.0f", percentage)}%"
        else             -> "Agotado: ${String.format("%.0f", percentage)}%"
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

private fun getSemaforoColor(percentage: Double): Color = when {
    percentage < 50  -> SemaforoBlue
    percentage < 80  -> SemaforoGreen
    percentage < 100 -> SemaforoYellow
    else             -> SemaforoRed
}
