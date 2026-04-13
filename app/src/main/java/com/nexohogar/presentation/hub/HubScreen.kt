package com.nexohogar.presentation.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class HubAction(
    val title          : String,
    val subtitle       : String,
    val icon           : ImageVector,
    val backgroundColor: Color,
    val iconColor      : Color,
    val enabled        : Boolean = true,
    val badge          : Int = 0,
    val onClick        : () -> Unit
)

// ─────────────────────────────────────────────────────────────────────────────
// HubScreen — Pantalla principal del hogar
// Muestra TODOS los módulos disponibles para el usuario.
// Título: "Bienvenido a [hogar]" + "¿Qué quieres hacer?"
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HubScreen(
    householdName             : String = "",
    onNavigateToDashboard     : () -> Unit = {},
    onNavigateToTransactions  : () -> Unit = {},
    onNavigateToAddMovement   : (String) -> Unit = {},
    onNavigateToAccounts      : () -> Unit,
    onNavigateToInviteMember  : () -> Unit,
    onNavigateToRecurringBills: () -> Unit,
    onNavigateToBudget        : () -> Unit,
    onNavigateToInventory     : () -> Unit = {},
    onNavigateToOptions       : () -> Unit,
    onNavigateToWishlist      : () -> Unit,
    onNavigateToMembership    : () -> Unit = {}, // ═══ NUEVO ═══
    overdueCount              : Int = 0,
    budgetAlertCount          : Int = 0,
    lowStockCount             : Int = 0,
    wishlistHighCount         : Int = 0,
    hubAlertCount             : Int = 0
) {
    // Todos los módulos de la app
    val actions = listOf(
        HubAction(
            title           = "Resumen",
            subtitle        = "Panel general",
            icon            = Icons.Default.Home,
            backgroundColor = Color(0xFFE3F2FD),
            iconColor       = Color(0xFF1565C0),
            onClick         = onNavigateToDashboard
        ),
        HubAction(
            title           = "Movimientos",
            subtitle        = "Ingresos y gastos",
            icon            = Icons.Default.List,
            backgroundColor = Color(0xFFE8F5E9),
            iconColor       = Color(0xFF2E7D32),
            onClick         = onNavigateToTransactions
        ),
        HubAction(
            title           = "Presupuesto",
            subtitle        = "Control de gastos",
            icon            = Icons.Default.AccountBalanceWallet,
            backgroundColor = Color(0xFFE8EAF6),
            iconColor       = Color(0xFF283593),
            badge           = budgetAlertCount,
            onClick         = onNavigateToBudget
        ),
        HubAction(
            title           = "Inventario",
            subtitle        = "Productos del hogar",
            icon            = Icons.Default.Inventory2,
            backgroundColor = Color(0xFFFFF3E0),
            iconColor       = Color(0xFFE65100),
            badge           = lowStockCount,
            onClick         = onNavigateToInventory
        ),
        HubAction(
            title           = "Recurrentes",
            subtitle        = "Servicios mensuales",
            icon            = Icons.Default.Repeat,
            backgroundColor = Color(0xFFFCE4EC),
            iconColor       = Color(0xFFC62828),
            badge           = overdueCount,
            onClick         = onNavigateToRecurringBills
        ),
        HubAction(
            title           = "Cuentas",
            subtitle        = "Ver saldos",
            icon            = Icons.Default.AccountBalance,
            backgroundColor = Color(0xFFF3E5F5),
            iconColor       = Color(0xFF6A1B9A),
            onClick         = onNavigateToAccounts
        ),
        HubAction(
            title           = "Lista de Deseos",
            subtitle        = "Compras pendientes",
            icon            = Icons.Default.Favorite,
            backgroundColor = Color(0xFFFCE4EC),
            iconColor       = Color(0xFFC62828),
            badge           = wishlistHighCount,
            onClick         = onNavigateToWishlist
        ),
        HubAction(
            title           = "Invitar",
            subtitle        = "Agregar miembro",
            icon            = Icons.Default.PersonAdd,
            backgroundColor = Color(0xFFE0F7FA),
            iconColor       = Color(0xFF00695C),
            onClick         = onNavigateToInviteMember
        ),
        HubAction(
            title           = "Opciones",
            subtitle        = "Configuración",
            icon            = Icons.Default.Settings,
            backgroundColor = Color(0xFFF5F5F5),
            iconColor       = Color(0xFF424242),
            badge           = hubAlertCount,
            onClick         = onNavigateToOptions
        )
    )

    // ── Dialog para agregar movimiento ──────────────────────────────────────
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddMovementDialog(
            onDismiss  = { showAddDialog = false },
            onSelect   = { type ->
                showAddDialog = false
                onNavigateToAddMovement(type)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(52.dp))

        // ── Título de bienvenida ────────────────────────────────────────────
        Text(
            text       = if (householdName.isNotBlank()) "Bienvenido a $householdName" else "Bienvenido",
            style      = MaterialTheme.typography.headlineMedium,
            color      = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text       = "¿Qué quieres hacer?",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Botón rápido: Agregar movimiento ────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAddDialog = true },
            shape    = RoundedCornerShape(20.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Add,
                        contentDescription = "Agregar",
                        tint               = MaterialTheme.colorScheme.onPrimary,
                        modifier           = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text       = "Agregar Movimiento",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 17.sp,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text     = "Ingreso, Gasto o Transferencia",
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Grid de módulos ─────────────────────────────────────────────────
        actions.chunked(2).forEach { rowActions ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowActions.forEach { action ->
                    HubActionCard(
                        action     = action,
                        modifier   = Modifier.weight(1f),
                        horizontal = rowActions.size == 1
                    )
                }
                // last odd item spans full width
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialog para seleccionar tipo de movimiento (Ingreso / Gasto / Transferencia)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AddMovementDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Agregar Movimiento", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "¿Qué tipo de movimiento quieres registrar?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Ingreso
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect("income") },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, "Ingreso", tint = Color(0xFF4CAF50))
                        Column {
                            Text("Ingreso", fontWeight = FontWeight.Bold)
                            Text("Dinero que entra", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
                // Gasto
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect("expense") },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, "Gasto", tint = Color(0xFFF44336))
                        Column {
                            Text("Gasto", fontWeight = FontWeight.Bold)
                            Text("Dinero que sale", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
                // Transferencia
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect("transfer") },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, "Transferencia", tint = Color(0xFF2196F3))
                        Column {
                            Text("Transferencia", fontWeight = FontWeight.Bold)
                            Text("Entre cuentas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Tarjeta de acción
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HubActionCard(
    action:     HubAction,
    modifier:   Modifier = Modifier,
    horizontal: Boolean  = false
) {
    Card(
        modifier  = modifier
            .then(if (horizontal) Modifier.height(72.dp) else Modifier.height(130.dp))
            .alpha(if (action.enabled) 1f else 0.45f)
            .clickable(enabled = action.enabled) { action.onClick() },
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (action.enabled) 2.dp else 0.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (horizontal) {
            // ── Layout horizontal: ícono | título + subtítulo ──────────────
            Row(
                modifier          = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (action.badge > 0) {
                            Badge {
                                Text(if (action.badge > 9) "9+" else action.badge.toString())
                            }
                        }
                    }
                ) {
                    Box(
                        modifier         = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(action.backgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = action.icon,
                            contentDescription = action.title,
                            tint               = action.iconColor,
                            modifier           = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text       = action.title,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    if (action.subtitle.isNotEmpty()) {
                        Text(
                            text     = action.subtitle,
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        } else {
            // ── Layout vertical original ───────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                BadgedBox(
                    badge = {
                        if (action.badge > 0) {
                            Badge {
                                Text(if (action.badge > 9) "9+" else action.badge.toString())
                            }
                        }
                    }
                ) {
                    Box(
                        modifier         = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(action.backgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = action.icon,
                            contentDescription = action.title,
                            tint               = action.iconColor,
                            modifier           = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text       = action.title,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text     = action.subtitle,
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}
