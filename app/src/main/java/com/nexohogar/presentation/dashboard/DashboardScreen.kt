package com.nexohogar.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexohogar.domain.model.DashboardSummary
import com.nexohogar.domain.model.Transaction
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onTransactionClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    onCreateTransaction: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            LoadingOverlay()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. BalanceCard (Saldo, Ingresos, Gastos)
            item {
                uiState.summary?.let { summary ->
                    BalanceCard(summary = summary, format = clpFormat)
                }
            }

            // 2. MonthlyChart (Gráfico de barras)
            item {
                MonthlyChart()
            }

            // 3. QuickActionsRow (Acciones rápidas)
            item {
                QuickActionsRow(onActionClick = onCreateTransaction)
            }

            // 4. RecentTransactionsList (Últimos movimientos)
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

@Composable
fun BalanceCard(
    summary: DashboardSummary,
    format: NumberFormat
) {
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
            Text(
                text = "Saldo Total",
                style = MaterialTheme.typography.labelMedium
            )
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
                SummaryInfo(
                    label = "Ingresos",
                    amount = summary.totalIncome,
                    color = Color(0xFF4CAF50),
                    format = format
                )
                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                SummaryInfo(
                    label = "Gastos",
                    amount = summary.totalExpense,
                    color = Color(0xFFF44336),
                    format = format
                )
            }
        }
    }
}

@Composable
private fun SummaryInfo(
    label: String,
    amount: Double,
    color: Color,
    format: NumberFormat
) {
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

@Composable
fun MonthlyChart() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Text(
                    text = "Tendencia Mensual (Próximamente)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuickActionsRow(onActionClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionItem(
            modifier = Modifier.weight(1f),
            label = "Ingreso",
            icon = Icons.Default.ArrowDownward,
            color = Color(0xFF4CAF50),
            onClick = { onActionClick("income") }
        )
        QuickActionItem(
            modifier = Modifier.weight(1f),
            label = "Gasto",
            icon = Icons.Default.ArrowUpward,
            color = Color(0xFFF44336),
            onClick = { onActionClick("expense") }
        )
        QuickActionItem(
            modifier = Modifier.weight(1f),
            label = "Traspaso",
            icon = Icons.Default.SwapHoriz,
            color = Color(0xFF2196F3),
            onClick = { onActionClick("transfer") }
        )
        QuickActionItem(
            modifier = Modifier.weight(1f),
            label = "Tarjeta",
            icon = Icons.Default.CreditCard,
            color = Color(0xFF9C27B0),
            onClick = { onActionClick("credit_payment") }
        )
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
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

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
                text = "No hay movimientos recientes.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            transactions.forEach { transaction ->
                TransactionRowItem(transaction, format, onTransactionClick)
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(transaction.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (transaction.amount >= 0) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFF44336).copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (transaction.amount >= 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (transaction.amount >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
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
                Text(
                    text = transaction.createdAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = format.format(transaction.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.amount >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}
