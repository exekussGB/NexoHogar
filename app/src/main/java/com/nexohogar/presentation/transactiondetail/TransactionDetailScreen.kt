package com.nexohogar.presentation.transactiondetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexohogar.core.util.DateFormatter
import com.nexohogar.domain.model.TransactionDetail
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: String,
    viewModel: TransactionDetailViewModel,
    onNavigateBack: () -> Unit,
    openInEditMode: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(transactionId) {
        viewModel.loadTransactionDetail(transactionId)
    }

    // Auto-start editing if navigated with edit=true.
    // IMPORTANT: uses a one-shot flag to avoid re-triggering when cancelEditing()
    // sets isEditing=false (which would cause a blink and re-open the editor).
    var autoStarted by remember { mutableStateOf(false) }
    LaunchedEffect(uiState) {
        if (!autoStarted && openInEditMode &&
            uiState is TransactionDetailUiState.Success &&
            !(uiState as TransactionDetailUiState.Success).isEditing) {
            autoStarted = true
            viewModel.startEditing()
        }
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
                actions = {
                    val state = uiState
                    if (state is TransactionDetailUiState.Success && state.isSuperUser && !state.isEditing) {
                        IconButton(onClick = { viewModel.startEditing() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                        }
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
                is TransactionDetailUiState.Success -> {
                    if (state.isEditing) {
                        EditTransactionContent(
                            state = state,
                            onAmountChange = viewModel::onEditAmountChange,
                            onDescriptionChange = viewModel::onEditDescriptionChange,
                            onDateChange = viewModel::onEditDateChange,
                            onSave = viewModel::saveEdit,
                            onCancel = viewModel::cancelEditing,
                            onClearError = viewModel::clearEditError
                        )
                    } else {
                        TransactionDetailContent(state.detail)
                    }
                }
                is TransactionDetailUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.loadTransactionDetail(transactionId) }
                )
            }
        }
    }
}

@Composable
private fun EditTransactionContent(
    state: TransactionDetailUiState.Success,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Editando transaccion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Solo puedes modificar monto, descripcion y fecha", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                }
            }
        }

        OutlinedTextField(
            value = state.editAmount,
            onValueChange = onAmountChange,
            label = { Text("Monto (CLP)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            prefix = { Text("$") },
            enabled = !state.isSaving
        )

        OutlinedTextField(
            value = state.editDescription,
            onValueChange = onDescriptionChange,
            label = { Text("Descripcion") },
            singleLine = false,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving
        )

        OutlinedTextField(
            value = state.editDate,
            onValueChange = onDateChange,
            label = { Text("Fecha (yyyy-MM-dd)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
            placeholder = { Text("2026-01-15") }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("El tipo de transaccion y las cuentas no se pueden modificar por integridad contable.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
            }
        }

        state.editError?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(text = error, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), enabled = !state.isSaving) { Text("Cancelar") }
            Button(onClick = onSave, modifier = Modifier.weight(1f), enabled = !state.isSaving && state.editAmount.isNotBlank() && state.editDescription.isNotBlank()) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Guardar")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

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
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(typeColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = typeLabel, style = MaterialTheme.typography.labelLarge, color = typeColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = clpFormat.format(detail.amountClp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                val fechaHora = formatDateTimeForDisplay(transactionDate = detail.transactionDate, createdAt = detail.createdAt)
                DetailRow(Icons.Default.CalendarToday, "Fecha y hora", fechaHora)

                detail.description?.let { desc ->
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    DetailRow(Icons.Default.Notes, "Descripcion", desc)
                }

                val fromLabel = if (detail.type.lowercase() == "transfer") "Cuenta origen" else "Cuenta"
                detail.fromAccountName?.let { name ->
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    DetailRow(Icons.Default.AccountBalance, fromLabel, name)
                }

                if (detail.type.lowercase() == "transfer") {
                    detail.toAccountName?.let { name ->
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        DetailRow(Icons.Default.AccountBalance, "Cuenta destino", name)
                    }
                }

                detail.createdByName?.let { name ->
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    DetailRow(Icons.Default.Person, "Registrado por", name)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun formatDateTimeForDisplay(transactionDate: String?, createdAt: String?): String {
    val timePart = createdAt?.let {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(it.substringBefore("+").substringBefore("Z"))
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            outputFormat.format(date!!)
        } catch (e: Exception) {
            null
        }
    }
    val datePart = transactionDate?.let { DateFormatter.formatForDisplay(it) } ?: "Sin fecha"
    return if (timePart != null) "$datePart a las $timePart hrs" else datePart
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}
