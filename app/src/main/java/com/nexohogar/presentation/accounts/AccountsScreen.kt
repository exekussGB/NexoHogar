package com.nexohogar.presentation.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
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
fun AccountsScreen(viewModel: AccountsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Cuentas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowCreateDialog() }) {
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
                else -> AccountsList(uiState.accounts)
            }
        }
    }

    if (uiState.showCreateDialog) {
        CreateAccountDialog(
            isCreating  = uiState.isCreating,
            createError = uiState.createError,
            onDismiss   = { viewModel.onDismissCreateDialog() },
            onCreate    = { name, type, subtype -> viewModel.createAccount(name, type, subtype) }
        )
    }
}

// ---------------------------------------------------------------------------
// Diálogo de creación de cuenta
// ---------------------------------------------------------------------------

data class AccountTypeOption(
    val label: String,
    val accountType: String,
    val accountSubtype: String
)

/**
 * CORRECCIÓN: los valores account_type deben ser lowercase para coincidir
 * con los CHECK constraints / enum de la tabla accounts en Supabase.
 * (Antes eran "ASSET", "LIABILITY", etc. — eso causaba HTTP 400).
 */
val accountTypeOptions = listOf(
    AccountTypeOption("Billetera / Efectivo",  "asset",     "cash"),
    AccountTypeOption("Cuenta Bancaria",        "asset",     "bank"),
    AccountTypeOption("Tarjeta de Crédito",     "liability", "credit_card"),
    AccountTypeOption("Categoría de Gasto",     "expense",   "other"),
    AccountTypeOption("Fuente de Ingreso",      "income",    "other"),
    AccountTypeOption("Otro",                   "asset",     "other")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountDialog(
    isCreating: Boolean,
    createError: String?,
    onDismiss: () -> Unit,
    onCreate: (name: String, accountType: String, accountSubtype: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf(accountTypeOptions[0]) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Nueva cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Campo nombre
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Nombre de la cuenta") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    enabled       = !isCreating
                )

                // Dropdown tipo de cuenta
                ExposedDropdownMenuBox(
                    expanded          = dropdownExpanded,
                    onExpandedChange  = { if (!isCreating) dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = selectedOption.label,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Tipo de cuenta") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier      = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        enabled = !isCreating
                    )
                    ExposedDropdownMenu(
                        expanded          = dropdownExpanded,
                        onDismissRequest  = { dropdownExpanded = false }
                    ) {
                        accountTypeOptions.forEach { option ->
                            DropdownMenuItem(
                                text    = { Text(option.label) },
                                onClick = {
                                    selectedOption = option
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Mensaje de error
                if (createError != null) {
                    Text(
                        text  = createError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Spinner mientras crea
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
                onClick  = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), selectedOption.accountType, selectedOption.accountSubtype)
                    }
                },
                enabled = !isCreating && name.isNotBlank()
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text("Cancelar")
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Lista de cuentas
// ---------------------------------------------------------------------------

@Composable
fun AccountsList(accounts: List<AccountBalance>) {
    if (accounts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tienes cuentas registradas.")
        }
    } else {
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts) { account ->
                AccountItem(account)
            }
        }
    }
}

@Composable
fun AccountItem(account: AccountBalance) {
    val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    val (icon, iconColor) = when (account.accountType.uppercase()) {
        "ASSET"     -> Icons.Default.Savings              to Color(0xFF1565C0)
        "LIABILITY" -> Icons.Default.CreditCard           to Color(0xFFC62828)
        "INCOME"    -> Icons.Default.TrendingUp           to Color(0xFF2E7D32)
        "EXPENSE"   -> Icons.Default.TrendingDown         to Color(0xFFE65100)
        else        -> Icons.Default.AccountBalanceWallet  to MaterialTheme.colorScheme.primary
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconColor,
                modifier           = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = account.accountName,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = account.accountType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text       = clpFormat.format(account.movementBalance),
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = if (account.movementBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
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
