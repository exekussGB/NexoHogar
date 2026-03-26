package com.nexohogar.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.model.DashboardSection
import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.model.MonthlyBalance
import com.nexohogar.domain.model.Transaction
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onTransactionClick: (String) -> Unit,
    onSeeAllClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

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
            // ── Fondo Común ──
            item {
                uiState.sharedSection?.let { section ->
                    SectionHeader(
                        title = "\uD83C\uDFE0 Fondo Común",
                        subtitle = "${section.accountsCount} cuentas"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionBalanceCard(
                        section = section,
                        format = clpFormat,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                } ?: uiState.summary?.let {
                    // Fallback: show old summary if dual not loaded
                    BalanceCard(summary = it, format = clpFormat)
                }
            }

            // Shared account chips
            if (uiState.sharedAccounts.isNotEmpty()) {
                item {
                    AccountChips(accounts = uiState.sharedAccounts, format = clpFormat)
                }
            }

            // ── Personal section (only if user has personal accounts) ──
            if (uiState.personalSection != null) {
                item {
                    SectionHeader(
                        title = "\uD83D\uDC64 Mi Billetera Personal",
                        subtitle = "${uiState.personalSection!!.accountsCount} cuentas"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionBalanceCard(
                        section = uiState.personalSection!!,
                        format = clpFormat,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
                if (uiState.personalAccounts.isNotEmpty()) {
                    item {
                        AccountChips(accounts = uiState.personalAccounts, format = clpFormat)
                    }
                }
            }

            // ── Monthly chart ──
            item {
                MonthlyChart(monthlyData = uiState.monthlyBalance)
            }

            // ── Recent transactions ──
            item {
                RecentTransactionsList(
                    transactions = uiState.recentTransactions,
                    format = clpFormat,
                    onTransactionClick = onTransactionClick,
                    onSeeAllClick = onSeeAllClick
                )
            }
        }
    }
}

// ─── Section Header ─────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Section Balance Card (for dual dashboard) ─────────────────────────
@Composable
fun SectionBalanceCard(section: DashboardSection, format: NumberFormat, containerColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Saldo", style = MaterialTheme.typography.labelMedium)
            Text(
                text = format.format(section.totalBalance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            if (section.totalIncome > 0 || section.totalExpense > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ingresos", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = format.format(section.totalIncome),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Gastos", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = format.format(section.totalExpense),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                    }
                }
            }
        }
    }
}

// ─── Account Chips ──────────────────────────────────────────────────────
@Composable
fun AccountChips(accounts: List<AccountBalance>, format: NumberFormat) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(accounts) { account ->
            val balanceColor = if (account.movementBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            AssistChip(
                onClick = {},
                label = {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(account.accountName, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = format.format(account.movementBalance),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = balanceColor
                        )
                    }
                },
                leadingIcon = {
                    val icon = when (account.accountType.lowercase()) {
                        "asset" -> Icons.Default.Savings
                        "liability" -> Icons.Default.CreditCard
                        else -> Icons.Default.AccountBalanceWallet
                    }
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
    }
}

// ─── Balance Card (fallback for old summary) ────────────────────────────
@Composable
fun BalanceCard(summary: DashboardSummary, format: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Saldo Total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = format.format(summary.totalBalance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Ingresos",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Ingresos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = format.format(summary.totalIncome),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = "Gastos",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Gastos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = format.format(summary.totalExpense),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "${summary.accountsCount} cuentas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "${summary.transactionsCount} movimientos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─── Monthly Chart ──────────────────────────────────────────────────────
@Composable
fun MonthlyChart(monthlyData: List<MonthlyBalance>) {
    if (monthlyData.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tendencia Mensual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .padding(end = 4.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(Color(0xFF4CAF50))
                    }
                }
                Text(" Ingresos", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .padding(end = 4.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(Color(0xFFF44336))
                    }
                }
                Text(" Gastos", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            val incomeColor = Color(0xFF4CAF50)
            val expenseColor = Color(0xFFF44336)
            val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
            val sortedData = monthlyData.sortedWith(compareBy({ it.yearNum }, { it.monthNum })).takeLast(6)

            val maxValue = sortedData.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0
            val monthNames = listOf("", "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

            var selectedIndex by remember { mutableIntStateOf(-1) }
            val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

            // Show tooltip for selected bar
            if (selectedIndex in sortedData.indices) {
                val selected = sortedData[selectedIndex]
                val monthLabel = monthNames.getOrElse(selected.monthNum) { "?" }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("$monthLabel ${selected.yearNum}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text("Ingreso: ${clpFormat.format(selected.income)}", style = MaterialTheme.typography.labelSmall, color = incomeColor)
                            Text("Gasto: ${clpFormat.format(selected.expense)}", style = MaterialTheme.typography.labelSmall, color = expenseColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(sortedData) {
                        detectTapGestures { offset ->
                            val chartWidth = size.width.toFloat()
                            val leftPadding = 10f
                            val rightPadding = 10f
                            val usableWidth = chartWidth - leftPadding - rightPadding
                            val groupWidth = usableWidth / sortedData.size
                            val tappedIndex = ((offset.x - leftPadding) / groupWidth).toInt()
                            selectedIndex = if (tappedIndex in sortedData.indices) {
                                if (selectedIndex == tappedIndex) -1 else tappedIndex
                            } else -1
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val bottomPadding = 30f
                val topPadding = 10f
                val leftPadding = 10f
                val rightPadding = 10f
                val chartHeight = canvasHeight - bottomPadding - topPadding
                val usableWidth = canvasWidth - leftPadding - rightPadding

                val groupWidth = usableWidth / sortedData.size
                val barWidth = groupWidth * 0.35f
                val gap = groupWidth * 0.05f

                sortedData.forEachIndexed { index, data ->
                    val groupStart = leftPadding + index * groupWidth
                    val incomeHeight = if (maxValue > 0) (data.income / maxValue * chartHeight).toFloat() else 0f
                    val expenseHeight = if (maxValue > 0) (data.expense / maxValue * chartHeight).toFloat() else 0f

                    // Income bar
                    drawRect(
                        color = if (selectedIndex == index) incomeColor.copy(alpha = 0.8f) else incomeColor,
                        topLeft = Offset(
                            x = groupStart + gap,
                            y = topPadding + chartHeight - incomeHeight
                        ),
                        size = Size(barWidth, incomeHeight)
                    )

                    // Expense bar
                    drawRect(
                        color = if (selectedIndex == index) expenseColor.copy(alpha = 0.8f) else expenseColor,
                        topLeft = Offset(
                            x = groupStart + barWidth + gap * 2,
                            y = topPadding + chartHeight - expenseHeight
                        ),
                        size = Size(barWidth, expenseHeight)
                    )

                    // Month label
                    val monthLabel = monthNames.getOrElse(data.monthNum) { "?" }
                    drawContext.canvas.nativeCanvas.drawText(
                        monthLabel,
                        groupStart + groupWidth / 2,
                        canvasHeight - 5f,
                        android.graphics.Paint().apply {
                            color = textColor
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

// ─── Recent Transactions List ───────────────────────────────────────────
@Composable
fun RecentTransactionsList(
    transactions: List<Transaction>,
    format: NumberFormat,
    onTransactionClick: (String) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Últimos Movimientos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onSeeAllClick) {
                    Text("Ver todos")
                }
            }

            if (transactions.isEmpty()) {
                Text(
                    text = "No hay movimientos registrados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                transactions.forEach { transaction ->
                    TransactionRowItem(
                        transaction = transaction,
                        format = format,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                }
            }
        }
    }
}

// ─── Transaction Row Item ───────────────────────────────────────────────
@Composable
fun TransactionRowItem(
    transaction: Transaction,
    format: NumberFormat,
    onClick: () -> Unit
) {
    val isIncome = transaction.type.lowercase() == "income"
    val amountColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    val amountPrefix = if (isIncome) "+" else "-"
    val icon = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = amountColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transaction.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$amountPrefix${format.format(kotlin.math.abs(transaction.amount))}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 36.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
