package com.nexohogar.presentation.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexohogar.domain.model.AccountBalance
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
                    onDeleteClick    = { accountId -> viewModel.showDeleteConfirm(accountId) }
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
}

// ---------------------------------------------------------------------------
// Diálogo de creación de cuenta
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
// ---------------------------------------------------------------------------

@Composable
fun AccountsList(
    sharedAccounts  : List<AccountBalance>,
    personalAccounts: List<AccountBalance>,
    onDeleteClick   : (String) -> Unit
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
                    AccountItem(account = account, onDeleteClick = onDeleteClick)
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
                    AccountItem(account = account, onDeleteClick = onDeleteClick)
                }
            }
        }
    }
}

@Composable
fun AccountItem(
    account     : AccountBalance,
    onDeleteClick: (String) -> Unit
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
        modifier  = Modifier.fillMaxWidth(),
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
