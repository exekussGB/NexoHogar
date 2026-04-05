package com.nexohogar.presentation.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Bottom Navigation Bar con botón "+" central integrado.
 *
 * Items: Resumen | Cuentas | (+) Agregar | Inventario | Más
 *
 * - El "+" es un ítem de acción que abre el diálogo de tipo de movimiento
 *   (Ingreso / Gasto / Transferencia).
 * - "Más" SIEMPRE navega al Hub (pantalla principal con todos los módulos).
 * - "Cuentas" reemplaza a "Movimientos" para mayor utilidad.
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
    onShowTransactionTypeDialog: () -> Unit   // reservado para long-press futuro
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
            if (item.isAction) {
                // ── Botón central "+" — abre diálogo de tipo de movimiento ──
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
            } else {
                // ── Ítems de navegación normales ─────────────────────────────
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
