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
import androidx.compose.ui.window.Dialog

// ---------------------------------------------------------------------------
// Modelo interno para cada botón del hub
// ---------------------------------------------------------------------------
private data class HubAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val iconColor: Color,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

// ---------------------------------------------------------------------------
// HubScreen
// ---------------------------------------------------------------------------
@Composable
fun HubScreen(
    householdName: String = "",
    onNavigateToDashboard: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAddMovement: (String) -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToInviteMember: () -> Unit,
    onNavigateToRecurringBills: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToOptions: () -> Unit
) {
    var showAddTypeDialog by remember { mutableStateOf(false) }

    val actions = listOf(
        HubAction(
            title           = "Resumen",
            subtitle        = "Saldos y gráficos",
            icon            = Icons.Default.BarChart,
            backgroundColor = Color(0xFFE8F5E9),
            iconColor       = Color(0xFF2E7D32),
            onClick         = onNavigateToDashboard
        ),
        HubAction(
            title           = "Movimientos",
            subtitle        = "Historial completo",
            icon            = Icons.Default.Receipt,
            backgroundColor = Color(0xFFE3F2FD),
            iconColor       = Color(0xFF1565C0),
            onClick         = onNavigateToTransactions
        ),
        HubAction(
            title           = "Agregar",
            subtitle        = "Nuevo movimiento",
            icon            = Icons.Default.AddCircle,
            backgroundColor = Color(0xFFFFF8E1),
            iconColor       = Color(0xFFF57F17),
            onClick         = { showAddTypeDialog = true }
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
            title           = "Invitar",
            subtitle        = "Agregar miembro",
            icon            = Icons.Default.PersonAdd,
            backgroundColor = Color(0xFFE0F7FA),
            iconColor       = Color(0xFF00695C),
            onClick         = onNavigateToInviteMember
        ),
        HubAction(
            title           = "Recurrentes",
            subtitle        = "Servicios mensuales",
            icon            = Icons.Default.Repeat,
            backgroundColor = Color(0xFFFCE4EC),
            iconColor       = Color(0xFFC62828),
            onClick         = onNavigateToRecurringBills
        ),
        HubAction(
            title           = "Inventario",
            subtitle        = "Control de stock",
            icon            = Icons.Default.Inventory,
            backgroundColor = Color(0xFFE8EAF6),
            iconColor       = Color(0xFF283593),
            onClick         = onNavigateToInventory
        ),
        HubAction(
            title           = "Opciones",
            subtitle        = "Configuración",
            icon            = Icons.Default.Settings,
            backgroundColor = Color(0xFFF5F5F5),
            iconColor       = Color(0xFF424242),
            onClick         = onNavigateToOptions
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(52.dp))

        Text(
            text = if (householdName.isNotBlank()) householdName else "Mi hogar",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "¿Qué quieres hacer?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(28.dp))

        val rows = actions.chunked(2)
        rows.forEachIndexed { _, rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowActions.forEach { action ->
                    HubActionCard(action = action, modifier = Modifier.weight(1f))
                }
                if (rowActions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAddTypeDialog) {
        AddMovementTypeDialog(
            onDismiss    = { showAddTypeDialog = false },
            onTypeSelected = { type ->
                showAddTypeDialog = false
                onNavigateToAddMovement(type)
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Tarjeta de acción individual
// ---------------------------------------------------------------------------
@Composable
private fun HubActionCard(
    action: HubAction,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .alpha(if (action.enabled) 1f else 0.45f)
            .clickable(enabled = action.enabled) { action.onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (action.enabled) 2.dp else 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(action.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = action.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = action.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = action.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Diálogo: selector de tipo de movimiento
// ---------------------------------------------------------------------------
@Composable
private fun AddMovementTypeDialog(
    onDismiss: () -> Unit,
    onTypeSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "¿Qué quieres registrar?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(20.dp))

                MovementTypeOption(
                    icon = Icons.Default.ArrowDownward,
                    label = "Ingreso",
                    description = "Sueldo, venta u otro ingreso",
                    iconColor = Color(0xFF2E7D32),
                    bgColor = Color(0xFFE8F5E9),
                    onClick = { onTypeSelected("income") }
                )
                Spacer(modifier = Modifier.height(10.dp))

                MovementTypeOption(
                    icon = Icons.Default.ArrowUpward,
                    label = "Gasto",
                    description = "Cuenta, compra u otro gasto",
                    iconColor = Color(0xFFC62828),
                    bgColor = Color(0xFFFFEBEE),
                    onClick = { onTypeSelected("expense") }
                )
                Spacer(modifier = Modifier.height(10.dp))

                MovementTypeOption(
                    icon = Icons.Default.SwapHoriz,
                    label = "Transferencia",
                    description = "Mover dinero entre cuentas",
                    iconColor = Color(0xFF1565C0),
                    bgColor = Color(0xFFE3F2FD),
                    onClick = { onTypeSelected("transfer") }
                )

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    }
}

@Composable
private fun MovementTypeOption(
    icon: ImageVector,
    label: String,
    description: String,
    iconColor: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
    }
}
