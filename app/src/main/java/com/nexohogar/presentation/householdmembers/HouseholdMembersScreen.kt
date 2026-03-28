package com.nexohogar.presentation.householdmembers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexohogar.domain.model.HouseholdMember
import com.nexohogar.presentation.components.LoadingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdMembersScreen(
    viewModel: HouseholdMembersViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Snackbar para mensajes de acción
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Miembros del Hogar") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> LoadingOverlay()

                uiState.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadMembers() }) {
                            Text("Reintentar")
                        }
                    }
                }

                uiState.members.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No hay miembros en este hogar.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                else -> {
                    // Separar miembros activos y pendientes
                    val active  = uiState.members.filter { it.role.lowercase() != "pending" }
                    val pending = uiState.members.filter { it.role.lowercase() == "pending" }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Contador
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${active.size} miembro${if (active.size != 1) "s" else ""}${
                                        if (pending.isNotEmpty()) " · ${pending.size} pendiente${if (pending.size != 1) "s" else ""}" else ""
                                    }",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Solicitudes pendientes primero
                            if (pending.isNotEmpty()) {
                                item {
                                    Text(
                                        "⏳ Solicitudes pendientes",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE65100),
                                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                    )
                                }
                                items(pending) { member ->
                                    PendingMemberCard(
                                        member = member,
                                        isProcessing = uiState.isProcessing,
                                        onAccept = { viewModel.acceptMember(member.userId) },
                                        onReject = { viewModel.rejectMember(member.userId) }
                                    )
                                }
                            }

                            if (active.isNotEmpty()) {
                                item {
                                    Text(
                                        "Miembros activos",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }
                                items(active) { member ->
                                    MemberCard(member)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Card para miembro pendiente con botones Accept/Reject ────────────────

@Composable
private fun PendingMemberCard(
    member: HouseholdMember,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.label(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Solicitud pendiente",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                }
            }

            // Botones de acción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rechazar")
                }

                Button(
                    onClick = onAccept,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aceptar")
                    }
                }
            }
        }
    }
}

// ── Card para miembro activo ─────────────────────────────────────────────

@Composable
private fun MemberCard(member: HouseholdMember) {
    val isAdmin = member.role.lowercase() == "admin"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (isAdmin) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isAdmin) Icons.Default.Star else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isAdmin) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.label(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (member.joinedAt != null) {
                    Text(
                        text = "Se unió: ${member.joinedAt.take(10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isAdmin) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
            ) {
                Text(
                    text = if (isAdmin) "Admin" else "Miembro",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isAdmin) Color(0xFFE65100) else Color(0xFF2E7D32)
                )
            }
        }
    }
}
