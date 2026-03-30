package com.nexohogar.presentation.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.nexohogar.core.util.DateFormatter
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.model.Transaction
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel     : AccountsViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Cuentas") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar cuenta")
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
                uiState.error != null -> ErrorState(uiState.error!!) { viewModel.loadAccounts() }
                else -> AccountsList(
                    sharedAccounts   = uiState.sharedAccounts,
                    personalAccounts = uiState.personalAccounts,
                    onDeleteClick    = { accountId -> viewModel.showDeleteConfirm(accountId) },
                    // ✅ NUEVO: callback para click en cuenta
                    onAccountClick   = { account -> viewModel.selectAccount(account) }
                )
            }
        }
    }

    // ── Diálogo crear cuenta ───────────────────────────────────────────────
    if (uiState.showCreateDialog) {
        CreateAccountDialog(
            name        = uiState.newAccountName,
            subtype     = uiState.newAccountSubtype,
            isShared    = uiState.newAccountIsShared,
            isCreating  = uiState.isCreating,
            error       = uiState.error,
            onNameChange    = { viewModel.onNameChange(it) },
            onSubtypeChange = { viewModel.onSubtypeChange(it) },
            onIsSharedChange = { viewModel.onIsSharedChange(it) },
            onDismiss   = { viewModel.dismissCreateDialog() },
            onCreate    = { viewModel.createAccount() }
        )
    }

    // ── Confirmación de eliminación ────────────────────────────────────────
    if (uiState.showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = { Text("Eliminar cuenta") },
            text  = { Text("¿Estás seguro de que deseas eliminar esta cuenta? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteAccount() },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirm() }) { Text("Cancelar") }
            }
        )
    }

    // ✅ NUEVO: BottomSheet con detalle de cuenta y movimientos recientes
    uiState.selectedAccount?.let { account ->
        AccountDetailSheet(
            account = account,
            transactions = uiState.selectedAccountTransactions,
            isLoading = uiState.isLoadingTransactions,
            onDismiss = { viewModel.dismissAccountDetail() }
        )
    }
}

// ---------------------------------------------------------------------------
// ✅ NUEVO: BottomSheet con detalle de cuenta y últimos movimientos
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDetailSheet(
    account: AccountBalance,
    transactions: List<Transaction>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    val (icon, iconColor) = when (account.accountType.lowercase()) {
        "asset"     -> Icons.Default.Savings             to Color(0xFF1565C0)
        "liability" -> Icons.Default.CreditCard          to Color(0xFFC62828)
        "income"    -> Icons.Default.TrendingUp          to Color(0xFF2E7D32)
        "expense"   -> Icons.Default.TrendingDown        to Color(0xFFE65100)
        else        -> Icons.Default.AccountBalanceWallet to Color(0xFF424242)
    }

    val typeLabel = when (account.accountType.lowercase()) {
        "asset"     -> "Activo"
        "liability" -> "Pasivo / Deuda"
        "income"    -> "Fuente de ingresos"
        "expense"   -> "Categoría de gasto"
        else        -> account.accountType
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Cabecera de la cuenta ────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = account.accountName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = typeLabel,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Saldo ─────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = iconColor.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Saldo actual", fontSize = 13.sp, color = Color.Gray)
                    Text(
                        text = clpFormat.format(account.movementBalance),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = if (account.movementBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // ── Últimos movimientos ─────────────────────────────────────
            Text(
                "Últimos movimientos",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = iconColor)
                    }
                }
                transactions.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Sin movimientos registrados",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(transactions, key = { it.id }) { transaction ->
                            AccountTransactionRow(transaction = transaction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountTransactionRow(transaction: Transaction) {
    val isIncome = transaction.type.lowercase() == "income"
    val isTransfer = transaction.type.lowercase() == "transfer"
    val color = when {
        isIncome    -> Color(0xFF4CAF50)
        isTransfer  -> Color(0xFF2196F3)
        else        -> Color(0xFFF44336)
    }
    val icon = when {
        isIncome    -> Icons.Default.ArrowDownward
        isTransfer  -> Icons.Default.SwapHoriz
        else        -> Icons.Default.ArrowUpward
    }
    val typeLabel = when (transaction.type.lowercase()) {
        "income"    -> "Ingreso"
        "transfer"  -> "Transferencia"
        else        -> "Gasto"
    }

    val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description ?: typeLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = DateFormatter.formatForDisplay(transaction.transactionDate),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = clpFormat.format(transaction.amountClp),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = color
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Diálogo de creación de cuenta (sin cambios)
// ---------------------------------------------------------------------------

data class AccountTypeOption(
    val label         : String,
    val accountType   : String,
    val accountSubtype: String
)

val accountTypeOptions = listOf(
    AccountTypeOption("Billetera / Efectivo", "asset",     "cash"),
    AccountTypeOption("Cuenta Bancaria",       "asset",     "bank"),
    AccountTypeOption("Tarjeta de Crédito",    "liability", "credit_card"),
    AccountTypeOption("Categoría de Gasto",    "expense",   "other"),
    AccountTypeOption("Fuente de Ingreso",     "income",    "other"),
    AccountTypeOption("Otro",                  "asset",     "other")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountDialog(
    name            : String,
    subtype         : String,
    isShared        : Boolean,
    isCreating      : Boolean,
    error           : String?,
    onNameChange    : (String) -> Unit,
    onSubtypeChange : (String) -> Unit,
    onIsSharedChange: (Boolean) -> Unit,
    onDismiss       : () -> Unit,
    onCreate        : () -> Unit
) {
    val selectedOption = accountTypeOptions.firstOrNull { it.accountSubtype == subtype }
        ?: accountTypeOptions[0]
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Nueva cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Nombre
                OutlinedTextField(
                    value         = name,
                    onValueChange = onNameChange,
                    label         = { Text("Nombre de la cuenta") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    enabled       = !isCreating
                )

                // Tipo de cuenta
                ExposedDropdownMenuBox(
                    expanded      = dropdownExpanded,
                    onExpandedChange = { if (!isCreating) dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = selectedOption.label,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Tipo de cuenta") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier      = Modifier.menuAnchor().fillMaxWidth(),
                        enabled       = !isCreating
                    )
                    ExposedDropdownMenu(
                        expanded        = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        accountTypeOptions.forEach { option ->
                            DropdownMenuItem(
                                text    = { Text(option.label) },
                                onClick = {
                                    onSubtypeChange(option.accountSubtype)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Compartida / Personal
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = if (isShared) "Cuenta compartida" else "Cuenta personal",
                        modifier = Modifier.weight(1f),
                        style    = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked         = isShared,
                        onCheckedChange = onIsSharedChange,
                        enabled         = !isCreating
                    )
                }

                if (error != null) {
                    Text(
                        text  = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isCreating) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creando cuenta...")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCreate,
                enabled = !isCreating && name.isNotBlank()
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancelar") }
        }
    )
}

// ---------------------------------------------------------------------------
// Lista de cuentas
// ✅ MODIFICADO: Agregado onAccountClick
// ---------------------------------------------------------------------------

@Composable
fun AccountsList(
    sharedAccounts  : List<AccountBalance>,
    personalAccounts: List<AccountBalance>,
    onDeleteClick   : (String) -> Unit,
    onAccountClick  : (AccountBalance) -> Unit   // ✅ NUEVO
) {
    if (sharedAccounts.isEmpty() && personalAccounts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No tienes cuentas registradas.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Usa el botón + para agregar una.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sharedAccounts.isNotEmpty()) {
                item {
                    Text(
                        text  = "Cuentas compartidas",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(sharedAccounts) { account ->
                    AccountItem(
                        account = account,
                        onDeleteClick = onDeleteClick,
                        onAccountClick = onAccountClick   // ✅ NUEVO
                    )
                }
            }
            if (personalAccounts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text  = "Cuentas personales",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(personalAccounts) { account ->
                    AccountItem(
                        account = account,
                        onDeleteClick = onDeleteClick,
                        onAccountClick = onAccountClick   // ✅ NUEVO
                    )
                }
            }
        }
    }
}

@Composable
fun AccountItem(
    account       : AccountBalance,
    onDeleteClick : (String) -> Unit,
    onAccountClick: (AccountBalance) -> Unit   // ✅ NUEVO
) {
    val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    val (icon, iconColor) = when (account.accountType.lowercase()) {
        "asset"     -> Icons.Default.Savings             to Color(0xFF1565C0)
        "liability" -> Icons.Default.CreditCard          to Color(0xFFC62828)
        "income"    -> Icons.Default.TrendingUp          to Color(0xFF2E7D32)
        "expense"   -> Icons.Default.TrendingDown        to Color(0xFFE65100)
        else        -> Icons.Default.AccountBalanceWallet to MaterialTheme.colorScheme.primary
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            // ✅ NUEVO: Click handler para mostrar detalle
            .clickable { onAccountClick(account) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector      = icon,
                contentDescription = null,
                tint             = iconColor,
                modifier         = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = account.accountName,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = when (account.accountType.lowercase()) {
                        "asset"     -> "Activo"
                        "liability" -> "Pasivo / Deuda"
                        "income"    -> "Fuente de ingresos"
                        "expense"   -> "Categoría de gasto"
                        else        -> account.accountType
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text       = clpFormat.format(account.movementBalance),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = if (account.movementBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onDeleteClick(account.accountId) }) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint               = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
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
