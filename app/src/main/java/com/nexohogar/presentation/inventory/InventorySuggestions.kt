package com.nexohogar.presentation.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.nexohogar.domain.model.PurchaseSuggestion

// ─── Pestaña: Sugerencias ─────────────────────────────────────────────────────
@Composable
internal fun SuggestionsTab(suggestions: List<PurchaseSuggestion>) {
    if (suggestions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null,
                    tint = PrimaryBlue, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Sin sugerencias por ahora", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text("Las sugerencias aparecen cuando el stock baje del 50% de tu consumo mensual.",
                    fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("💡 Basado en tu consumo del último mes", fontSize = 13.sp, color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp))
        }
        items(suggestions, key = { it.product.id }) { suggestion ->
            SuggestionCard(suggestion)
        }
    }
}

@Composable
internal fun SuggestionCard(suggestion: PurchaseSuggestion) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null,
                    tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(suggestion.product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (!suggestion.product.category.isNullOrBlank()) {
                        Text(suggestion.product.category, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Comprar: ${String.format("%.2f", suggestion.suggestedQuantity)} ${suggestion.product.unit}",
                fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
            if (suggestion.estimatedCost != null) {
                Text("Costo estimado: $${String.format("%.0f", suggestion.estimatedCost)}",
                    fontSize = 13.sp, color = Color(0xFF2E7D32))
            }
            Spacer(Modifier.height(4.dp))
            Text(suggestion.reason, fontSize = 8.sp, color = Color.Gray)
        }
    }
}
