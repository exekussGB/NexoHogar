package com.nexohogar.presentation.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.domain.model.InventoryMovement
import com.nexohogar.domain.model.Product

// ─── Sheet: Historial de producto ────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductHistorySheet(
    product: Product,
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.movementsState.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Stock: ${String.format("%.2f", product.currentStock)} ${product.unit}",
                    fontSize = 14.sp, color = Color.Gray)
                if (!product.category.isNullOrBlank()) {
                    Surface(color = PrimaryBlue.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(product.category, fontSize = 8.sp, color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            when {
                state.isLoading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
                state.movements.isEmpty() -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("Sin movimientos registrados", color = Color.Gray)
                }
                else -> LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.movements, key = { it.id }) { movement ->
                        MovementRow(movement = movement, unit = product.unit)
                    }
                }
            }
        }
    }
}

@Composable
internal fun MovementRow(movement: InventoryMovement, unit: String) {
    val isIn = movement.movementType == "in"
    val color = if (isIn) GreenIn else RedOut
    val sign = if (isIn) "+" else "-"
    val label = if (isIn) "Compra" else "Consumo"

    Row(
        Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (isIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
            contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, color = color, fontWeight = FontWeight.SemiBold)
            Text(movement.movementDate, fontSize = 11.sp, color = Color.Gray)
            if (!movement.store.isNullOrBlank()) {
                Text("📍 ${movement.store}", fontSize = 11.sp, color = Color.Gray)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$sign${String.format("%.2f", movement.quantity)} $unit",
                fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
            if (movement.priceTotal != null) {
                Text("$${String.format("%.0f", movement.priceTotal)}", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}
