package com.nexohogar.presentation.tutorial

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ═══════════════════════════════════════════════════════════════════════════════
// ARCHIVO NUEVO: Sistema de tutorial / onboarding reutilizable
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Representa un paso del tutorial.
 */
data class TutorialStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color = Color(0xFF1565C0),
    val iconBgColor: Color = Color(0xFFE3F2FD)
)

/**
 * Overlay de tutorial que muestra pasos secuenciales.
 * Se muestra como un diálogo modal con navegación entre pasos.
 */
@Composable
fun TutorialOverlay(
    steps: List<TutorialStep>,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val step = steps[currentStep]
    val isLastStep = currentStep == steps.lastIndex

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Indicador de progreso ────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    steps.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(
                                    width = if (index == currentStep) 24.dp else 8.dp,
                                    height = 8.dp
                                )
                                .clip(CircleShape)
                                .background(
                                    if (index <= currentStep) Color(0xFF1565C0)
                                    else Color(0xFFE0E0E0)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Ícono ─────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(step.iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = step.iconColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Título ────────────────────────────────────────────────
                Text(
                    text = step.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF212121)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Descripción ───────────────────────────────────────────
                Text(
                    text = step.description,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF666666),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Botones de navegación ─────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Saltar
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Saltar",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Botón Atrás
                        if (currentStep > 0) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Atrás")
                            }
                        }

                        // Botón Siguiente / Entendido
                        Button(
                            onClick = {
                                if (isLastStep) onDismiss()
                                else currentStep++
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1565C0)
                            )
                        ) {
                            Text(
                                if (isLastStep) "¡Entendido!" else "Siguiente",
                                fontWeight = FontWeight.Bold
                            )
                            if (!isLastStep) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Contador de pasos
                Text(
                    text = "${currentStep + 1} de ${steps.size}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Pasos predefinidos para HubScreen
// ═══════════════════════════════════════════════════════════════════════════════

val hubTutorialSteps = listOf(
    TutorialStep(
        title = "¡Bienvenido a NexoHogar! 🏠",
        description = "Este es tu centro de control. Desde aquí puedes acceder a todas las funciones de tu hogar.",
        icon = Icons.Default.Home,
        iconColor = Color(0xFF1565C0),
        iconBgColor = Color(0xFFE3F2FD)
    ),
    TutorialStep(
        title = "Resumen financiero",
        description = "Desde \"Resumen\" puedes ver los saldos de tus cuentas, gráficos de gastos e ingresos del mes.",
        icon = Icons.Default.BarChart,
        iconColor = Color(0xFF2E7D32),
        iconBgColor = Color(0xFFE8F5E9)
    ),
    TutorialStep(
        title = "Agregar movimientos",
        description = "Usa el botón \"Agregar\" para registrar ingresos, gastos o transferencias entre cuentas. ¡Es lo que más usarás!",
        icon = Icons.Default.AddCircle,
        iconColor = Color(0xFFF57F17),
        iconBgColor = Color(0xFFFFF8E1)
    ),
    TutorialStep(
        title = "Gestiona tus cuentas",
        description = "En \"Cuentas\" puedes crear billeteras, cuentas bancarias y tarjetas de crédito. Toca una cuenta para ver sus últimos movimientos.",
        icon = Icons.Default.AccountBalance,
        iconColor = Color(0xFF6A1B9A),
        iconBgColor = Color(0xFFF3E5F5)
    ),
    TutorialStep(
        title = "Control de presupuesto",
        description = "Establece límites de gasto mensuales por categoría y monitorea tu progreso en \"Presupuesto\".",
        icon = Icons.Default.AccountBalanceWallet,
        iconColor = Color(0xFF283593),
        iconBgColor = Color(0xFFE8EAF6)
    ),
    TutorialStep(
        title = "Inventario del hogar",
        description = "Lleva el control de tu despensa en \"Inventario\". Registra compras, consumos y recibe sugerencias de compra automáticas.",
        icon = Icons.Default.Inventory,
        iconColor = Color(0xFF33691E),
        iconBgColor = Color(0xFFF1F8E9)
    ),
    TutorialStep(
        title = "Invita a tu hogar",
        description = "¡NexoHogar es colaborativo! Invita a los miembros de tu hogar para que todos puedan registrar movimientos.",
        icon = Icons.Default.PersonAdd,
        iconColor = Color(0xFF00695C),
        iconBgColor = Color(0xFFE0F7FA)
    )
)

// ═══════════════════════════════════════════════════════════════════════════════
// Pasos predefinidos para InventoryScreen (más detallado)
// ═══════════════════════════════════════════════════════════════════════════════

val inventoryTutorialSteps = listOf(
    TutorialStep(
        title = "Tu despensa digital 🥫",
        description = "Aquí puedes controlar todo lo que hay en tu hogar: alimentos, productos de limpieza, y más.",
        icon = Icons.Default.Inventory2,
        iconColor = Color(0xFF33691E),
        iconBgColor = Color(0xFFF1F8E9)
    ),
    TutorialStep(
        title = "Pestaña: Productos",
        description = "Aquí ves todos tus productos con su stock actual. Los colores indican el nivel:\n🟢 Verde = OK\n🟠 Naranja = Bajo\n🔴 Rojo = Sin stock\n\nToca un producto para ver su historial o registrar consumo.",
        icon = Icons.Default.GridView,
        iconColor = Color(0xFF1565C0),
        iconBgColor = Color(0xFFE3F2FD)
    ),
    TutorialStep(
        title = "Pestaña: Registrar",
        description = "Aquí registras movimientos de stock:\n\n🛒 Compra: Cuando compras algo y quieres sumar stock\n📦 Consumo: Cuando usas un producto y se reduce el stock\n\nPuedes registrar para productos existentes o crear uno nuevo.",
        icon = Icons.Default.ShoppingCart,
        iconColor = Color(0xFF2E7D32),
        iconBgColor = Color(0xFFE8F5E9)
    ),
    TutorialStep(
        title = "Crear producto nuevo",
        description = "Al crear un producto:\n1. Dale un nombre descriptivo\n2. Elige una categoría (o crea una nueva desde el menú)\n3. Selecciona la unidad de medida (kg, unidades, litros...)\n4. Opcionalmente registra el stock inicial que ya tienes en casa.",
        icon = Icons.Default.AddCircleOutline,
        iconColor = Color(0xFF00838F),
        iconBgColor = Color(0xFFE0F7FA)
    ),
    TutorialStep(
        title = "Pestaña: Categorías",
        description = "Organiza tus productos en categorías (Lácteos, Limpieza, Carnes, etc.).\n\nAquí también ves las estadísticas de gasto por categoría para saber en qué gastas más.",
        icon = Icons.Default.Category,
        iconColor = Color(0xFF6A1B9A),
        iconBgColor = Color(0xFFF3E5F5)
    ),
    TutorialStep(
        title = "Pestaña: Sugerencias",
        description = "NexoHogar analiza tu consumo mensual y te sugiere qué comprar cuando un producto baje del 50% de tu uso habitual.\n\n¡Entre más uses el inventario, mejores serán las sugerencias!",
        icon = Icons.Default.AutoAwesome,
        iconColor = Color(0xFFF57F17),
        iconBgColor = Color(0xFFFFF8E1)
    ),
    TutorialStep(
        title = "Registrar como compra 💡",
        description = "Al crear un producto con stock inicial, puedes marcar \"Registrar como compra\" para que el gasto aparezca en tus finanzas del hogar.\n\nAsí mantienes sincronizado tu inventario con tus gastos.",
        icon = Icons.Default.AddShoppingCart,
        iconColor = Color(0xFF2E7D32),
        iconBgColor = Color(0xFFE8F5E9)
    ),
    TutorialStep(
        title = "Escáner de boletas 📸",
        description = "¿Compraste muchos productos? Usa el escáner de boletas para agregar múltiples productos automáticamente desde una foto de tu boleta.",
        icon = Icons.Default.CameraAlt,
        iconColor = Color(0xFFC62828),
        iconBgColor = Color(0xFFFFEBEE)
    )
)
