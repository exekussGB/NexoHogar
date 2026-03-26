package com.nexohogar.presentation.budget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexohogar.domain.model.BudgetItem
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.util.*

// ─── Colores semáforo ────────────────────────────────────────────────────────
private val SemGreen  = Color(0xFF2E7D32)
private val SemYellow = Color(0xFFF57F17)
private val SemRed    = Color(0xFFC62828)

/** Retorna el color semáforo según porcentaje de consumo. */
private fun trafficColor(pct: Double): Color = when {
    pct >= 100.0 -> SemRed
    pct >= 80.0  -> SemYellow
    pct >= 50.0  -> SemGreen
    else         -> Color(0xFF1565C0) // azul tranquilo < 50 %
}

private val MONTH_NAMES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel     : BudgetViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clpFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar
    LaunchedEffect(uiState.snackMessage) {
        uiState.snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Presupuestos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo presupuesto")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> LoadingOverlay()
                uiState.error != null -> ErrorContent(uiState.error!!) { viewModel.load() }
                else -> BudgetContent(
                    uiState    = uiState,
                    clpFormat  = clpFormat,
                    onPrevious = { viewModel.previousMonth() },
                    onNext     = { viewModel.nextMonth() },
                    onEdit     = { viewModel.showEditDialog(it) },
                    onDelete   = { viewModel.showDeleteConfirm(it) }
                )
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    if (uiState.showCreateDialog) {
        CreateBudgetDialog(
            categories          = uiState.categories,
            existingCategoryIds = uiState.items.map { it.categoryId }.toSet(),
            isCreating          = uiState.isCreating,
            onDismiss           = { viewModel.hideCreateDialog() },
            onCreate            = { catId, amount, memberId ->
                viewModel.createBudget(catId, amount.toLong(), memberId)
            }
        )
    }

    uiState.showEditDialog?.let { item ->
        EditBudgetDialog(
            budgetItem = item,
            isUpdating = uiState.isUpdating,
            onDismiss  = { viewModel.hideEditDialog() },
            onUpdate   = { newAmount -> viewModel.updateBudget(item.budgetId, newAmount.toLong()) }
        )
    }

    uiState.showDeleteConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirm() },
            icon  = { Icon(Icons.Default.Delete, contentDescription = null, tint = SemRed) },
            title = { Text("Eliminar presupuesto") },
            text  = { Text("¿Eliminar el presupuesto de \"${item.categoryName}\"?\nEsta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteBudget(item.budgetId) },
                    enabled = !uiState.isDeleting
                ) {
                    Text(if (uiState.isDeleting) "Eliminando…" else "Eliminar", color = SemRed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.hideDeleteConfirm() },
                    enabled = !uiState.isDeleting
                ) { Text("Cancelar") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BudgetContent(
    uiState   : BudgetUiState,
    clpFormat : NumberFormat,
    onPrevious: () -> Unit,
    onNext    : () -> Unit,
    onEdit    : (BudgetItem) -> Unit,
    onDelete  : (BudgetItem) -> Unit
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Selector de mes ─────────────────────────────────────────────────
        item {
            MonthSelector(
                year       = uiState.year,
                month      = uiState.month,
                onPrevious = onPrevious,
                onNext     = onNext
            )
        }

        // ── Resumen total ───────────────────────────────────────────────────
        if (uiState.items.isNotEmpty()) {
            item { BudgetSummaryCard(uiState.items, clpFormat) }
        }

        // ── Sin presupuestos ────────────────────────────────────────────────
        if (uiState.items.isEmpty()) {
            item { EmptyState() }
        } else {
            // ── Ítems de presupuesto ────────────────────────────────────────
            items(uiState.items) { item ->
                BudgetItemCard(
                    item      = item,
                    clpFormat = clpFormat,
                    onEdit    = { onEdit(item) },
                    onDelete  = { onDelete(item) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Month Selector
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MonthSelector(
    year      : Int,
    month     : Int,
    onPrevious: () -> Unit,
    onNext    : () -> Unit
) {
    val monthName = if (month in 1..12) MONTH_NAMES[month - 1] else "?"
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
        }
        Text(
            text       = "$monthName $year",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Summary Card (with traffic light)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BudgetSummaryCard(
    items    : List<BudgetItem>,
    clpFormat: NumberFormat
) {
    val totalBudgeted  = items.sumOf { it.budgetedAmount }
    val totalConsumed  = items.sumOf { it.consumedAmount }
    val totalRemaining = totalBudgeted - totalConsumed
    val globalPct      = if (totalBudgeted > 0) (totalConsumed.toDouble() / totalBudgeted.toDouble()) * 100.0 else 0.0
    val progressColor  = trafficColor(globalPct)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Resumen del mes",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
                // Percentage badge
                Surface(
                    color = progressColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text     = "${String.format("%.1f", globalPct)}%",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress   = { (globalPct / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier   = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color      = progressColor,
                trackColor = progressColor.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Gastado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        clpFormat.format(totalConsumed),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = progressColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Presupuesto",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        clpFormat.format(totalBudgeted),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (totalRemaining >= 0) {
                Text(
                    text  = "Disponible: ${clpFormat.format(totalRemaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            } else {
                Text(
                    text     = "⚠ Excedido en: ${clpFormat.format(-totalRemaining)}",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = SemRed,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Budget Item Card (with traffic light + edit/delete)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BudgetItemCard(
    item     : BudgetItem,
    clpFormat: NumberFormat,
    onEdit   : () -> Unit,
    onDelete : () -> Unit
) {
    val pct      = item.consumptionPct
    val barColor = trafficColor(pct)
    val isOver   = item.remainingAmount < 0

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick     = onEdit,
                onLongClick = onDelete
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        // Red tinted card when overbudget
        colors = if (isOver) CardDefaults.cardColors(
            containerColor = SemRed.copy(alpha = 0.08f)
        ) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row: name + percentage badge + actions
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = item.categoryName,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f)
                )
                // Traffic light badge
                Surface(
                    color = barColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text     = "${String.format("%.1f", pct)}%",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                // Edit button
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        modifier = Modifier.size(18.dp),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Delete button
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        modifier = Modifier.size(18.dp),
                        tint     = SemRed.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            LinearProgressIndicator(
                progress   = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier   = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color      = barColor,
                trackColor = barColor.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Amounts row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = "Gastado: ${clpFormat.format(item.consumedAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = "de ${clpFormat.format(item.budgetedAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Alert badges
            if (pct >= 50.0) {
                Spacer(modifier = Modifier.height(6.dp))
                AlertBadge(pct = pct, isOver = isOver, clpFormat = clpFormat, overAmount = if (isOver) -item.remainingAmount else 0)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Alert Badge (traffic-light style)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AlertBadge(pct: Double, isOver: Boolean, clpFormat: NumberFormat, overAmount: Long) {
    val (icon, text, color) = when {
        isOver       -> Triple("🔴", "Excedido en ${clpFormat.format(overAmount)} (${String.format("%.1f", pct)}%)", SemRed)
        pct >= 100.0 -> Triple("🔴", "Presupuesto agotado", SemRed)
        pct >= 80.0  -> Triple("🟡", "Atención: ${String.format("%.1f", pct)}% consumido", SemYellow)
        pct >= 50.0  -> Triple("🟢", "${String.format("%.1f", pct)}% consumido", SemGreen)
        else         -> return
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text       = text,
                style      = MaterialTheme.typography.labelSmall,
                color      = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyState() {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier           = Modifier.size(56.dp),
                tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text      = "Sin presupuestos para este mes",
                style     = MaterialTheme.typography.bodyLarge,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text      = "Usa el botón + para agregar uno.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Error content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}
