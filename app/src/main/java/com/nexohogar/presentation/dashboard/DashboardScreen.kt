package com.nexohogar.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.model.MonthlyBalance
import com.nexohogar.domain.model.Transaction
import com.nexohogar.domain.model.RecurringBill
import com.nexohogar.domain.model.RecurringBillStatus
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
private val chileZone    = ZoneId.of("America/Santiago")
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val dtFormatter   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

internal fun formatChileDate(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val zoned   = instant.atZone(chileZone)
        if (isoString.length > 10) zoned.format(dtFormatter) else zoned.format(dateFormatter)
    } catch (_: Exception) {
        try {
            val parts = isoString.take(10).split("-")
            if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}"
            else isoString.take(10)
        } catch (_: Exception) {
            isoString.take(10)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DashboardScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    tutorialManager: TutorialManager,
    onTransactionClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    onNavigateToCategoryExp: () -> Unit,
    onNavigateToPersonal: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onAddMovement: () -> Unit = {},
    onAddExpense: () -> Unit = {},
    onAddIncome: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState  by viewModel.uiState.collectAsState()
    val clpFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }

    var showTutorial by remember {
        mutableStateOf(!tutorialManager.isTutorialCompleted(TutorialModule.DASHBOARD))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDashboard()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Resumen Financiero") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
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

                // ── Balance ────────────────────────────────────────────────────
                item {
                    Box(modifier = Modifier.testTag("dashboard_balance")) {
                        val summary         = uiState.summary
                        val computedBalance = uiState.computedTotalBalance
                        if (summary != null) {
                            BalanceCardWithInsights(summary = summary, format = clpFormat, uiState = uiState)
                        } else if (computedBalance != null) {
                            BalanceCardWithInsights(
                                summary = DashboardSummary(
                                    householdId       = "",
                                    totalBalance      = computedBalance,
                                    totalIncome       = 0.0,
                                    totalExpense      = 0.0,
                                    accountsCount     = 0,
                                    transactionsCount = 0
                                ),
                                format  = clpFormat,
                                uiState = uiState
                            )
                        }
                    }
                }
                // ── Tarjetas de crédito ────────────────────────────────────────
                if (uiState.creditCards.isNotEmpty()) {
                    item {
                        CreditCardsCard(cards = uiState.creditCards, format = clpFormat)
                    }
                }

                // ── Próximos Vencimientos ──────────────────────────────────────
                if (uiState.upcomingBills.isNotEmpty()) {
                    item {
                        UpcomingBillsCard(
                            bills        = uiState.upcomingBills,
                            totalPending = uiState.pendingBillsTotal,
                            format       = clpFormat,
                            onClick      = onNavigateToRecurring
                        )
                    }
                }

                // ── Gráfico de tendencia ───────────────────────────────────────
                item {
                    Box(modifier = Modifier.testTag("dashboard_chart")) {
                        MonthlyChart(monthlyData = uiState.monthlyBalance)
                    }
                }

                // ── Gastos por categoría ───────────────────────────────────────
                item {
                    CategoryExpensesButton(onClick = onNavigateToCategoryExp)
                }

                // ── Ahorro ────────────────────────────────────────────────────
                if (uiState.totalSavings != 0L) {
                    item {
                        SavingsCard(totalSavings = uiState.totalSavings, format = clpFormat)
                    }
                }

                // ── Últimos movimientos ────────────────────────────────────────
                item {
                    Box(modifier = Modifier.testTag("dashboard_recent")) {
                        RecentTransactionsList(
                            transactions      = uiState.recentTransactions,
                            format            = clpFormat,
                            onTransactionClick = onTransactionClick,
                            onSeeAllClick     = onSeeAllClick
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            if (showTutorial) {
                TutorialOverlay(
                    module     = TutorialModule.DASHBOARD,
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
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryExpensesButton
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CategoryExpensesButton(onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Gastos por Categoría", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("Ver desglose del mes actual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BalanceCardWithInsights
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BalanceCardWithInsights(
    summary : DashboardSummary,
    format  : NumberFormat,
    uiState : DashboardUiState
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier              = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text("Saldo Disponible", style = MaterialTheme.typography.labelMedium)
                Text(
                    text       = format.format(uiState.computedTotalBalance ?: summary.totalBalance),
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )

                if (uiState.actualLiquidity != null && uiState.pendingBillsTotal > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint     = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text       = "Liquidez Real: ${format.format(uiState.actualLiquidity)}",
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        "Saldo disponible tras pagar cuentas",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CAF50), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ingresos: ${format.format(summary.totalIncome)}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFF44336), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gastos: ${format.format(summary.totalExpense)}", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Gráfico de Dona
            Box(
                modifier          = Modifier.size(100.dp).weight(0.8f),
                contentAlignment  = Alignment.Center
            ) {
                val total        = (summary.totalIncome + summary.totalExpense).toFloat()
                val incomeAngle  = if (total > 0) (summary.totalIncome.toFloat() / total) * 360f else 180f
                val expenseAngle = 360f - incomeAngle

                Canvas(modifier = Modifier.size(80.dp)) {
                    drawArc(color = Color(0xFF4CAF50), startAngle = -90f, sweepAngle = incomeAngle,  useCenter = false, style = Stroke(width = 25f, cap = StrokeCap.Round))
                    drawArc(color = Color(0xFFF44336), startAngle = -90f + incomeAngle, sweepAngle = expenseAngle, useCenter = false, style = Stroke(width = 25f, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val ratio = if (summary.totalIncome > 0) (summary.totalExpense / summary.totalIncome * 100).toInt() else 0
                    Text(
                        text       = "$ratio%",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color      = if (ratio > 80) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface
                    )
                    Text("gastado", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BalanceCard (Legacy)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BalanceCard(summary: DashboardSummary, format: NumberFormat, uiState: DashboardUiState = DashboardUiState()) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor   = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Saldo Disponible", style = MaterialTheme.typography.labelMedium)
            Text(
                text       = format.format(uiState.computedTotalBalance ?: summary.totalBalance),
                style      = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryInfo("Ingresos", summary.totalIncome, Color(0xFF4CAF50), format)
                VerticalDivider(modifier = Modifier.height(40.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                SummaryInfo("Gastos", summary.totalExpense, Color(0xFFF44336), format)
            }
        }
    }
}

@Composable
private fun SummaryInfo(label: String, amount: Double, color: Color, format: NumberFormat) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(text = format.format(amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MonthlyChart
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MonthlyChart(monthlyData: List<MonthlyBalance>) {
    val monthNames = listOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")
    val clpFormat  = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Tendencia Mensual", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (selectedIndex != null) {
                    TextButton(onClick = { selectedIndex = null }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
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
                    val netColor  = if (item.net >= 0) Color(0xFF43A047) else Color(0xFFE53935)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape    = RoundedCornerShape(8.dp),
                        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface)
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("$monthName ${item.yearNum}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.inverseOnSurface)
                                Text("Ingresos: ${clpFormat.format(item.income)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f))
                                Text("Gastos: ${clpFormat.format(item.expense)}",  style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f))
                            }
                            Text(text = clpFormat.format(item.net), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = netColor)
                        }
                    }
                }
            }

            val hasData = monthlyData.any { it.net != 0L }
            if (!hasData) {
                Box(modifier = Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        Text("Sin movimientos aún", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                return@Column
            }

            val positiveColor = Color(0xFF4CAF50)
            val negativeColor = Color(0xFFF44336)
            val selectedColor = Color(0xFF1565C0)
            val labelColor    = Color(0xFF757575)
            val maxAbs        = monthlyData.maxOf { kotlin.math.abs(it.net) }.takeIf { it > 0L } ?: 1L
            val MIN_BAR_HEIGHT = 8f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .pointerInput(monthlyData) {
                        detectTapGestures { offset ->
                            val count = monthlyData.size
                            if (count == 0) return@detectTapGestures
                            val slotW  = size.width / count.toFloat()
                            val tapped = (offset.x / slotW).toInt().coerceIn(0, count - 1)
                            selectedIndex = if (selectedIndex == tapped) null else tapped
                        }
                    }
            ) {
                val count      = monthlyData.size
                val slotWidth  = size.width / count
                val barWidth   = slotWidth * 0.5f
                val barAreaH   = size.height * 0.72f
                val baselineY  = size.height * 0.72f
                val labelY     = size.height - 4f

                drawLine(color = Color(0xFFBDBDBD), start = Offset(0f, baselineY), end = Offset(size.width, baselineY), strokeWidth = 1.5f)

                monthlyData.forEachIndexed { i, item ->
                    val isSelected    = (selectedIndex == i)
                    val centerX       = slotWidth * i + slotWidth / 2f
                    val barLeft       = centerX - barWidth / 2f
                    val fraction      = item.net.toFloat() / maxAbs.toFloat()
                    val computedBarH  = kotlin.math.abs(fraction) * barAreaH * 0.85f
                    val barH          = if (item.net != 0L) maxOf(computedBarH, MIN_BAR_HEIGHT) else computedBarH
                    val topY          = if (item.net >= 0) baselineY - barH else baselineY
                    val barColor      = when { isSelected -> selectedColor; item.net >= 0 -> positiveColor; else -> negativeColor }
                    val alpha         = when { isSelected -> 1f; selectedIndex != null -> 0.35f; i == count - 1 -> 1f; else -> 0.75f }

                    drawRect(color = barColor, topLeft = Offset(barLeft, topY), size = Size(barWidth, barH), alpha = alpha)

                    if (item.net != 0L && computedBarH < 3f) {
                        val dotY = if (item.net >= 0) topY - 6f else topY + barH + 6f
                        drawCircle(color = barColor, radius = 3f, center = Offset(centerX, dotY), alpha = alpha)
                    }

                    val label = monthNames[(item.monthNum - 1).coerceIn(0, 11)]
                    drawContext.canvas.nativeCanvas.drawText(
                        label, centerX, labelY,
                        android.graphics.Paint().apply {
                            color     = if (isSelected) android.graphics.Color.rgb(21, 101, 192) else labelColor.toArgb()
                            textSize  = 28f
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
// UpcomingBillsCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun UpcomingBillsCard(
    bills        : List<RecurringBill>,
    totalPending : Long,
    format       : NumberFormat,
    onClick      : () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventRepeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Próximos Vencimientos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(12.dp))

            bills.forEach { bill ->
                val status      = bill.status()
                val statusColor = when (status) {
                    RecurringBillStatus.OVERDUE  -> Color(0xFFB71C1C)
                    RecurringBillStatus.DUE_SOON -> Color(0xFFE65100)
                    else                         -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(bill.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(bill.statusLabel(), style = MaterialTheme.typography.labelSmall, color = statusColor)
                    }
                    Text(format.format(bill.amountClp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Comprometido este mes:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = format.format(totalPending), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
// ─────────────────────────────────────────────────────────────────────────────
// CreditCardsCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CreditCardsCard(cards: List<AccountBalance>, format: NumberFormat) {
    val totalDebt = cards.sumOf { it.movementBalance }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tarjetas de crédito", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text(
                    text       = format.format(kotlin.math.abs(totalDebt)),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFFC62828)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            cards.forEach { card ->
                val debt         = kotlin.math.abs(card.movementBalance)
                val limit        = card.creditLimit ?: 0L
                val progress     = if (limit > 0) (debt.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 0f
                val usagePercent = (progress * 100).toInt()
                val available    = maxOf(0L, limit - debt)
                val isHighUsage  = progress > 0.75f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(card.accountName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isHighUsage) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text     = "$usagePercent% usado",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style    = MaterialTheme.typography.labelSmall,
                                color    = if (isHighUsage) Color(0xFFE65100) else Color(0xFF2E7D32)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text  = "Deuda: ${format.format(debt)}${if (limit > 0) " / ${format.format(limit)}" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (available > 0) {
                            Text(
                                text       = "${format.format(available)} libre",
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF2E7D32)
                            )
                        }
                    }

                    if (limit > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress  = { progress },
                            modifier  = Modifier.fillMaxWidth().height(5.dp),
                            color     = if (isHighUsage) Color(0xFFC62828) else Color(0xFF2E7D32),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
// ─────────────────────────────────────────────────────────────────────────────
// SavingsCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SavingsCard(totalSavings: Long, format: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Savings, null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Ahorro total", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6A1B9A))
                Text(text = format.format(totalSavings), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF6A1B9A))
                Text("No incluido en el balance operativo", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}



// ─────────────────────────────────────────────────────────────────────────────
// RecentTransactionsList
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun RecentTransactionsList(
    transactions       : List<Transaction>,
    format             : NumberFormat,
    onTransactionClick : (String) -> Unit,
    onSeeAllClick      : () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Últimos Movimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onSeeAllClick) { Text("Ver todos") }
        }

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Sin movimientos este mes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            transactions.forEach { TransactionRowItem(it, format, onTransactionClick) }
        }
    }
}

@Composable
private fun TransactionRowItem(
    transaction : Transaction,
    format      : NumberFormat,
    onClick     : (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(transaction.id) },
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val (icon, iconColor) = when (transaction.type) {
                "income"   -> Icons.Default.ArrowDownward to Color(0xFF4CAF50)
                "expense"  -> Icons.Default.ArrowUpward   to Color(0xFFF44336)
                "transfer" -> Icons.Default.SwapHoriz     to Color(0xFF2196F3)
                else       -> Icons.Default.AttachMoney   to Color(0xFF9E9E9E)
            }
            Surface(shape = MaterialTheme.shapes.small, color = iconColor.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(8.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaction.description ?: "Sin descripción", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = formatChileDate(transaction.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val amountColor = when (transaction.type) {
                "income"   -> Color(0xFF4CAF50)
                "expense"  -> Color(0xFFF44336)
                "transfer" -> Color(0xFF2196F3)
                else       -> MaterialTheme.colorScheme.onSurface
            }
            Text(text = format.format(transaction.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = amountColor)
        }
    }
}