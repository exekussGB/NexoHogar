package com.nexohogar.presentation.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.nexohogar.domain.model.AccountBalance
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clpFormat = remember { NumberFormat.getIntegerInstance(Locale("es", "CL")) }

    // Error snackbar
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuentas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Filled.Add, contentDescription = "Crear cuenta")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Cuentas compartidas ──
                    item {
                        Text(
                            text = "Cuentas Compartidas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (uiState.sharedAccounts.isEmpty()) {
                        item {
                            Text(
                                "No hay cuentas compartidas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(uiState.sharedAccounts, key = { it.accountId }) { account ->
                            AccountCard(
                                account = account,
                                canDelete = true, // Shared accounts: any household member can delete
                                onDelete = { viewModel.showDeleteConfirm(account.accountId) },
                                clpFormat = clpFormat
                            )
                        }
                    }

                    // ── Cuentas personales ──
                    if (uiState.personalAccounts.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Mis Cuentas Personales",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(uiState.personalAccounts, key = { it.accountId }) { account ->
                            AccountCard(
                                account = account,
                                canDelete = account.ownerUserId == uiState.currentUserId,
                                onDelete = { viewModel.showDeleteConfirm(account.accountId) },
                                clpFormat = clpFormat
                            )
                        }
                    }
                }
            }

            // Error message
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(uiState.error ?: "")
                }
            }
        }
    }

    // ── Dialog: Crear cuenta ─────────────────────────────────────────────────
    if (uiState.showCreateDialog) {
        CreateAccountDialog(
            name         = uiState.newAccountName,
            subtype      = uiState.newAccountSubtype,
            isShared     = uiState.newAccountIsShared,
            isCreating   = uiState.isCreating,
            onNameChange = viewModel::onNameChange,
            onSubtypeChange = viewModel::onSubtypeChange,
            onIsSharedChange = viewModel::onIsSharedChange,
            onConfirm    = viewModel::createAccount,
            onDismiss    = viewModel::dismissCreateDialog
        )
    }

    // ── Dialog: Confirmar eliminación ────────────────────────────────────────
    if (uiState.showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = { Text("Eliminar cuenta") },
            text = { Text("¿Estás seguro de que quieres eliminar esta cuenta? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteAccount,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirm) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun AccountCard(
    account: AccountBalance,
    canDelete: Boolean,
    onDelete: () -> Unit,
    clpFormat: NumberFormat
) {
    val subtypeIcon = when (account.accountType) {
        "LIABILITY" -> Icons.Filled.CreditCard
        else -> Icons.Filled.AccountBalance
    }
    val balanceColor = when {
        account.movementBalance > 0 -> Color(0xFF2E7D32)
        account.movementBalance < 0 -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = subtypeIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = account.accountName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = account.accountType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                text = "$${clpFormat.format(account.movementBalance)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = balanceColor
            )
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAccountDialog(
    name: String,
    subtype: String,
    isShared: Boolean,
    isCreating: Boolean,
    onNameChange: (String) -> Unit,
    onSubtypeChange: (String) -> Unit,
    onIsSharedChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val subtypes = listOf(
        "cash" to "Efectivo",
        "debit" to "Cuenta Débito",
        "credit_card" to "Tarjeta de Crédito",
        "savings" to "Cuenta de Ahorro",
        "other" to "Otra"
    )

    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Nueva Cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        onNameChange(it)
                        nameError = if (it.trim().isBlank()) "El nombre es obligatorio" else null
                    },
                    label = { Text("Nombre de la cuenta") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tipo de cuenta", style = MaterialTheme.typography.labelMedium)
                Column {
                    subtypes.forEach { (value, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = subtype == value,
                                onClick = { onSubtypeChange(value) }
                            )
                            Text(label, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isShared) "Cuenta Compartida" else "Cuenta Personal",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isShared,
                        onCheckedChange = onIsSharedChange
                    )
                }
                Text(
                    text = if (isShared) "Todos los miembros del hogar pueden usarla"
                    else "Solo tú puedes ver y usar esta cuenta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isCreating && name.trim().isNotBlank()
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Crear")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text("Cancelar")
            }
        }
    )
}
