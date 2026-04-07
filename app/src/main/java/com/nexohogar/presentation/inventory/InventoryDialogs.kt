package com.nexohogar.presentation.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.domain.model.Product

// ─── Popup de acciones de producto (ACTUALIZADO con onEditProduct) ─────────────
@Composable
internal fun ProductActionPopup(
    product: Product,
    onDismiss: () -> Unit,
    onViewHistory: (Product) -> Unit,
    onQuickConsume: (Product) -> Unit,
    onEditProduct: (Product) -> Unit = {}   // ✅ NUEVO
) {
    val stockColor = when {
        product.currentStock <= 0 -> Color(0xFFC62828)
        product.currentStock < 1.0 -> Color(0xFFE65100)
        else -> Color(0xFF2E7D32)
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
                // ✅ NUEVO: botón Editar producto
                OutlinedButton(
                    onClick = { onEditProduct(product) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Editar producto", fontWeight = FontWeight.SemiBold)
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

// ─── Diálogo de edición de producto ✅ NUEVO ──────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditProductDialog(
    form: EditProductFormState,
    categories: List<String>,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onBrandChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onMinStockChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    val units = listOf("kg", "gramos", "unidades", "litros", "ml")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar producto", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = { Text("Nombre *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Unidad
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it }
                ) {
                    OutlinedTextField(
                        value = form.unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidad *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit) },
                                onClick = { onUnitChange(unit); unitExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = form.brand,
                    onValueChange = onBrandChange,
                    label = { Text("Marca (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Categoría
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = form.category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría (opcional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        DropdownMenuItem(text = { Text("Sin categoría") },
                            onClick = { onCategoryChange(""); categoryExpanded = false })
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) },
                                onClick = { onCategoryChange(cat); categoryExpanded = false })
                        }
                    }
                }

                // ✅ NUEVO: stock mínimo de alerta
                OutlinedTextField(
                    value = form.minStock,
                    onValueChange = onMinStockChange,
                    label = { Text("Stock mínimo de alerta (opcional)") },
                    placeholder = { Text("ej: 2") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                form.error?.let { ErrorBanner(it) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !form.isSubmitting && form.name.isNotBlank()
            ) {
                if (form.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(16.dp))
                } else {
                    Text("Guardar", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ─── Diálogo: lista de compras automática ✅ NUEVO ────────────────────────────
@Composable
internal fun ShoppingListDialog(
    products: List<Product>,
    listText: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null,
                    tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Lista de compras (${products.size})", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (products.isEmpty()) {
                    Text("No hay productos con stock bajo.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    products.forEach { product ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(product.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text(
                                "Stock: ${String.format("%.1f", product.currentStock)} ${product.unit}",
                                fontSize = 12.sp,
                                color = Color(0xFFC62828)
                            )
                        }
                        HorizontalDivider(Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clipboardManager.setText(AnnotatedString(listText))
                onDismiss()
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copiar lista", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
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
