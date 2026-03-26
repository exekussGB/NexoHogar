package com.nexohogar.presentation.budget

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
                    containerColor  = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
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
                    onNext     = { viewModel.nextMonth() }
                )
            }
        }
    }
}

@Composable
private fun BudgetContent(
    uiState  : BudgetUiState,
    clpFormat: NumberFormat,
    onPrevious: () -> Unit,
    onNext    : () -> Unit
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Selector de mes ────────────────────────────────────────────────
        item {
            MonthSelector(
                year      = uiState.year,
                month     = uiState.month,
                onPrevious = onPrevious,
                onNext     = onNext
            )
        }

        // ── Resumen total ──────────────────────────────────────────────────
        if (uiState.items.isNotEmpty()) {
            item {
                BudgetSummaryCard(uiState.items, clpFormat)
            }
        }

        // ── Sin presupuestos ───────────────────────────────────────────────
        if (uiState.items.isEmpty()) {
            item {
                Box(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment    = Alignment.Center
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
                            text  = "Sin presupuestos para este mes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text  = "Agrega presupuestos por categoría\ndesde el panel web.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            // ── Ítems de presupuesto ───────────────────────────────────────
            items(uiState.items) { item ->
                BudgetItemCard(item, clpFormat)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Selector de mes
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MonthSelector(
    year     : Int,
    month    : Int,
    onPrevious: () -> Unit,
    onNext   : () -> Unit
) {
    val monthName = if (month in 1..12) MONTH_NAMES[month - 1] else "?"
    Row(
        modifier                = Modifier.fillMaxWidth(),
        horizontalArrangement   = Arrangement.SpaceBetween,
        verticalAlignment       = Alignment.CenterVertically
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
// Resumen total
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BudgetSummaryCard(
    items    : List<BudgetItem>,
    clpFormat: NumberFormat
) {
    val totalBudgeted = items.sumOf { it.budgetedAmount }
    val totalConsumed = items.sumOf { it.consumedAmount }
    val totalRemaining = totalBudgeted - totalConsumed
    val globalPct = if (totalBudgeted > 0) (totalConsumed.toFloat() / totalBudgeted.toFloat()) else 0f

    val progressColor = when {
        globalPct >= 1f  -> Color(0xFFC62828)
        globalPct >= 0.8f -> Color(0xFFF57F17)
        else             -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Resumen del mes",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress             = { globalPct.coerceIn(0f, 1f) },
                modifier             = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color                = progressColor,
                trackColor           = progressColor.copy(alpha = 0.2f)
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
            if (totalRemaining >= 0) {
                Text(
                    text  = "Disponible: ${clpFormat.format(totalRemaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text  = "Excedido en: ${clpFormat.format(-totalRemaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC62828),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ítem individual de presupuesto
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BudgetItemCard(
    item     : BudgetItem,
    clpFormat: NumberFormat
) {
    val pct  = (item.consumptionPct / 100.0).toFloat().coerceIn(0f, 1f)
    val barColor = when {
        pct >= 1f   -> Color(0xFFC62828)
        pct >= 0.8f -> Color(0xFFF57F17)
        else        -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = item.categoryName,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text  = "${item.consumptionPct.toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = barColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress   = { pct },
                modifier   = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color      = barColor,
                trackColor = barColor.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(6.dp))
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
            if (item.remainingAmount < 0) {
                Text(
                    text  = "⚠ Excedido en ${clpFormat.format(-item.remainingAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC62828),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier              = Modifier.fillMaxSize(),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}
