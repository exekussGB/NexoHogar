package com.nexohogar.presentation.transactiondetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexohogar.domain.model.TransactionDetail
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: String,
    viewModel: TransactionDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(transactionId) {
        viewModel.loadTransactionDetail(transactionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Movimiento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
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
            when (val state = uiState) {
                is TransactionDetailUiState.Loading -> LoadingOverlay()
                is TransactionDetailUiState.Success -> TransactionDetailContent(state.detail)
                is TransactionDetailUiState.Error   -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.loadTransactionDetail(transactionId) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Contenido principal
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TransactionDetailContent(detail: TransactionDetail) {
    val typeColor = when (detail.type.lowercase()) {
        "income"   -> Color(0xFF4CAF50)
        "expense"  -> Color(0xFFF44336)
        "transfer" -> Color(0xFF2196F3)
        else       -> Color.Gray
    }
    val typeIcon: ImageVector = when (detail.type.lowercase()) {
        "income"   -> Icons.Default.ArrowDownward
        "expense"  -> Icons.Default.ArrowUpward
        "transfer" -> Icons.Default.SwapHoriz
        else       -> Icons.Default.AttachMoney
    }
    val typeLabel = when (detail.type.lowercase()) {
        "income"   -> "Ingreso"
        "expense"  -> "Gasto"
        "transfer" -> "Transferencia"
        else       -> detail.type
    }

    val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Cabecera: tipo + ícono + monto ─────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = typeColor.copy(alpha = 0.12f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(typeColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = typeColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = clpFormat.format(detail.amountClp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ── Filas de detalle ───────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {

                detail.description?.let { desc ->
                    DetailRow(Icons.Default.Notes, "Descripción", desc)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                detail.transactionDate?.let { date ->
                    DetailRow(Icons.Default.CalendarToday, "Fecha", formatDate(date))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                val fromLabel = if (detail.type.lowercase() == "transfer") "Cuenta origen" else "Cuenta"
                detail.fromAccountName?.let { name ->
                    DetailRow(Icons.Default.AccountBalance, fromLabel, name)
                }

                if (detail.type.lowercase() == "transfer") {
                    detail.toAccountName?.let { name ->
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        DetailRow(Icons.Default.AccountBalance, "Cuenta destino", name)
                    }
                }

                detail.status?.let { status ->
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    val statusLabel = when (status.lowercase()) {
                        "confirmed"  -> "Confirmado"
                        "pending"    -> "Pendiente"
                        "cancelled"  -> "Cancelado"
                        else         -> status
                    }
                    DetailRow(Icons.Default.CheckCircle, "Estado", statusLabel)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatDate(isoDate: String): String {
    return try {
        val dt = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        buildDateString(dt)
    } catch (e: Exception) {
        try {
            val dt = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            buildDateString(dt)
        } catch (e2: Exception) {
            isoDate
        }
    }
}

private fun buildDateString(dt: LocalDateTime): String {
    val month = dt.month.getDisplayName(TextStyle.SHORT, Locale("es", "ES"))
    val hh = dt.hour.toString().padStart(2, '0')
    val mm = dt.minute.toString().padStart(2, '0')
    return "${dt.dayOfMonth} $month ${dt.year} · $hh:$mm"
}