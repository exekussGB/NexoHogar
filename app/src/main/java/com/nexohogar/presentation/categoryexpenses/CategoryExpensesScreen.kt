package com.nexohogar.presentation.categoryexpenses

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.domain.model.CategoryExpenseGroup
import com.nexohogar.domain.model.CategoryExpenseByUser
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.util.*

// User colors palette
private val userColors = listOf(
    Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF26A69A),
    Color(0xFF66BB6A), Color(0xFFFFCA28), Color(0xFFFF7043),
    Color(0xFFEC407A), Color(0xFFAB47BC), Color(0xFF8D6E63),
    Color(0xFF78909C)
)

// Semáforo colors (same as budget)
private val SemaforoBlue   = Color(0xFF42A5F5)
private val SemaforoGreen  = Color(0xFF66BB6A)
private val SemaforoYellow = Color(0xFFFFA726)
private val SemaforoRed    = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryExpensesScreen(
    viewModel: CategoryExpensesViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clpFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }

    // Build a user → color map
    val userColorMap = remember(uiState.categories) {
        val allUserIds = uiState.categories
            .flatMap { it.users }
            .map { it.userId ?: it.userName }
            .distinct()
        allUserIds.mapIndexed { i, uid -> uid to userColors[i % userColors.size] }.toMap()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gastos por Categoría", fontWeight = FontWeight.Bold)
                        val periodText = when (uiState.selectedMonths) {
                            1 -> "Mes actual"
                            6 -> "Últimos 6 meses"
                            12 -> "Últimos 12 meses"
                            else -> "Últimos ${uiState.selectedMonths} meses"
                        }
                        Text(
                            text = periodText,
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
                                text = uiState.error ?: "Error desconocido",
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
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Sin gastos en este período",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        // Period filter buttons
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1 to "Este mes", 6 to "6 meses", 12 to "12 meses").forEach { (months, label) ->
                                    FilterChip(
                                        selected = uiState.selectedMonths == months,
                                        onClick = { viewModel.setMonths(months) },
                                        label = { Text(label, fontSize = 13.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        // Summary card
                        item {
                            val total = uiState.categories.sumOf { it.totalAmount }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Total gastado",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = clpFormat.format(total),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Text(
                                        text = "${uiState.categories.size} categorías",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        // Category cards with user breakdown
                        items(uiState.categories, key = { it.categoryName }) { category ->
                            CategoryExpenseCard(
                                category = category,
                                clpFormat = clpFormat,
                                userColorMap = userColorMap,
                                totalGlobal = uiState.categories.sumOf { it.totalAmount }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryExpenseCard(
    category: CategoryExpenseGroup,
    clpFormat: NumberFormat,
    userColorMap: Map<String, Color>,
    totalGlobal: Long
) {
    var expanded by remember { mutableStateOf(false) }
    val pct = if (totalGlobal > 0) (category.totalAmount * 100.0 / totalGlobal) else 0.0
    val barColor = getSemaforoColor(pct)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Main category row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.categoryName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = clpFormat.format(category.totalAmount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = barColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", pct)}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = barColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Contraer" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Category progress bar
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = barColor.copy(alpha = 0.15f)
            )

            // User breakdown (expanded)
            if (expanded && category.users.size > 0) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Desglose por usuario",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Find max amount within this category for proportional bars
                val maxUserAmount = category.users.maxOfOrNull { it.totalAmount } ?: 1L

                category.users.forEach { user ->
                    val userKey = user.userId ?: user.userName
                    val userColor = userColorMap[userKey] ?: Color(0xFF78909C)
                    val userPct = if (category.totalAmount > 0)
                        (user.totalAmount * 100.0 / category.totalAmount) else 0.0
                    val barFraction = (user.totalAmount.toFloat() / maxUserAmount).coerceIn(0f, 1f)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        // User avatar circle
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(userColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = userColor
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = user.userName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${clpFormat.format(user.totalAmount)} (${String.format("%.0f", userPct)}%)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { barFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = userColor,
                                trackColor = userColor.copy(alpha = 0.12f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getSemaforoColor(percentage: Double): Color = when {
    percentage < 15  -> SemaforoBlue
    percentage < 30  -> SemaforoGreen
    percentage < 50  -> SemaforoYellow
    else             -> SemaforoRed
}
