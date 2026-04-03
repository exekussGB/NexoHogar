package com.nexohogar.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.model.MonthlyBalance
import com.nexohogar.domain.model.Transaction
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.ui.platform.testTag
import com.nexohogar.core.tutorial.TutorialManager
import com.nexohogar.core.tutorial.TutorialModule
import com.nexohogar.presentation.tutorial.TutorialOverlay

// ─────────────────────────────────────────────────────────────────────────────
// Formateador de fecha para Chile (America/Santiago)
// ─────────────────────────────────────────────────────────────────────────────
private val chileZone = ZoneId.of("America/Santiago")
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val dtFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

internal fun formatChileDate(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val zoned = instant.atZone(chileZone)
        if (isoString.length > 10) zoned.format(dtFormatter) else zoned.format(dateFormatter)
    } catch (_: Exception) {
        try {
            // Fallback: tomar solo los primeros 10 chars "yyyy-MM-dd" y reformatear
            val parts = isoString.take(10).split("-")
            if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}"
            else isoString.take(10)
        } catch (_: Exception) {
            isoString.take(10)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DashboardScreen — toggle Fondo Común / Mis Cuentas
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    tutorialManager: TutorialManager,
    onTransactionClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    onNavigateToCategoryExp: () -> Unit,
    onNavigateToPersonal: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clpFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }

    var showTutorial by remember {
        mutableStateOf(!tutorialManager.isTutorialCompleted(TutorialModule.DASHBOARD))
    }
    // Refresca al volver a la pantalla (ej. tras agregar un movimiento)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDashboard()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) LoadingOverlay()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Título + botón Mis Cuentas ─────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Resumen Financiero",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (onNavigateToPersonal != null) {
                        OutlinedButton(
                            onClick = onNavigateToPersonal,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mis Cuentas", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            // ── Balance ────────────────────────────────────────────────────
            item {
                Box(modifier = Modifier.testTag("dashboard_balance")) {
                    uiState.summary?.let { BalanceCard(summary = it, format = clpFormat) }
                }
            }

            // ── Gráfico de tendencia ───────────────────────────────────────
            item {
                Box(modifier = Modifier.testTag("dashboard_chart")) {
                    MonthlyChart(monthlyData = uiState.monthlyBalance)
                }
            }

            // ── Gastos por categoría (botón) ───────────────────────────────
            item {
                CategoryExpensesButton(onClick = onNavigateToCategoryExp)
            }

            // ── Últimos movimientos ────────────────────────────────────────
            item {
                Box(modifier = Modifier.testTag("dashboard_recent")) {
                    RecentTransactionsList(
                        transactions = uiState.recentTransactions,
                        format = clpFormat,
                        onTransactionClick = onTransactionClick,
                        onSeeAllClick = onSeeAllClick
                    )
                }
            }
        }
        // ── Tutorial overlay ────────────────────────────────────────────────────
        if (showTutorial) {
            TutorialOverlay(
                module = TutorialModule.DASHBOARD,
                onComplete = {
                    tutorialManager.markTutorialCompleted(TutorialModule.DASHBOARD)
                    showTutorial = false
                },
                onSkip = {
                    tutorialManager.markTutorialCompleted(TutorialModule.DASHBOARD)
                    showTutorial = false
                }
            )
        }
    }

}

// ─────────────────────────────────────────────────────────────────────────────
// Botón acceso rápido Gastos por Categoría
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CategoryExpensesButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Gastos por Categoría",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Ver desglose del mes actual",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BalanceCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BalanceCard(summary: DashboardSummary, format: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Saldo Total", style = MaterialTheme.typography.labelMedium)
            Text(
                text = format.format(summary.totalBalance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryInfo("Ingresos", summary.totalIncome, Color(0xFF4CAF50), format)
                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                SummaryInfo("Gastos", summary.totalExpense, Color(0xFFF44336), format)
            }
        }
    }
}

@Composable
private fun SummaryInfo(label: String, amount: Double, color: Color, format: NumberFormat) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            text = format.format(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MonthlyChart — CORREGIDO: barras con altura mínima visible
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MonthlyChart(monthlyData: List<MonthlyBalance>) {
    val monthNames = listOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")
    val clpFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tendencia Mensual",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (selectedIndex != null) {
                    TextButton(
                        onClick = { selectedIndex = null },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("✕ cerrar", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val selIdx = selectedIndex
            if (selIdx != null) {
                val item = monthlyData.getOrNull(selIdx)
                if (item != null) {
                    val monthName = monthNames.getOrElse(item.monthNum - 1) { "?" }
                    val netColor = if (item.net >= 0) Color(0xFF43A047) else Color(0xFFE53935)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.inverseSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "$monthName ${item.yearNum}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.inverseOnSurface
                                )
                                Text(
                                    text = "Ingresos: ${clpFormat.format(item.income)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Gastos: ${clpFormat.format(item.expense)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = clpFormat.format(item.net),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = netColor
                            )
                        }
                    }
                }
            }

            val hasData = monthlyData.any { it.net != 0L }
            if (!hasData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Text(
                            "Sin movimientos aún",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                return@Column
            }

            val positiveColor = Color(0xFF4CAF50)
            val negativeColor = Color(0xFFF44336)
            val selectedColor = Color(0xFF1565C0)
            val labelColor = Color(0xFF757575)
            val maxAbs = monthlyData.maxOf { kotlin.math.abs(it.net) }.takeIf { it > 0L } ?: 1L

            // ── FIX: Altura mínima para barras con datos ──────────────────
            // Cuando un mes tiene movimientos pero su valor neto es muy pequeño
            // en comparación con el máximo, la barra era invisible (ej. $1.410
            // vs $716.361 → 0.2% de altura). Ahora garantizamos una altura
            // mínima de 8px para que siempre sea visible y clickeable.
            val MIN_BAR_HEIGHT = 8f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .pointerInput(monthlyData) {
                        detectTapGestures { offset ->
                            val count = monthlyData.size
                            if (count == 0) return@detectTapGestures
                            val slotW = size.width / count.toFloat()
                            val tapped = (offset.x / slotW).toInt().coerceIn(0, count - 1)
                            selectedIndex = if (selectedIndex == tapped) null else tapped
                        }
                    }
            ) {
                val count = monthlyData.size
                val slotWidth = size.width / count
                val barWidth = slotWidth * 0.5f
                val barAreaH = size.height * 0.72f
                val baselineY = size.height * 0.72f
                val labelY = size.height - 4f

                drawLine(
                    color = Color(0xFFBDBDBD),
                    start = Offset(0f, baselineY),
                    end = Offset(size.width, baselineY),
                    strokeWidth = 1.5f
                )

                monthlyData.forEachIndexed { i, item ->
                    val isSelected = (selectedIndex == i)
                    val centerX = slotWidth * i + slotWidth / 2f
                    val barLeft = centerX - barWidth / 2f
                    val fraction = item.net.toFloat() / maxAbs.toFloat()
                    val computedBarH = kotlin.math.abs(fraction) * barAreaH * 0.85f

                    // ── FIX: Si el mes tiene datos (net != 0), asegurar
                    // una altura mínima visible ─────────────────────────
                    val barH = if (item.net != 0L) {
                        maxOf(computedBarH, MIN_BAR_HEIGHT)
                    } else {
                        computedBarH
                    }

                    val topY = if (item.net >= 0) baselineY - barH else baselineY

                    val barColor = when {
                        isSelected -> selectedColor
                        item.net >= 0 -> positiveColor
                        else -> negativeColor
                    }
                    val alpha = when {
                        isSelected -> 1f
                        selectedIndex != null -> 0.35f
                        i == count - 1 -> 1f
                        else -> 0.75f
                    }

                    drawRect(
                        color = barColor,
                        topLeft = Offset(barLeft, topY),
                        size = Size(barWidth, barH),
                        alpha = alpha
                    )

                    // ── Indicador de valor pequeño: un punto encima de la barra
                    // cuando la barra real sería < 3px pero tiene datos ──────
                    if (item.net != 0L && computedBarH < 3f) {
                        val dotY = if (item.net >= 0) topY - 6f else topY + barH + 6f
                        drawCircle(
                            color = barColor,
                            radius = 3f,
                            center = Offset(centerX, dotY),
                            alpha = alpha
                        )
                    }

                    val label = monthNames[(item.monthNum - 1).coerceIn(0, 11)]
                    drawContext.canvas.nativeCanvas.drawText(
                        label, centerX, labelY,
                        android.graphics.Paint().apply {
                            color = if (isSelected) android.graphics.Color.rgb(21, 101, 192)
                            else labelColor.toArgb()
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RecentTransactionsList
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RecentTransactionsList(
    transactions : List<Transaction>,
    format : NumberFormat,
    onTransactionClick: (String) -> Unit,
    onSeeAllClick : () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Últimos Movimientos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onSeeAllClick) { Text("Ver todos") }
        }

        if (transactions.isEmpty()) {
            Text(
                text = "No hay movimientos recientes.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            transactions.forEach { TransactionRowItem(it, format, onTransactionClick) }
        }
    }
}

@Composable
private fun TransactionRowItem(
    transaction: Transaction,
    format : NumberFormat,
    onClick : (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(transaction.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val (icon, iconColor) = when (transaction.type) {
                "income" -> Icons.Default.ArrowDownward to Color(0xFF4CAF50)
                "expense" -> Icons.Default.ArrowUpward to Color(0xFFF44336)
                "transfer" -> Icons.Default.SwapHoriz to Color(0xFF2196F3)
                else -> Icons.Default.AttachMoney to Color(0xFF9E9E9E)
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description ?: "Sin descripción",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                // ── Fecha formateada para Chile ────────────────────────────
                Text(
                    text = formatChileDate(transaction.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val amountColor = when (transaction.type) {
                "income" -> Color(0xFF4CAF50)
                "expense" -> Color(0xFFF44336)
                "transfer" -> Color(0xFF2196F3)
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = format.format(transaction.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
