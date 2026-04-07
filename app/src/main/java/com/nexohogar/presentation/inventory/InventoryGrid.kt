package com.nexohogar.presentation.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.domain.model.Product

// ─── Pestaña: Productos (solo muestra stock) ───────────────────────────────────
@Composable
internal fun ProductsTab(
    uiState: InventoryUiState,
    onProductClick: (Product) -> Unit,
    onQuickConsume: (Product) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onAddToInventory: () -> Unit,
    onProductAction: (Product) -> Unit
) {
    val products = uiState.filteredProducts
    val categories = uiState.availableCategories

    Column(Modifier.fillMaxSize()) {
        if (categories.isNotEmpty()) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text("Todos (${uiState.products.size})") }
                )
                categories.forEach { cat ->
                    val count = uiState.products.count { it.category == cat }
                    FilterChip(
                        selected = uiState.selectedCategory == cat,
                        onClick = { onSelectCategory(if (uiState.selectedCategory == cat) null else cat) },
                        label = { Text("$cat ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f)
                        )
                    )
                }
            }
            HorizontalDivider()
        }

        if (products.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.Inventory2, contentDescription = null,
                        tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    if (uiState.selectedCategory != null) {
                        Text("Sin productos en \"${uiState.selectedCategory}\"", color = Color.Gray, fontSize = 16.sp)
                    } else {
                        Text("Sin productos aún", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Ve a la pestaña Registrar para agregar productos", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onAddToInventory,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenIn),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Agregar a despensa", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedButton(
                        onClick = onAddToInventory,
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GreenIn),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = GreenIn, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Agregar a despensa", color = GreenIn, fontWeight = FontWeight.SemiBold)
                    }
                }
                items(products, key = { it.id }) { product ->
                    ProductGridCard(
                        product = product,
                        onClick = { onProductAction(product) }
                    )
                }
            }
        }
    }
}

// ─── Card de producto — stock prominente ──────────────────────────────────────
@Composable
internal fun ProductGridCard(
    product: Product,
    onClick: () -> Unit
) {
    val minStockWarning = product.minStock != null && product.currentStock <= product.minStock
    val stockColor = when {
        product.currentStock <= 0 -> Color(0xFFC62828)
        minStockWarning -> Color(0xFFE65100)
        product.currentStock < 1.0 -> Color(0xFFE65100)
        else -> Color(0xFF2E7D32)
    }
    val stockLabel = when {
        product.currentStock <= 0 -> "Sin stock"
        else -> String.format("%.1f", product.currentStock).trimEnd('0').trimEnd('.')
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (minStockWarning) Color(0xFFFFF8E1) else Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                color = Color(0xFF212121),
                maxLines = 2
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = stockLabel,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = stockColor,
                    lineHeight = 14.sp
                )
                if (product.currentStock > 0) {
                    Text(
                        text = product.unit,
                        fontWeight = FontWeight.Medium,
                        fontSize = 8.sp,
                        color = stockColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            if (!product.category.isNullOrBlank()) {
                Surface(
                    color = PrimaryBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        product.category,
                        fontSize = 8.sp,
                        color = PrimaryBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (minStockWarning) {
                Text(
                    text = "⚠️ Stock bajo",
                    fontSize = 7.sp,
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
