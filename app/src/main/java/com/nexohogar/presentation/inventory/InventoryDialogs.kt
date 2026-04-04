package com.nexohogar.presentation.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.domain.model.Product

@Composable
internal fun ProductActionPopup(
    product: Product,
    onDismiss: () -> Unit,
    onViewHistory: (Product) -> Unit,
    onQuickConsume: (Product) -> Unit
) {
    val stockColor = when {
        product.currentStock <= 0  -> Color(0xFFC62828)
        product.currentStock < 1.0 -> Color(0xFFE65100)
        else                       -> Color(0xFF2E7D32)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Stock: ", fontSize = 13.sp, color = Color.Gray)
                    Text(
                        "${String.format("%.2f", product.currentStock)} ${product.unit}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = stockColor
                    )
                }
                if (!product.category.isNullOrBlank()) {
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            product.category,
                            fontSize = 11.sp,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                if (!product.brand.isNullOrBlank()) {
                    Text(
                        "Marca: ${product.brand}",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onDismiss(); onViewHistory(product) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver historial", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onDismiss(); onQuickConsume(product) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RedOut),
                    shape = RoundedCornerShape(8.dp),
                    enabled = product.currentStock > 0
                ) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Registrar consumo", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// ─── Diálogo de consumo rápido ─────────────────────────────────────────────────
@Composable
internal fun QuickConsumeDialog(
    product: Product,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var qty by remember { mutableStateOf("") }
    val qtyDouble = qty.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Consumir: ${product.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Stock actual: ${String.format("%.2f", product.currentStock)} ${product.unit}",
                    color = Color.Gray, fontSize = 13.sp)
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Cantidad (${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = qtyDouble != null && qtyDouble <= 0
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { qtyDouble?.let { if (it > 0) onConfirm(it) } },
                enabled = qtyDouble != null && qtyDouble > 0
            ) { Text("Confirmar", color = RedOut, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ─── Banner de error reutilizable ─────────────────────────────────────────────
@Composable
internal fun ErrorBanner(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null,
                tint = RedOut, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, color = RedOut, fontSize = 13.sp)
        }
    }
}
