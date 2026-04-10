package com.nexohogar.presentation.membership

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.domain.model.UserUsage

@Composable
fun MembershipScreen(
    viewModel: MembershipViewModel,
    householdId: String,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usage = viewModel.usage.collectAsState()
    val isLoading = viewModel.isLoading.collectAsState()
    val error = viewModel.error.collectAsState()

    LaunchedEffect(householdId) {
        android.util.Log.d("MembershipScreen", "LaunchedEffect fired, householdId='$householdId'")
        viewModel.loadUsage(householdId)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Error state
        if (error.value != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFCDD2), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Error: ${error.value}",
                        color = Color(0xFFC62828),
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Loading state
        if (isLoading.value) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier.padding(32.dp)
                )
            }
        }

        // Content
        usage.value?.let { userUsage ->
            // Plan badge
            item {
                PlanBadge(isPremium = userUsage.isPremium, planName = userUsage.planName)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Usage cards
            item {
                UsageProgressCard(
                    title = "Productos",
                    usage = userUsage.products,
                    icon = "📦"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                UsageProgressCard(
                    title = "Inventario",
                    usage = userUsage.inventory,
                    icon = "📋"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                UsageProgressCard(
                    title = "Lista de Deseos",
                    usage = userUsage.wishlist,
                    icon = "❤️"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                UsageProgressCard(
                    title = "Sugerencias",
                    usage = userUsage.suggestions,
                    icon = "💡"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                UsageProgressCard(
                    title = "Pagos Recurrentes",
                    usage = userUsage.recurring,
                    icon = "🔄"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                UsageProgressCard(
                    title = "Cuentas",
                    usage = userUsage.accounts,
                    icon = "🏦"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                UsageProgressCard(
                    title = "Miembros",
                    usage = userUsage.members,
                    icon = "👥"
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Upgrade button
            if (!userUsage.isPremium) {
                item {
                    Button(
                        onClick = onUpgradeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Actualizar a Premium - \$2.990/mes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                item {
                    Button(
                        onClick = onUpgradeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Gestionar suscripción",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PlanBadge(isPremium: Boolean, planName: String) {
    val backgroundColor = if (isPremium) Color(0xFFFDD835) else Color(0xFFE0E0E0)
    val textColor = if (isPremium) Color(0xFF212121) else Color(0xFF424242)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isPremium) "✨ PREMIUM" else "FREE",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun UsageProgressCard(
    title: String,
    usage: UserUsage.LimitUsage,
    icon: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = icon, fontSize = 20.sp)
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
                Text(
                    text = "${usage.used}/${usage.limit}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }

            LinearProgressIndicator(
                progress = { usage.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (usage.isAtLimit) Color(0xFFC62828) else Color(0xFF4CAF50),
                trackColor = Color(0xFFE0E0E0),
            )

            if (usage.isAtLimit) {
                Text(
                    text = "Límite alcanzado",
                    fontSize = 11.sp,
                    color = Color(0xFFC62828),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
