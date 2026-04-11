package com.nexohogar.presentation.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.domain.model.LimitType
import com.nexohogar.domain.model.Plan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumLimitsScreen(
    viewModel: PremiumLimitsViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resumen de Plan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Error cargando información",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            state.errorMessage ?: "Error desconocido",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            state.plan != null -> {
                PremiumLimitsContent(
                    plan = state.plan!!,
                    viewModel = viewModel,
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun PremiumLimitsContent(
    plan: Plan,
    viewModel: PremiumLimitsViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con nombre del plan
        PlanHeaderCard(plan = plan)

        // Límites
        SectionLabel("Límites del Plan")

        LimitCard(
            title = "Productos",
            limit = viewModel.getLimitDisplay(LimitType.PRODUCTS),
            icon = Icons.Default.Inventory,
            hasLimit = viewModel.hasLimit(LimitType.PRODUCTS),
            isPremium = viewModel.isPremium()
        )

        LimitCard(
            title = "Inventario",
            limit = viewModel.getLimitDisplay(LimitType.INVENTORY_ITEMS),
            icon = Icons.Default.Inventory2,
            hasLimit = viewModel.hasLimit(LimitType.INVENTORY_ITEMS),
            isPremium = viewModel.isPremium()
        )

        LimitCard(
            title = "Lista de Deseos",
            limit = viewModel.getLimitDisplay(LimitType.WISHLIST_ITEMS),
            icon = Icons.Default.Favorite,
            hasLimit = viewModel.hasLimit(LimitType.WISHLIST_ITEMS),
            isPremium = viewModel.isPremium()
        )

        LimitCard(
            title = "Compras Futuras",
            limit = viewModel.getLimitDisplay(LimitType.FUTURE_PURCHASES),
            icon = Icons.Default.ShoppingCart,
            hasLimit = viewModel.hasLimit(LimitType.FUTURE_PURCHASES),
            isPremium = viewModel.isPremium()
        )

        LimitCard(
            title = "Pagos Recurrentes",
            limit = viewModel.getLimitDisplay(LimitType.RECURRING),
            icon = Icons.Default.Repeat,
            hasLimit = viewModel.hasLimit(LimitType.RECURRING),
            isPremium = viewModel.isPremium()
        )

        LimitCard(
            title = "Cuentas",
            limit = viewModel.getLimitDisplay(LimitType.ACCOUNTS),
            icon = Icons.Default.AccountBalance,
            hasLimit = viewModel.hasLimit(LimitType.ACCOUNTS),
            isPremium = viewModel.isPremium()
        )

        LimitCard(
            title = "Miembros del Hogar",
            limit = viewModel.getLimitDisplay(LimitType.MEMBERS),
            icon = Icons.Default.Group,
            hasLimit = viewModel.hasLimit(LimitType.MEMBERS),
            isPremium = viewModel.isPremium()
        )

        // Características
        if (plan.features.isNotEmpty()) {
            SectionLabel("Características Incluidas")
            FeaturesCard(features = plan.features)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PlanHeaderCard(plan: Plan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (plan.isPremium()) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (plan.isPremium()) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Premium",
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = plan.displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (plan.isPremium()) "Acceso ilimitado a todas las funciones" else "Plan básico con límites",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LimitCard(
    title: String,
    limit: String,
    icon: ImageVector,
    hasLimit: Boolean,
    isPremium: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (hasLimit && !isPremium) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (hasLimit && !isPremium) Color(0xFFC62828) else Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = limit,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (hasLimit && !isPremium && limit != "Ilimitado") Color(0xFFC62828) else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FeaturesCard(features: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = feature.replace("_", " ").capitalize(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}
