package com.nexohogar.presentation.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.domain.model.PurchaseSuggestion

// Punto 5: símbolo de moneda como constante — cámbialo aquí para toda la app
private const val CURRENCY_SYMBOL = "$"

// ─── Tab principal ────────────────────────────────────────────────────────────

@Composable
internal fun SuggestionsTab(
    suggestions: List<PurchaseSuggestion>,
    // Punto 3: recibe isLoading para mostrar spinner mientras carga
    isLoading: Boolean,
    onAddToWishlist: (PurchaseSuggestion) -> Unit
) {
    // Punto 3: mostrar spinner mientras se calculan las sugerencias
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Calculando sugerencias...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (suggestions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "¡Todo en orden!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No hay productos con stock bajo en este momento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "${suggestions.size} producto${if (suggestions.size != 1) "s" else ""} necesitan reposición",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(suggestions, key = { it.productId }) { suggestion ->
            SuggestionCard(
                suggestion      = suggestion,
                onAddToWishlist = onAddToWishlist
            )
        }
    }
}

// ─── Tarjeta individual ───────────────────────────────────────────────────────

@Composable
private fun SuggestionCard(
    suggestion: PurchaseSuggestion,
    onAddToWishlist: (PurchaseSuggestion) -> Unit
) {
    var added by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Cabecera: nombre + categoría
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = suggestion.productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                suggestion.category?.let { cat ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(cat, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Métricas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricItem(
                    label = "Stock actual",
                    value = formatQty(suggestion.currentStock, suggestion.unit),
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Sugerido",
                    value = formatQty(suggestion.suggestedQuantity, suggestion.unit),
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
                suggestion.estimatedCost?.let { cost ->
                    MetricItem(
                        label = "Costo est.",
                        value = "$CURRENCY_SYMBOL${String.format("%.0f", cost)}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Razón
            Text(
                text = suggestion.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Botón: agregar a wishlist
            OutlinedButton(
                onClick = {
                    if (!added) {
                        onAddToWishlist(suggestion)
                        added = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !added
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (added) "Agregado a wishlist ✓" else "Agregar a wishlist")
            }
        }
    }
}

// ─── Composables auxiliares ───────────────────────────────────────────────────

@Composable
private fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun formatQty(qty: Double, unit: String = ""): String {
    val formatted = if (qty == kotlin.math.floor(qty)) {
        qty.toInt().toString()
    } else {
        String.format("%.1f", qty)
    }
    return if (unit.isNotBlank()) "$formatted $unit" else formatted
}
