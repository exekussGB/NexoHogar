package com.nexohogar.presentation.expenses

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseByCategoryScreen(
    viewModel: ExpenseByCategoryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clpFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }
    val monthNames = listOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gastos por Categoría") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Filtro de mes ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newMonth = if (uiState.selectedMonth == 1) 12 else uiState.selectedMonth - 1
                        val newYear = if (uiState.selectedMonth == 1) uiState.selectedYear - 1 else uiState.selectedYear
                        viewModel.onMonthChanged(newMonth, newYear)
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
                    }
                    Text(
                        text = "${monthNames[uiState.selectedMonth - 1]} ${uiState.selectedYear}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        val newMonth = if (uiState.selectedMonth == 12) 1 else uiState.selectedMonth + 1
                        val newYear = if (uiState.selectedMonth == 12) uiState.selectedYear + 1 else uiState.selectedYear
                        viewModel.onMonthChanged(newMonth, newYear)
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
                    }
                }
            }

            // ── Filtro de usuario ────────────────────────────────────────────
            item {
                var expanded by remember { mutableStateOf(false) }
                val selectedName = if (uiState.selectedUserId == null) "Todos los miembros"
                else uiState.members.find { it.id == uiState.selectedUserId }?.label() ?: "Seleccionar"

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filtrar por usuario") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos los miembros") },
                            onClick = {
                                viewModel.onUserFilterChanged(null)
                                expanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) }
                        )
                        uiState.members.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member.label()) },
                                onClick = {
                                    viewModel.onUserFilterChanged(member.id)
                                    expanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                            )
                        }
                    }
                }
            }

            // ── Total ────────────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total Gastos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(
                                clpFormat.format(uiState.totalExpenses),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // ── Loading ──────────────────────────────────────────────────────
            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            // ── Categories ───────────────────────────────────────────────────
            if (!uiState.isLoading && uiState.expenses.isEmpty()) {
                item {
                    Text(
                        "No hay gastos en este período",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

            val categoryColors = listOf(
                Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00),
                Color(0xFF8E24AA), Color(0xFF00ACC1), Color(0xFFD81B60), Color(0xFF3949AB)
            )

            items(uiState.expenses) { expense ->
                val index = uiState.expenses.indexOf(expense)
                val color = categoryColors[index % categoryColors.size]
                val percentage = if (uiState.totalExpenses > 0)
                    (expense.totalAmount.toFloat() / uiState.totalExpenses.toFloat()) * 100f else 0f

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Color bar
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(48.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                expense.categoryName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${expense.transactionCount} movimiento${if (expense.transactionCount != 1L) "s" else ""} · ${"%.1f".format(percentage)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            // Progress bar
                            LinearProgressIndicator(
                                progress = { (percentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(4.dp),
                                color = color,
                                trackColor = color.copy(alpha = 0.15f)
                            )
                        }
                        Text(
                            clpFormat.format(expense.totalAmount),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                    }
                }
            }

            // ── Error ────────────────────────────────────────────────────────
            if (uiState.error != null) {
                item {
                    Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}