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
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading || uiState.isRemoving -> LoadingOverlay()

                uiState.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadMembers() }) { Text("Reintentar") }
                    }
                }

                uiState.members.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No hay miembros en este hogar.", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    "${uiState.members.size} miembro${if (uiState.members.size != 1) "s" else ""}",
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
                            items(uiState.members) { member ->
                                MemberCard(
                                    member = member,
                                    isCurrentUserAdmin = uiState.isCurrentUserAdmin,
                                    isCurrentUser = member.userId == uiState.currentUserId,
                                    onRemove = { viewModel.showRemoveConfirm(member) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Delete confirmation dialog ───────────────────────────────────────────
    uiState.memberToRemove?.let { member ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRemoveConfirm,
            title = { Text("Eliminar miembro") },
            text = {
                Text("¿Estás seguro de que quieres eliminar a ${member.label()} del hogar? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::removeMember,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRemoveConfirm) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun MemberCard(
    member: HouseholdMember,
    isCurrentUserAdmin: Boolean,
    isCurrentUser: Boolean,
    onRemove: () -> Unit
) {
    val isAdmin = member.role.lowercase() == "admin"

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isAdmin) Icons.Default.Star else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isAdmin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(member.label(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (member.joinedAt != null) {
                    Text("Se unió: ${member.joinedAt.take(10)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            // Botón eliminar: solo si el usuario actual es admin Y no es el mismo miembro Y el miembro no es admin
            if (isCurrentUserAdmin && !isCurrentUser && !isAdmin) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.RemoveCircleOutline,
                        contentDescription = "Eliminar miembro",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
