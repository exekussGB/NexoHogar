package com.nexohogar.presentation.recurringbills

import androidx.compose.animation.core.animateFloatAsState
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
import com.nexohogar.data.remote.dto.RecurringBillPaymentDto
import com.nexohogar.data.remote.dto.RecurringBillWithStatusDto
import com.nexohogar.data.remote.dto.RecurringSummaryDto
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.RecurringBill
import com.nexohogar.domain.model.RecurringBillStatus
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.platform.testTag
import com.nexohogar.core.tutorial.TutorialManager
import com.nexohogar.core.tutorial.TutorialModule
import com.nexohogar.presentation.tutorial.TutorialOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillsScreen(
    viewModel: RecurringBillsViewModel,
    tutorialManager: TutorialManager,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTutorial by remember {
        mutableStateOf(!tutorialManager.isTutorialCompleted(TutorialModule.RECURRING_BILLS))
    }
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
            FloatingActionButton(
                onClick = { viewModel.onShowCreateDialog() },
                modifier = Modifier.testTag("recurring_add_button")
            ) {
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

                uiState.bills.isEmpty() && uiState.billsWithStatus.isEmpty() ->
                    EmptyBillsState { viewModel.onShowCreateDialog() }

                else -> {
                    val active   = uiState.bills.filter { it.isActive }
                    val inactive = uiState.bills.filter { !it.isActive }
                    val statusMap = uiState.billsWithStatus.associateBy { it.id }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("recurring_list"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Resumen mensual
                        uiState.summary?.let { summary ->
                            item { SummaryCard(summary, clpFormat) }
                        }

                        // Alertas
                        val alertCount = active.count {
                            val st = it.status()
                            st == RecurringBillStatus.OVERDUE || st == RecurringBillStatus.DUE_SOON
                        }
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
                                    statusDto = statusMap[bill.id],
                                    format = clpFormat,
                                    onMarkAsPaid   = {
                                        statusMap[bill.id]?.let { viewModel.showPayDialog(it) }
                                            ?: viewModel.confirmMarkAsPaid(bill)
                                    },
                                    onToggleActive = { viewModel.toggleActive(bill) },
                                    onDelete       = { viewModel.deleteBill(bill) },
                                    onShowHistory  = { statusMap[bill.id]?.let { viewModel.showHistory(it) } }
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
                                    statusDto = statusMap[bill.id],
                                    format = clpFormat,
                                    onMarkAsPaid   = {},
                                    onToggleActive = { viewModel.toggleActive(bill) },
                                    onDelete       = { viewModel.deleteBill(bill) },
                                    onShowHistory  = { statusMap[bill.id]?.let { viewModel.showHistory(it) } }
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
            onCreate    = { name, amount, day, notes, totalInst, paidInst ->
                viewModel.createBill(name, amount, day, notes, totalInst, paidInst)
            }
        )
    }

    // ── Popup de pago mejorado ──────────────────────────────────────────────
    val billToPay = uiState.billToPay
    if (billToPay != null) {
        PayBillDialog(
            bill       = billToPay,
            accounts   = uiState.accounts,
            isPaying   = uiState.isPayingBill,
            onDismiss  = { viewModel.dismissPayDialog() },
            onConfirm  = { amount, accountId, notes ->
                viewModel.payBill(amount, accountId, notes)
            }
        )
    }

    // ── Historial de pagos ──────────────────────────────────────────────────
    val historyBill = uiState.showHistoryFor
    if (historyBill != null) {
        PaymentHistoryDialog(
            billName   = historyBill.name,
            payments   = uiState.paymentHistory,
            isLoading  = uiState.isLoadingHistory,
            format     = NumberFormat.getCurrencyInstance(Locale("es", "CL")),
            onDismiss  = { viewModel.dismissHistory() }
        )
    }
    // ── Tutorial overlay ────────────────────────────────────────────────────
    if (showTutorial) {
        TutorialOverlay(
            module = TutorialModule.RECURRING_BILLS,
            onComplete = {
                tutorialManager.markTutorialCompleted(TutorialModule.RECURRING_BILLS)
                showTutorial = false
            },
            onSkip = {
                tutorialManager.markTutorialCompleted(TutorialModule.RECURRING_BILLS)
                showTutorial = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Resumen mensual
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(summary: RecurringSummaryDto, format: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Resumen del mes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem("Total", format.format(summary.totalMonthlyClp), MaterialTheme.colorScheme.onPrimaryContainer)
                SummaryItem("Pagado", format.format(summary.paidAmountClp), Color(0xFF2E7D32))
                SummaryItem("Pendiente", format.format(summary.pendingAmountClp), Color(0xFFE65100))
            }
            if (summary.overdueCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "⚠️ ${summary.overdueCount} vencida${if (summary.overdueCount > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB71C1C),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Popup de pago con monto editable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayBillDialog(
    bill: RecurringBillWithStatusDto,
    accounts: List<Account>,
    isPaying: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, accountId: String?, notes: String?) -> Unit
) {
    var amount by remember { mutableStateOf(bill.amountClp.toString()) }
    var selectedAccountId by remember { mutableStateOf(bill.accountId) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isPaying) onDismiss() },
        icon = { Icon(Icons.Default.Payment, contentDescription = null, tint = Color(0xFF2E7D32)) },
        title = { Text("Pagar ${bill.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ══ Info de cuotas ══
                if (bill.isInstallment) {
                    val nextPaid = bill.paidInstallments + 1
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1565C0).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "📋 Cuota $nextPaid de ${bill.totalInstallments}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val progress = nextPaid.toFloat() / (bill.totalInstallments ?: 1).toFloat()
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = Color(0xFF1565C0),
                                trackColor = Color(0xFF1565C0).copy(alpha = 0.2f)
                            )
                            if (nextPaid >= (bill.totalInstallments ?: 0)) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "🎉 ¡Esta es la última cuota!",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                // Monto editable
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("Monto (CLP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("$") },
                    singleLine = true,
                    enabled = !isPaying
                )

                // Selector de cuenta de pago
                if (accounts.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { if (!isPaying) expanded = it }
                    ) {
                        OutlinedTextField(
                            value = accounts.find { it.id == selectedAccountId }?.name ?: "Seleccionar cuenta",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cuenta de pago") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name) },
                                    onClick = {
                                        selectedAccountId = account.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Notas opcionales
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    enabled = !isPaying
                )

                // Info de vencimiento
                if (bill.isOverdue) {
                    Text(
                        "⚠️ Vencida (día ${bill.dueDay})",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (bill.daysUntilDue != null) {
                    Text(
                        "📅 Vence en ${bill.daysUntilDue} días",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amount.toLongOrNull()
                    if (parsedAmount != null && parsedAmount > 0) {
                        onConfirm(
                            parsedAmount,
                            selectedAccountId,
                            notes.ifBlank { null }
                        )
                    }
                },
                enabled = !isPaying && (amount.toLongOrNull()?.let { it > 0 } ?: false)
            ) {
                if (isPaying) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("✅ Confirmar Pago")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isPaying) {
                Text("Cancelar")
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Historial de pagos
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PaymentHistoryDialog(
    billName: String,
    payments: List<RecurringBillPaymentDto>,
    isLoading: Boolean,
    format: NumberFormat,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historial: $billName") },
        text = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (payments.isEmpty()) {
                Text("No hay pagos registrados", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(payments) { payment ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        payment.paidAt.take(10),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (!payment.notes.isNullOrBlank()) {
                                        Text(
                                            payment.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    format.format(payment.amountClp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Item de cuenta recurrente (mejorado con cuotas)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecurringBillItem(
    bill: RecurringBill,
    statusDto: RecurringBillWithStatusDto?,
    format: NumberFormat,
    onMarkAsPaid: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    onShowHistory: () -> Unit
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
                        // Mostrar último pago si existe
                        statusDto?.lastPaymentAmount?.let { lastAmt ->
                            if (statusDto.isPaidThisMonth) {
                                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "Pagado: ${format.format(lastAmt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    // ══ Barra de progreso de cuotas ══
                    if (bill.isInstallment) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = bill.installmentLabel ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            val animatedProgress by animateFloatAsState(
                                targetValue = bill.installmentProgress,
                                label = "installment_progress"
                            )
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp),
                                color = if (bill.installmentProgress >= 1f) Color(0xFF2E7D32) else Color(0xFF1565C0),
                                trackColor = Color(0xFF1565C0).copy(alpha = 0.15f)
                            )
                            if (bill.installmentProgress >= 1f) {
                                Text(
                                    "✅",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        if (bill.isActive && status != RecurringBillStatus.PAID) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (bill.isInstallment) "Pagar cuota"
                                        else "Pagar"
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Payment, contentDescription = null, tint = Color(0xFF2E7D32)) },
                                onClick = { expanded = false; onMarkAsPaid() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Ver historial") },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                            onClick = { expanded = false; onShowHistory() }
                        )
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
// Diálogo de creación (con soporte de cuotas)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CreateBillDialog(
    isCreating: Boolean,
    createError: String?,
    onDismiss: () -> Unit,
    onCreate: (name: String, amountClp: Long, dueDayOfMonth: Int, notes: String?, totalInstallments: Int?, paidInstallments: Int) -> Unit
) {
    var name        by remember { mutableStateOf("") }
    var amountText  by remember { mutableStateOf("") }
    var dueDayText  by remember { mutableStateOf("") }
    var notes       by remember { mutableStateOf("") }
    var nameError   by remember { mutableStateOf(false) }
    var dayError    by remember { mutableStateOf(false) }

    // ── Cuotas ──
    var isInstallment       by remember { mutableStateOf(false) }
    var totalInstText       by remember { mutableStateOf("") }
    var paidInstText        by remember { mutableStateOf("0") }
    var totalInstError      by remember { mutableStateOf(false) }
    var paidInstError       by remember { mutableStateOf(false) }

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

                // ══ Toggle de cuotas ══
                Divider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "¿Es en cuotas?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Ej: TV en 12 cuotas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isInstallment,
                        onCheckedChange = {
                            isInstallment = it
                            if (!it) {
                                totalInstText = ""
                                paidInstText = "0"
                                totalInstError = false
                                paidInstError = false
                            }
                        },
                        enabled = !isCreating
                    )
                }

                if (isInstallment) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = totalInstText,
                            onValueChange = {
                                totalInstText = it.filter { c -> c.isDigit() }.take(3)
                                totalInstError = false
                            },
                            label = { Text("Total cuotas") },
                            placeholder = { Text("12") },
                            isError = totalInstError,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !isCreating
                        )
                        OutlinedTextField(
                            value = paidInstText,
                            onValueChange = {
                                paidInstText = it.filter { c -> c.isDigit() }.take(3)
                                paidInstError = false
                            },
                            label = { Text("Ya pagadas") },
                            placeholder = { Text("0") },
                            isError = paidInstError,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !isCreating
                        )
                    }

                    // Preview de progreso
                    val total = totalInstText.toIntOrNull() ?: 0
                    val paid  = paidInstText.toIntOrNull() ?: 0
                    if (total > 0) {
                        val progress = paid.toFloat() / total.toFloat()
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1565C0).copy(alpha = 0.08f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "Cuota ${paid}/$total",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1565C0)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(5.dp),
                                    color = if (progress >= 1f) Color(0xFF2E7D32) else Color(0xFF1565C0),
                                    trackColor = Color(0xFF1565C0).copy(alpha = 0.2f)
                                )
                                if (paid > 0 && total > paid) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Quedan ${total - paid} cuota${if (total - paid > 1) "s" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (totalInstError) {
                        Text(
                            "Ingresa el total de cuotas (mínimo 2)",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (paidInstError) {
                        Text(
                            "Las cuotas pagadas no pueden superar el total",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

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

                    // Validar cuotas
                    var installmentsValid = true
                    if (isInstallment) {
                        val totalInst = totalInstText.toIntOrNull()
                        val paidInst  = paidInstText.toIntOrNull() ?: 0
                        totalInstError = totalInst == null || totalInst < 2
                        paidInstError  = paidInst < 0 || (totalInst != null && paidInst >= totalInst)
                        installmentsValid = !totalInstError && !paidInstError
                    }

                    if (!nameError && !dayError && installmentsValid) {
                        val totalInst = if (isInstallment) totalInstText.toIntOrNull() else null
                        val paidInst  = if (isInstallment) (paidInstText.toIntOrNull() ?: 0) else 0
                        onCreate(
                            name.trim(),
                            amountText.toLongOrNull() ?: 0L,
                            day!!,
                            notes.trim().ifBlank { null },
                            totalInst,
                            paidInst
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
