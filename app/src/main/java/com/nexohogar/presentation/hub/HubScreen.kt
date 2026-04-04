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

private data class HubAction(
    val title          : String,
    val subtitle       : String,
    val icon           : ImageVector,
    val backgroundColor: Color,
    val iconColor      : Color,
    val enabled        : Boolean = true,
    val onClick        : () -> Unit
)

// ─────────────────────────────────────────────────────────────────────────────
// HubScreen — "Más opciones"
// Items now accessible from BottomBar (Dashboard, Transactions, Inventory)
// have been removed. Remaining items:
//   Presupuesto  | Cuentas Recurrentes
//   Cuentas      | Lista de Deseos
//   Invitar      | Opciones
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
    onNavigateToWishlist      : () -> Unit
) {
    // Simplified grid — items already in BottomBar are removed
    val actions = listOf(
        HubAction(
            title           = "Presupuesto",
            subtitle        = "Control de gastos",
            icon            = Icons.Default.AccountBalanceWallet,
            backgroundColor = Color(0xFFE8EAF6),
            iconColor       = Color(0xFF283593),
            onClick         = onNavigateToBudget
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
            text       = "Más opciones",
            style      = MaterialTheme.typography.headlineMedium,
            color      = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text       = if (householdName.isNotBlank()) "Hogar: $householdName" else "Herramientas adicionales",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(28.dp))

        actions.chunked(2).forEach { rowActions ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowActions.forEach { action ->
                    HubActionCard(action = action, modifier = Modifier.weight(1f))
                }
                if (rowActions.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tarjeta de acción
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HubActionCard(action: HubAction, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier
            .height(130.dp)
            .alpha(if (action.enabled) 1f else 0.45f)
            .clickable(enabled = action.enabled) { action.onClick() },
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (action.enabled) 2.dp else 0.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
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
