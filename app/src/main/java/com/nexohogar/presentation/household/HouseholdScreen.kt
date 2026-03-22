package com.nexohogar.presentation.household

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.nexohogar.domain.model.Household
import com.nexohogar.presentation.components.LoadingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdScreen(
    viewModel: HouseholdViewModel,
    onHouseholdSelected: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    // Navegar al dashboard cuando se crea exitosamente
    LaunchedEffect(uiState.createSuccess) {
        if (uiState.createSuccess) {
            viewModel.clearCreateSuccess()
            val newHousehold = uiState.households.lastOrNull()
            if (newHousehold != null) {
                onHouseholdSelected(newHousehold.id)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Hogares") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // FAB secundario: unirse con código
                SmallFloatingActionButton(
                    onClick           = { viewModel.onShowJoinDialog() },
                    containerColor    = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor      = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = "Unirse con código")
                }
                // FAB principal: crear hogar
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Crear Hogar")
                }
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

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadHouseholds() }
                    )
                }

                uiState.households.isEmpty() -> {
                    EmptyHouseholdState(
                        onCreateClick = { showCreateDialog = true },
                        onJoinClick   = { viewModel.onShowJoinDialog() }
                    )
                }

                else -> {
                    HouseholdList(
                        households       = uiState.households,
                        onHouseholdClick = { onHouseholdSelected(it.id) }
                    )
                }
            }
        }
    }

    // ── Diálogo de creación ──────────────────────────────────────────────────
    if (showCreateDialog) {
        CreateHouseholdDialog(
            isCreating   = uiState.isCreating,
            errorMessage = uiState.createError,
            onConfirm    = { name ->
                viewModel.createHousehold(name)
            },
            onDismiss    = {
                showCreateDialog = false
                viewModel.clearCreateError()
            }
        )
    }

    // ── Diálogo de unión con código ──────────────────────────────────────────
    if (uiState.showJoinDialog) {
        JoinHouseholdDialog(
            code       = uiState.joinCode,
            isJoining  = uiState.isJoining,
            errorMessage = uiState.joinError,
            onCodeChange = { viewModel.onJoinCodeChange(it) },
            onConfirm    = { viewModel.joinHousehold() },
            onDismiss    = { viewModel.onDismissJoinDialog() }
        )
    }
}

// ---------------------------------------------------------------------------
// Lista de hogares
// ---------------------------------------------------------------------------

@Composable
fun HouseholdList(
    households: List<Household>,
    onHouseholdClick: (Household) -> Unit
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(households) { household ->
            HouseholdItem(household, onHouseholdClick)
        }
    }
}

@Composable
fun HouseholdItem(
    household: Household,
    onClick: (Household) -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onClick(household) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.Home,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text       = household.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                household.description?.let { desc ->
                    if (desc.isNotBlank()) {
                        Text(
                            text  = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Estado vacío
// ---------------------------------------------------------------------------

@Composable
fun EmptyHouseholdState(
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    Column(
        modifier              = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector        = Icons.Default.Home,
            contentDescription = null,
            modifier           = Modifier.size(72.dp),
            tint               = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text  = "No tienes hogares aún",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text  = "Crea un nuevo hogar o únete a uno existente con un código de invitación",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick  = onCreateClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crear Hogar")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick  = onJoinClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.VpnKey, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("¿Tienes código de invitación?")
        }
    }
}

// ---------------------------------------------------------------------------
// Diálogos
// ---------------------------------------------------------------------------

@Composable
fun CreateHouseholdDialog(
    isCreating: Boolean,
    errorMessage: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var householdName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title   = { Text("Nuevo Hogar") },
        text    = {
            Column {
                OutlinedTextField(
                    value         = householdName,
                    onValueChange = { householdName = it },
                    label         = { Text("Nombre del hogar") },
                    singleLine    = true,
                    enabled       = !isCreating,
                    modifier      = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text  = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(householdName) },
                enabled  = !isCreating && householdName.isNotBlank()
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Crear")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun JoinHouseholdDialog(
    code: String,
    isJoining: Boolean,
    errorMessage: String?,
    onCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isJoining) onDismiss() },
        title   = { Text("Unirse a un hogar") },
        text    = {
            Column {
                Text(
                    text  = "Ingresa el código de invitación que te compartieron",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value         = code,
                    onValueChange = { onCodeChange(it.uppercase()) },
                    label         = { Text("Código de invitación") },
                    singleLine    = true,
                    enabled       = !isJoining,
                    modifier      = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    placeholder   = { Text("Ej: ABC12345") }
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text  = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = onConfirm,
                enabled  = !isJoining && code.isNotBlank()
            ) {
                if (isJoining) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Unirse")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isJoining
            ) {
                Text("Cancelar")
            }
        }
    )
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
