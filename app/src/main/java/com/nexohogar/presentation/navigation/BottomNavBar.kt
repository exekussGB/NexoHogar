package com.nexohogar.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom Navigation Bar con botón "+" central integrado y badges por módulo.
 *
 * Items: Resumen | Cuentas | (+) Agregar | Inventario | Más
 *
 * - El "+" abre el diálogo de tipo de movimiento (Ingreso / Gasto / Transferencia).
 * - "Inventario" muestra badge rojo con `lowStockCount` (productos bajo mínimo).
 * - "Más" muestra badge rojo con `hubTotalAlerts` (suma de todas las alertas).
 * - Solo se muestra DESPUÉS de que el usuario selecciona un módulo desde el Hub.
 */

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val isAction: Boolean = false
) {
    object Dashboard : BottomNavItem("dashboard",  "Resumen",    Icons.Default.Home)
    object Accounts  : BottomNavItem("accounts",   "Cuentas",    Icons.Default.AccountBalance)
    object AddMove   : BottomNavItem("add_action", "Agregar",    Icons.Default.Add, isAction = true)
    object Inventory : BottomNavItem("inventory",  "Inventario", Icons.Default.Inventory2)
    object More      : BottomNavItem("hub",        "Más",        Icons.Default.MoreHoriz)
}

@Composable
fun NexoHogarBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAddMovement: () -> Unit,
    onShowTransactionTypeDialog: () -> Unit,  // reservado para long-press futuro
    lowStockCount: Int = 0,                   // badge en Inventario
    hubTotalAlerts: Int = 0                   // badge en Más (suma global)
) {
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Accounts,
        BottomNavItem.AddMove,
        BottomNavItem.Inventory,
        BottomNavItem.More
    )

    NavigationBar {
        items.forEach { item ->
            when {
                item.isAction -> {
                    // ── Botón central "+" ─────────────────────────────────────
                    NavigationBarItem(
                        selected = false,
                        onClick  = onAddMovement,
                        icon     = {
                            Icon(
                                imageVector        = item.icon,
                                contentDescription = item.title,
                                modifier           = Modifier.size(28.dp),
                                tint               = MaterialTheme.colorScheme.primary
                            )
                        },
                        label = { Text(item.title) }
                    )
                }
                item == BottomNavItem.Inventory && lowStockCount > 0 -> {
                    // ── Inventario con badge de stock bajo ────────────────────
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick  = { onNavigate(item.route) },
                        icon     = {
                            BadgedBox(badge = {
                                Badge { Text(lowStockCount.toString(), fontSize = 10.sp) }
                            }) {
                                Icon(item.icon, contentDescription = item.title)
                            }
                        },
                        label = { Text(item.title) }
                    )
                }
                item == BottomNavItem.More && hubTotalAlerts > 0 -> {
                    // ── Más con badge de alertas globales ─────────────────────
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick  = { onNavigate(item.route) },
                        icon     = {
                            BadgedBox(badge = {
                                Badge { Text(hubTotalAlerts.toString(), fontSize = 10.sp) }
                            }) {
                                Icon(item.icon, contentDescription = item.title)
                            }
                        },
                        label = { Text(item.title) }
                    )
                }
                else -> {
                    // ── Ítems normales sin badge ──────────────────────────────
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick  = { onNavigate(item.route) },
                        icon     = { Icon(item.icon, contentDescription = item.title) },
                        label    = { Text(item.title) }
                    )
                }
            }
        }
    }
}
