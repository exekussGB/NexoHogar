package com.nexohogar.presentation.navigation

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Bottom Navigation Bar con FAB central.
 * Referencia: nexohogar_report.md — Sección 1.1
 * Referencia: nexohogar_ux_analysis.md — Prioridad Alta
 *
 * Items: Resumen, Movimientos, (+) FAB, Inventario, Más
 *
 * El FAB central:
 * - Toque corto → abre directamente el formulario de Gasto
 * - Toque largo → muestra dialog con Ingreso / Gasto / Transferencia
 *
 * TODO: Implementar la integración completa con NavGraph.kt
 */

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : BottomNavItem("dashboard", "Resumen", Icons.Default.Home)
    object Transactions : BottomNavItem("transactions", "Movim.", Icons.Default.List)
    object Inventory : BottomNavItem("inventory", "Inventario", Icons.Default.Inventory2)
    object More : BottomNavItem("more", "Más", Icons.Default.MoreHoriz)
}

@Composable
fun NexoHogarBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAddExpense: () -> Unit,
    onShowTransactionTypeDialog: () -> Unit
) {
    NavigationBar {
        val items = listOf(
            BottomNavItem.Dashboard,
            BottomNavItem.Transactions,
            // FAB ocupa el espacio central (ver Scaffold)
            BottomNavItem.Inventory,
            BottomNavItem.More
        )

        items.forEachIndexed { index, item ->
            // Insertar espacio para el FAB central
            if (index == 2) {
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(0.dp)) },
                    label = { Text("") },
                    enabled = false
                )
            }

            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) }
            )
        }
    }
}
