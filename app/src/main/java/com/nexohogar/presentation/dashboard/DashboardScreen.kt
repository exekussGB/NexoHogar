package com.nexohogar.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.model.MonthlyBalance
import com.nexohogar.domain.model.Transaction
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.util.*

// ─── Utilidad: formatea "2026-03-20T12:52:10.832678+00:00" → "20 mar 2026 · 12:52"
private fun formatDate(isoDate: String): String {
    return try {
        val datePart = isoDate.substringBefore("T")
        val timePart = isoDate.substringAfter("T").substring(0, 5)
        val (year, month, day) = datePart.split("-")
        val monthNames = listOf("ene","feb","mar","abr","may","jun","jul","ago","sep","oct","nov","dic")
        val monthName = monthNames[(month.toInt() - 1).coerceIn(0, 11)]
        "$day $monthName $year · $timePart"
    } catch (_: Exception) { isoDate }
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onTransactionClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    onCreateTransaction: (String) -> Unit
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
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Tarjeta saldo total
            item {
                uiState.summary?.let { BalanceCard(summary = it, format = clpFormat) }
            }

            // 2. Saldos por cuenta (nuevo)
            item {
                if (uiState.accountBalances.isNotEmpty()) {
                    AccountBalancesSection(
                        accounts = uiState.accountBalances,
                        format = clpFormat
                    )
                }
            }

            // 3. Gráfico mensual
            item {
                MonthlyChart(monthlyData = uiState.monthlyBalance)
            }

            // 4. Acciones rápidas
            item {
                QuickActionsRow(onActionClick = onCreateTransaction)
            }

            // 5. Últimos movimientos
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

// ─── Saldos por cuenta ────────────────────────────────────────────────────────

@Composable
fun AccountBalancesSection(accounts: List<AccountBalance>, format: NumberFormat) {
    Column {
        Text(
            text = "Mis Cuentas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filtrar cuentas del sistema
            accounts
                .filter { !it.accountName.startsWith("__SYSTEM") }
                .forEach { account ->
                    AccountBalanceCard(account = account, format = format)
                }
        }
    }
}

@Composable
fun AccountBalanceCard(account: AccountBalance, format: NumberFormat) {
    val (icon, color) = when (account.accountType.uppercase()) {
        "ASSET"     -> Icons.Default.AccountBalance to Color(0xFF1976D2)
        "LIABILITY" -> Icons.Default.CreditCard     to Color(0xFF9C27B0)
        else        -> Icons.Default.Wallet         to Color(0xFF757575)
    }

    Card(
        modifier = Modifier.width(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = account.accountName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = format.format(account.movementBalance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (account.movementBalance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
    }
}

// ─── Tarjeta saldo total ──────────────────────────────────────────────────────

@Composable
fun BalanceCard(summary: DashboardSummary, format: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor   = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Saldo Total", style = MaterialTheme.typography.labelMedium)
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
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(
            text = format.format(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ─── Gráfico mensual ──────────────────────────────────────────────────────────

@Composable
fun MonthlyChart(monthlyData: List<MonthlyBalance>) {
    val monthNames = listOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tendencia Mensual", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            val hasData = monthlyData.any { it.net != 0L }

            if (!hasData) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(130.dp),
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
                            text = "Sin movimientos aún",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                return@Column
            }

            val positiveColor = Color(0xFF4CAF50)
            val negativeColor = Color(0xFFF44336)
            val labelColor    = Color(0xFF757575)
            val maxAbs = monthlyData.maxOf { Math.abs(it.net) }.takeIf { it > 0 } ?: 1L

            Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                val count       = monthlyData.size
                val slotWidth   = size.width / count
                val barWidth    = slotWidth * 0.5f
                val barAreaH    = size.height * 0.72f
                val baselineY   = size.height * 0.72f
                val labelY      = size.height - 4f

                drawLine(
                    color = Color(0xFFBDBDBD),
                    start = Offset(0f, baselineY),
                    end   = Offset(size.width, baselineY),
                    strokeWidth = 1.5f
                )

                monthlyData.forEachIndexed { i, item ->
                    val centerX  = slotWidth * i + slotWidth / 2f
                    val fraction = item.net.toFloat() / maxAbs.toFloat()
                    val barH     = Math.abs(fraction) * barAreaH * 0.85f
                    val barColor = if (item.net >= 0) positiveColor else negativeColor
                    val topY     = if (item.net >= 0) baselineY - barH else baselineY

                    drawRect(
                        color   = barColor,
                        topLeft = Offset(centerX - barWidth / 2f, topY),
                        size    = Size(barWidth, barH),
                        alpha   = if (i == monthlyData.size - 1) 1f else 0.75f
                    )

                    val label = monthNames[(item.monthNum - 1).coerceIn(0, 11)]
                    drawContext.canvas.nativeCanvas.drawText(
                        label, centerX, labelY,
                        android.graphics.Paint().apply {
                            color     = labelColor.toArgb()
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

// ─── Acciones rápidas ─────────────────────────────────────────────────────────

@Composable
fun QuickActionsRow(onActionClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionItem(Modifier.weight(1f), "Ingreso",   Icons.Default.ArrowDownward, Color(0xFF4CAF50)) { onActionClick("income") }
        QuickActionItem(Modifier.weight(1f), "Gasto",     Icons.Default.ArrowUpward,   Color(0xFFF44336)) { onActionClick("expense") }
        QuickActionItem(Modifier.weight(1f), "Traspaso",  Icons.Default.SwapHoriz,     Color(0xFF2196F3)) { onActionClick("transfer") }
        QuickActionItem(Modifier.weight(1f), "Tarjeta",   Icons.Default.CreditCard,    Color(0xFF9C27B0)) { onActionClick("credit_payment") }
    }
}

@Composable
fun QuickActionItem(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.aspectRatio(1f).clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

// ─── Últimos movimientos ──────────────────────────────────────────────────────

@Composable
fun RecentTransactionsList(
    transactions: List<Transaction>,
    format: NumberFormat,
    onTransactionClick: (String) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Últimos Movimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onSeeAllClick) { Text("Ver todos") }
        }

        if (transactions.isEmpty()) {
            Text(
                text = "No hay movimientos recientes.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            transactions.forEach { TransactionRowItem(it, format, onTransactionClick) }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    format: NumberFormat,
    onClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(transaction.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val (icon, iconColor) = when (transaction.type) {
                "income"   -> Icons.Default.ArrowDownward to Color(0xFF4CAF50)
                "expense"  -> Icons.Default.ArrowUpward   to Color(0xFFF44336)
                "transfer" -> Icons.Default.SwapHoriz     to Color(0xFF2196F3)
                else       -> Icons.Default.AttachMoney   to Color(0xFF9E9E9E)
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(8.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description ?: "Sin descripción",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                // FIX: fecha formateada
                Text(
                    text = formatDate(transaction.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val amountColor = when (transaction.type) {
                "income"   -> Color(0xFF4CAF50)
                "expense"  -> Color(0xFFF44336)
                "transfer" -> Color(0xFF2196F3)
                else       -> MaterialTheme.colorScheme.onSurface
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