package com.nexohogar.presentation.recurringbills

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexohogar.domain.model.RecurringBill
import com.nexohogar.domain.model.RecurringBillStatus
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillsScreen(
    viewModel: RecurringBillsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuentas Recurrentes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar cuenta recurrente")
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

                uiState.error != null -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadBills() }) { Text("Reintentar") }
                }

                uiState.bills.isEmpty() -> EmptyBillsState { viewModel.onShowCreateDialog() }

                else -> {
                    // Separar activas e inactivas
                    val active   = uiState.bills.filter { it.isActive }
                    val inactive = uiState.bills.filter { !it.isActive }
                    val alertCount = active.count {
                        val st = it.status()
                        st == RecurringBillStatus.OVERDUE || st == RecurringBillStatus.DUE_SOON
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Banner de alertas
                        if (alertCount > 0) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFF6F00).copy(alpha = 0.12f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF6F00))
                                        Text(
                                            text = if (alertCount == 1) "1 cuenta vence pronto o está vencida"
                                            else "$alertCount cuentas vencen pronto o están vencidas",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                            }
                        }

                        if (active.isNotEmpty()) {
                            item {
                                Text(
                                    "Activas",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(active) { bill ->
                                RecurringBillItem(
                                    bill = bill,
                                    format = clpFormat,
                                    onMarkAsPaid   = { viewModel.confirmMarkAsPaid(bill) },
                                    onToggleActive = { viewModel.toggleActive(bill) },
                                    onDelete       = { viewModel.deleteBill(bill) }
                                )
                            }
                        }

                        if (inactive.isNotEmpty()) {
                            item {
                                Text(
                                    "Pausadas",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(inactive) { bill ->
                                RecurringBillItem(
                                    bill = bill,
                                    format = clpFormat,
                                    onMarkAsPaid   = {},
                                    onToggleActive = { viewModel.toggleActive(bill) },
                                    onDelete       = { viewModel.deleteBill(bill) }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // ── Diálogo de creación ──────────────────────────────────────────────────
    if (uiState.showCreateDialog) {
        CreateBillDialog(
            isCreating  = uiState.isCreating,
            createError = uiState.createError,
            onDismiss   = { viewModel.onDismissCreateDialog() },
            onCreate    = { name, amount, day, notes -> viewModel.createBill(name, amount, day, notes) }
        )
    }

    // ── Diálogo de confirmar pago ────────────────────────────────────────────
    val billToPay = uiState.billToPay
    if (billToPay != null) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isMarkingPaid) viewModel.dismissPayDialog() },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32)) },
            title = { Text("Marcar como pagado") },
            text = {
                Text("¿Confirmas que ya pagaste \"${billToPay.name}\" hoy?")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.markAsPaid() },
                    enabled = !uiState.isMarkingPaid
                ) {
                    if (uiState.isMarkingPaid) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Sí, pagado")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissPayDialog() },
                    enabled = !uiState.isMarkingPaid
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Item de cuenta recurrente
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecurringBillItem(
    bill: RecurringBill,
    format: NumberFormat,
    onMarkAsPaid: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val status = bill.status()

    val (statusColor, statusLabel, statusIcon) = when (status) {
        RecurringBillStatus.OVERDUE  -> Triple(Color(0xFFB71C1C), "VENCIDO", Icons.Default.ErrorOutline)
        RecurringBillStatus.DUE_SOON -> Triple(Color(0xFFE65100), "VENCE PRONTO", Icons.Default.Warning)
        RecurringBillStatus.PAID     -> Triple(Color(0xFF2E7D32), "PAGADO", Icons.Default.CheckCircle)
        RecurringBillStatus.OK       -> Triple(Color(0xFF1565C0), "Al día ${bill.dueDayOfMonth}", Icons.Default.CalendarToday)
        RecurringBillStatus.INACTIVE -> Triple(Color(0xFF757575), "PAUSADO", Icons.Default.PauseCircle)
    }

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (bill.isActive) 2.dp else 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                RecurringBillStatus.OVERDUE  -> Color(0xFFFFEBEE)
                RecurringBillStatus.DUE_SOON -> Color(0xFFFFF3E0)
                else                         -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(bill.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                        if (bill.amountClp > 0) {
                            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = format.format(bill.amountClp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Menú de 3 puntos
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        if (bill.isActive && status != RecurringBillStatus.PAID) {
                            DropdownMenuItem(
                                text = { Text("Marcar como pagado") },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32)) },
                                onClick = { expanded = false; onMarkAsPaid() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (bill.isActive) "Pausar" else "Activar") },
                            leadingIcon = {
                                Icon(
                                    if (bill.isActive) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = null
                                )
                            },
                            onClick = { expanded = false; onToggleActive() }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { expanded = false; onDelete() }
                        )
                    }
                }
            }

            // Nota si existe
            if (!bill.notes.isNullOrBlank()) {
                Text(
                    text = bill.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 12.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Estado vacío
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyBillsState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Repeat,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Sin cuentas recurrentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Agrega tus servicios mensuales (agua, luz, gas, internet...)\npara recibir alertas cuando vencen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Agregar primera cuenta")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Diálogo de creación
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CreateBillDialog(
    isCreating: Boolean,
    createError: String?,
    onDismiss: () -> Unit,
    onCreate: (name: String, amountClp: Long, dueDayOfMonth: Int, notes: String?) -> Unit
) {
    var name        by remember { mutableStateOf("") }
    var amountText  by remember { mutableStateOf("") }
    var dueDayText  by remember { mutableStateOf("") }
    var notes       by remember { mutableStateOf("") }
    var nameError   by remember { mutableStateOf(false) }
    var dayError    by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Nueva cuenta recurrente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Nombre (Ej: Agua, Luz, Gas)") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Monto mensual (opcional)") },
                    placeholder = { Text("0") },
                    prefix = { Text("$") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isCreating
                )

                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isDigit() }.take(2)
                        dueDayText = filtered
                        dayError = false
                    },
                    label = { Text("Día de vencimiento (1-31)") },
                    isError = dayError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isCreating,
                    supportingText = if (dayError) ({ Text("Ingresa un día entre 1 y 31") }) else null
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating
                )

                if (createError != null) {
                    Text(createError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (isCreating) {
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardando...")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val day = dueDayText.toIntOrNull()
                    nameError = name.isBlank()
                    dayError  = day == null || day < 1 || day > 31
                    if (!nameError && !dayError) {
                        onCreate(
                            name.trim(),
                            amountText.toLongOrNull() ?: 0L,
                            day!!,
                            notes.trim().ifBlank { null }
                        )
                    }
                },
                enabled = !isCreating
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancelar") }
        }
    )
}
