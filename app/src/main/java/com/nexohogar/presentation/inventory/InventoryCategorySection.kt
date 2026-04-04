package com.nexohogar.presentation.inventory

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.domain.model.InventoryCategory

// ─── Pestaña: Categorías (gestión + estadísticas) ──────────────────────────────
@Composable
internal fun CategoriesTab(
    viewModel: InventoryViewModel,
    categories: List<InventoryCategory>,
    stats: List<CategoryStat>
) {
    val categoryForm by viewModel.categoryForm.collectAsState()

    LaunchedEffect(categoryForm.success) {
        if (categoryForm.success) viewModel.resetCategoryForm()
    }

    var categoryToDelete by remember { mutableStateOf<InventoryCategory?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Gestionar categorías", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(4.dp))
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LightBlue),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Nueva categoría", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = PrimaryBlue)
                    OutlinedTextField(
                        value = categoryForm.name,
                        onValueChange = viewModel::onCategoryNameChange,
                        label = { Text("Nombre *") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = categoryForm.icon,
                        onValueChange = viewModel::onCategoryIconChange,
                        label = { Text("Ícono (emoji, opcional)") },
                        placeholder = { Text("Ej: 🥩 🧴 🍎") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    categoryForm.error?.let { ErrorBanner(it) }
                    Button(
                        onClick = { viewModel.submitCategory() },
                        enabled = !categoryForm.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (categoryForm.isSubmitting) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Crear categoría", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (categories.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Category, contentDescription = null,
                            tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Sin categorías aún", color = Color.Gray, fontSize = 14.sp)
                        Text("Crea una categoría arriba para organizar tus productos",
                            color = Color.Gray, fontSize = 8.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Categorías actuales (${categories.size})", fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp, color = Color.Gray)
            }
            items(categories, key = { it.id }) { category ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!category.icon.isNullOrBlank()) {
                            Text(category.icon, fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(category.name, fontWeight = FontWeight.Medium, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { categoryToDelete = category }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                                tint = RedOut, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        if (stats.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Estadísticas de gasto", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            val totalSpent = stats.sumOf { it.totalSpent }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = PrimaryBlue), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Total gastado registrado", color = Color.White, fontSize = 13.sp)
                        Text("$${String.format("%,.0f", totalSpent)}", color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("${stats.size} ${if (stats.size == 1) "categoría" else "categorías"}",
                            color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp)
                    }
                }
            }
            items(stats, key = { it.category }) { stat ->
                CategoryStatCard(stat = stat, totalSpent = totalSpent)
            }
        } else {
            item {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BarChart, contentDescription = null,
                            tint = PrimaryBlue, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Sin estadísticas aún", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Registra compras con precio para ver cuánto gastas por categoría.",
                            fontSize = 8.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Eliminar categoría", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que quieres eliminar \"${category.name}\"? Los productos con esta categoría no se eliminarán.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCategory(category.id); categoryToDelete = null }) {
                    Text("Eliminar", color = RedOut, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
internal fun CategoryStatCard(stat: CategoryStat, totalSpent: Double) {
    val percentage = if (totalSpent > 0) (stat.totalSpent / totalSpent * 100).toFloat() else 0f

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Category, contentDescription = null,
                        tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(stat.category, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("${stat.productCount} ${if (stat.productCount == 1) "producto" else "productos"}",
                            fontSize = 8.sp, color = Color.Gray)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$${String.format("%,.0f", stat.totalSpent)}", fontWeight = FontWeight.Bold,
                        color = PrimaryBlue, fontSize = 16.sp)
                    Text("${String.format("%.1f", percentage)}%", fontSize = 8.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = PrimaryBlue, trackColor = LightBlue
            )
        }
    }
}
