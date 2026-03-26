package com.nexohogar.presentation.categoryexpenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.util.*

// Paleta de colores para las categorías
private val categoryColors = listOf(
    Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF26A69A),
    Color(0xFF66BB6A), Color(0xFFFFCA28), Color(0xFFFF7043),
    Color(0xFFEC407A), Color(0xFFAB47BC), Color(0xFF8D6E63),
    Color(0xFF78909C)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryExpensesScreen(
    viewModel: CategoryExpensesViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState  by viewModel.uiState.collectAsState()
    val clpFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gastos por Categoría", fontWeight = FontWeight.Bold)
                        Text(
                            text  = "Mes actual",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> LoadingOverlay()

                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text  = uiState.error ?: "Error desconocido",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = viewModel::load) { Text("Reintentar") }
                        }
                    }
                }

                uiState.categories.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector        = Icons.Default.PieChart,
                                contentDescription = null,
                                modifier           = Modifier.size(56.dp),
                                tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text  = "Sin gastos este mes",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding      = PaddingValues(vertical = 16.dp)
                    ) {
                        // ── Resumen total ─────────────────────────────────────
                        item {
                            val total = uiState.categories.sumOf { it.totalAmount }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors   = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier                = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement   = Arrangement.SpaceBetween,
                                    verticalAlignment       = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text  = "Total gastado este mes",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text       = clpFormat.format(total),
                                            style      = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color      = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Text(
                                        text  = "${uiState.categories.size} categorías",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        // ── Lista de categorías ────────────────────────────────
                        itemsIndexed(uiState.categories) { index, category ->
                            val color = categoryColors[index % categoryColors.size]
                            CategoryExpenseRow(
                                categoryName = category.categoryName,
                                totalAmount  = category.totalAmount,
                                percentage   = category.percentage,
                                color        = color,
                                format       = clpFormat
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryExpenseRow(
    categoryName : String,
    totalAmount  : Long,
    percentage   : Double,
    color        : Color,
    format       : NumberFormat
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Indicador de color
                Box(
                    modifier        = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = categoryName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp
                    )
                    Text(
                        text  = format.format(totalAmount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text       = "${percentage}%",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = color
                )
            }

            // Barra de progreso
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress         = { (percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color            = color,
                trackColor       = color.copy(alpha = 0.15f)
            )
        }
    }
}
