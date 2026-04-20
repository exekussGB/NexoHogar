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
import com.nexohogar.core.tutorial.TutorialModule
import com.nexohogar.core.tutorial.TutorialManager

// ═══════════════════════════════════════════════════════════════════════════════
// Sistema de tutorial / onboarding reutilizable
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
// Menú Principal - Menciona seguridad biométrica y sesión mejorada
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
        title = "Seguridad biométrica 🔒",
        description = "Tu app ahora está protegida con huella dactilar o reconocimiento facial. Al abrir NexoHogar se te pedirá autenticación biométrica para mayor seguridad.",
        icon = Icons.Default.Fingerprint,
        iconColor = Color(0xFF00695C),
        iconBgColor = Color(0xFFE0F7FA)
    ),
    TutorialStep(
        title = "Resumen financiero",
        description = "Desde \"Resumen\" puedes ver los saldos reales de tus cuentas, gráficos de gastos e ingresos del mes. ¡El balance ahora se actualiza en tiempo real!",
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
        description = "En \"Cuentas\" puedes crear billeteras, cuentas bancarias y tarjetas de crédito. Ahora con íconos visuales por tipo y desliza para editar o eliminar.",
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
        title = "Navegación mejorada ←",
        description = "Ahora todas las pantallas tienen botón de retroceso para volver fácilmente. Navega sin perderte entre secciones.",
        icon = Icons.Default.ArrowBack,
        iconColor = Color(0xFF455A64),
        iconBgColor = Color(0xFFECEFF1)
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
// Inventario - Incluye pasos del carrito de compras desde Sugerencias
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
        title = "Carrito de compras 🛒",
        description = "En la pestaña \"Sugerencias\", toca el botón \"Agregar a lista de compras\" para guardar los productos que necesitas comprar.\n\nEl botón del carrito (🛒) siempre está visible para gestionar tu lista rápidamente.",
        icon = Icons.Default.ShoppingCart,
        iconColor = Color(0xFFE91E63),
        iconBgColor = Color(0xFFFCE4EC)
    ),
    TutorialStep(
        title = "Gestionar carrito 📋",
        description = "En el carrito ves:\n✅ Productos con bajo stock (rojo)\n✅ Nuevos items sugeridos (azul)\n✅ Cantidades a comprar (calculadas automáticamente)\n\nToca la X roja en cada producto si no quieres comprarlo.",
        icon = Icons.Default.ManageSearch,
        iconColor = Color(0xFF1565C0),
        iconBgColor = Color(0xFFE3F2FD)
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

// ═══════════════════════════════════════════════════════════════════════════════
// Dashboard — balance en tiempo real
// ═══════════════════════════════════════════════════════════════════════════════

val dashboardTutorialSteps = listOf(
    TutorialStep(
        title = "Panel de control 📊",
        description = "Aquí puedes ver un resumen de tus finanzas: saldos de cuentas, ingresos y gastos del mes.",
        icon = Icons.Default.BarChart,
        iconColor = Color(0xFF1565C0),
        iconBgColor = Color(0xFFE3F2FD)
    ),
    TutorialStep(
        title = "Liquidez Real 💧",
        description = "¡Nuevo! Ahora calculamos cuánto dinero tienes disponible realmente después de descontar tus cuentas pendientes del mes. No te lleves sorpresas al final de mes.",
        icon = Icons.Default.AccountBalanceWallet,
        iconColor = Color(0xFF0288D1),
        iconBgColor = Color(0xFFE1F5FE)
    ),
    TutorialStep(
        title = "Detección de Anomalías ⚠️",
        description = "NexoHogar te avisará si un gasto recurrente (como la luz o el agua) sube más de un 15% respecto a tu promedio de 3 meses. ¡Mantén tus gastos bajo control!",
        icon = Icons.Default.Warning,
        iconColor = Color(0xFFF57F17),
        iconBgColor = Color(0xFFFFF8E1)
    ),
    TutorialStep(
        title = "Segregación de Ahorros y Deudas",
        description = "Tus ahorros ahora se muestran aparte y no afectan tu balance operativo diario. También verás tu \"Carga Financiera\" para tener claridad sobre tus deudas.",
        icon = Icons.Default.CreditCard,
        iconColor = Color(0xFFE65100),
        iconBgColor = Color(0xFFFFF3E0)
    ),
    TutorialStep(
        title = "Balance en tiempo real 💰",
        description = "El balance total ahora se calcula sumando los saldos reales de todas tus cuentas. Se actualiza automáticamente cada vez que registras un movimiento.",
        icon = Icons.Default.AccountBalanceWallet,
        iconColor = Color(0xFF2E7D32),
        iconBgColor = Color(0xFFE8F5E9)
    ),
    TutorialStep(
        title = "Últimos movimientos",
        description = "Debajo del resumen verás tus movimientos más recientes. Toca uno para ver sus detalles completos.",
        icon = Icons.Default.Receipt,
        iconColor = Color(0xFFF57F17),
        iconBgColor = Color(0xFFFFF8E1)
    ),
    TutorialStep(
        title = "Gastos por categoría",
        description = "Accede a un desglose visual de tus gastos organizados por categoría para entender en qué gastas más.",
        icon = Icons.Default.PieChart,
        iconColor = Color(0xFF6A1B9A),
        iconBgColor = Color(0xFFF3E5F5)
    )
)

// ═══════════════════════════════════════════════════════════════════════════════
// Cuentas — íconos por tipo, secciones por subtipo, swipe
// ═══════════════════════════════════════════════════════════════════════════════

val accountsTutorialSteps = listOf(
    TutorialStep(
        title = "Tus cuentas 🏦",
        description = "Aquí puedes ver todas tus cuentas: billeteras, cuentas bancarias, tarjetas de crédito y más.",
        icon = Icons.Default.AccountBalance,
        iconColor = Color(0xFF1565C0),
        iconBgColor = Color(0xFFE3F2FD)
    ),
    TutorialStep(
        title = "Cupos de Crédito 💳",
        description = "¡Nuevo! Para tus tarjetas de crédito, ahora puedes definir un límite o cupo. La app te mostrará cuánto tienes disponible y el porcentaje de uso actual.",
        icon = Icons.Default.CreditCard,
        iconColor = Color(0xFFE91E63),
        iconBgColor = Color(0xFFFCE4EC)
    ),
    TutorialStep(
        title = "Ahorros vs Operativas",
        description = "Marca tus cuentas como \"Ahorro\" para que su saldo se sume al ahorro total del hogar y no se mezcle con tu dinero del día a día.",
        icon = Icons.Default.Savings,
        iconColor = Color(0xFF6A1B9A),
        iconBgColor = Color(0xFFF3E5F5)
    ),
    TutorialStep(
        title = "Íconos por tipo",
        description = "Cada cuenta ahora muestra un ícono según su tipo:\n\n🏦 Banco — cuenta corriente\n⭐ Ahorro — cuenta de ahorro\n💳 Tarjeta — crédito\n👛 Efectivo — billetera\n📈 Inversión\n📁 Otros",
        icon = Icons.Default.Category,
        iconColor = Color(0xFF6A1B9A),
        iconBgColor = Color(0xFFF3E5F5)
    ),
    TutorialStep(
        title = "Secciones organizadas",
        description = "Las cuentas están agrupadas por tipo: Bancarias, Ahorro, Tarjetas, Efectivo y Otros. Así encuentras rápido lo que buscas.",
        icon = Icons.Default.ViewList,
        iconColor = Color(0xFF00838F),
        iconBgColor = Color(0xFFE0F7FA)
    ),
    TutorialStep(
        title = "Desliza para gestionar ↔️",
        description = "Desliza cualquier cuenta hacia la derecha para editarla (naranja) o hacia la izquierda para eliminarla (rojo). Sentirás una vibración al cruzar el umbral.",
        icon = Icons.Default.SwipeRight,
        iconColor = Color(0xFFE65100),
        iconBgColor = Color(0xFFFFF3E0)
    ),
    TutorialStep(
        title = "Crear nueva cuenta",
        description = "Usa el botón \"+\" para agregar una nueva cuenta. Puedes elegir si es compartida con el hogar o personal.",
        icon = Icons.Default.AddCard,
        iconColor = Color(0xFF2E7D32),
        iconBgColor = Color(0xFFE8F5E9)
    )
)

// ═══════════════════════════════════════════════════════════════════════════════
// Transacciones — swipe mejorado, filtro por fecha, edición directa
// ═══════════════════════════════════════════════════════════════════════════════

val transactionsTutorialSteps = listOf(
    TutorialStep(
        title = "Movimientos 💰",
        description = "Aquí encuentras el historial completo de todos los ingresos, gastos y transferencias de tu hogar.",
        icon = Icons.Default.Receipt,
        iconColor = Color(0xFF1565C0),
        iconBgColor = Color(0xFFE3F2FD)
    ),
    TutorialStep(
        title = "Desliza para gestionar ↔️",
        description = "Desliza un movimiento hacia la derecha para editarlo directamente (se abre en modo edición). Desliza hacia la izquierda para ver el detalle. ¡Ahora el gesto es más sensible y con vibración!",
        icon = Icons.Default.SwipeRight,
        iconColor = Color(0xFFE65100),
        iconBgColor = Color(0xFFFFF3E0)
    ),
    TutorialStep(
        title = "Filtro por fecha 📅",
        description = "Toca el ícono de calendario para filtrar movimientos por rango de fechas. Puedes elegir un período personalizado o usar atajos rápidos: Hoy, Esta semana, Este mes o Mes anterior.",
        icon = Icons.Default.DateRange,
        iconColor = Color(0xFF283593),
        iconBgColor = Color(0xFFE8EAF6)
    ),
    TutorialStep(
        title = "Filtros combinados",
        description = "Combina el filtro de fecha con los chips de tipo (Ingresos, Gastos, Transferencias) para encontrar exactamente lo que buscas. Un chip mostrará el rango activo y puedes cerrarlo con ✕.",
        icon = Icons.Default.FilterList,
        iconColor = Color(0xFF00695C),
        iconBgColor = Color(0xFFE0F7FA)
    ),
    TutorialStep(
        title = "Agregar movimiento",
        description = "Usa el botón \"+\" para registrar un nuevo ingreso, gasto o transferencia entre cuentas.",
        icon = Icons.Default.AddCircle,
        iconColor = Color(0xFF2E7D32),
        iconBgColor = Color(0xFFE8F5E9)
    )
)

// ═══════════════════════════════════════════════════════════════════════════════
// Presupuestos
// ═══════════════════════════════════════════════════════════════════════════════

val budgetsTutorialSteps = listOf(
    TutorialStep(
        title = "Presupuestos 📋",
        description = "Establece límites de gasto mensuales por categoría para mantener tus finanzas bajo control.",
        icon = Icons.Default.AccountBalanceWallet,
        iconColor = Color(0xFF283593),
        iconBgColor = Color(0xFFE8EAF6)
    ),
    TutorialStep(
        title = "Semáforo de gasto",
        description = "Cada presupuesto muestra un semáforo:\n🟢 Menos del 50% gastado\n🟠 Entre 50% y 80%\n🔴 Más del 80%",
        icon = Icons.Default.Warning,
        iconColor = Color(0xFF2E7D32),
        iconBgColor = Color(0xFFE8F5E9)
    ),
    TutorialStep(
        title = "Crear presupuesto",
        description = "Usa el botón \"+\" para crear un nuevo presupuesto. Elige la categoría y el monto máximo mensual.",
        icon = Icons.Default.AddCircle,
        iconColor = Color(0xFFF57F17),
        iconBgColor = Color(0xFFFFF8E1)
    )
)

// ═══════════════════════════════════════════════════════════════════════════════
// Gastos Recurrentes — historial mejorado + categoría
// ═══════════════════════════════════════════════════════════════════════════════

val recurringBillsTutorialSteps = listOf(
    TutorialStep(
        title = "Gastos recurrentes 🔄",
        description = "Registra tus pagos periódicos: arriendo, servicios, suscripciones y más.",
        icon = Icons.Default.EventRepeat,
        iconColor = Color(0xFF1565C0),
        iconBgColor = Color(0xFFE3F2FD)
    ),
    TutorialStep(
        title = "Escaneo con OCR 📸",
        description = "¡Nuevo! Al pagar una cuenta, puedes usar la cámara para escanear el monto de tu boleta automáticamente mediante inteligencia artificial. ¡Ahorra tiempo!",
        icon = Icons.Default.CameraAlt,
        iconColor = Color(0xFF00838F),
        iconBgColor = Color(0xFFE0F7FA)
    ),
    TutorialStep(
        title = "Resumen mensual",
        description = "Ve el total de tus gastos recurrentes del mes y cuánto ya has pagado.",
        icon = Icons.Default.Summarize,
        iconColor = Color(0xFF2E7D32),
        iconBgColor = Color(0xFFE8F5E9)
    ),
    TutorialStep(
        title = "Categoría del gasto 🏷️",
        description = "Al crear o editar un gasto recurrente puedes asignarle una categoría. Así, cuando lo marques como pagado, la transacción se registrará con esa categoría y se reflejará en tus presupuestos.",
        icon = Icons.Default.Label,
        iconColor = Color(0xFFF57F17),
        iconBgColor = Color(0xFFFFF8E1)
    ),
    TutorialStep(
        title = "Historial de pagos",
        description = "Toca \"Ver historial\" en cualquier gasto recurrente para ver todos los pagos realizados. Los pagos se registran automáticamente al marcar como \"Pagado\" desde esta pantalla.",
        icon = Icons.Default.History,
        iconColor = Color(0xFF6A1B9A),
        iconBgColor = Color(0xFFF3E5F5)
    )
)

// ═══════════════════════════════════════════════════════════════════════════════
// Hogar —  diseño visual con tarjetas gradiente
// ═══════════════════════════════════════════════════════════════════════════════

val householdTutorialSteps = listOf(
    TutorialStep(
        title = "Selección de hogar 🏡",
        description = "Aquí seleccionas el hogar con el que quieres trabajar. Cada hogar aparece como una tarjeta visual con un gradiente de color único.",
        icon = Icons.Default.Home,
        iconColor = Color(0xFF1565C0),
        iconBgColor = Color(0xFFE3F2FD)
    ),
    TutorialStep(
        title = "Tarjetas visuales",
        description = "Las tarjetas muestran el nombre del hogar, los miembros que lo componen, y un ícono identificativo. Si tienes múltiples hogares, aparecen en una cuadrícula de 2 columnas.",
        icon = Icons.Default.Dashboard,
        iconColor = Color(0xFF6A1B9A),
        iconBgColor = Color(0xFFF3E5F5)
    ),
    TutorialStep(
        title = "Miembros del hogar",
        description = "Invita a los miembros de tu hogar para que todos puedan registrar movimientos y ver las finanzas compartidas.",
        icon = Icons.Default.Group,
        iconColor = Color(0xFF2E7D32),
        iconBgColor = Color(0xFFE8F5E9)
    ),
    TutorialStep(
        title = "Mi Plan 💎",
        description = "Desde \"Mi Plan\" puedes ver tu membresía activa, los límites de tu hogar y gestionar tu suscripción.",
        icon = Icons.Default.WorkspacePremium,
        iconColor = Color(0xFFFF8F00),
        iconBgColor = Color(0xFFFFE0B2)
    ),
    TutorialStep(
        title = "Repetir tutoriales",
        description = "Puedes volver a ver cualquier tutorial desde Ajustes, en la sección de ayuda.",
        icon = Icons.Default.Help,
        iconColor = Color(0xFFF57F17),
        iconBgColor = Color(0xFFFFF8E1)
    )
)

// ═══════════════════════════════════════════════════════════════════════════════
// Invitar miembro
// ═══════════════════════════════════════════════════════════════════════════════

val inviteMemberTutorialSteps = listOf(
    TutorialStep(
        title = "Invitar miembros 👥",
        description = "Desde aquí puedes compartir tu hogar con familiares o compañeros. Genera un código de invitación y compártelo.",
        icon = Icons.Default.PersonAdd,
        iconColor = Color(0xFF0097A7),
        iconBgColor = Color(0xFFE0F7FA)
    ),
    TutorialStep(
        title = "Código de invitación",
        description = "Tu código es único para tu hogar. Cópialo y envíalo por WhatsApp, mensaje o como prefieras. La otra persona lo ingresa en su app para unirse.",
        icon = Icons.Default.Share,
        iconColor = Color(0xFF1565C0),
        iconBgColor = Color(0xFFE3F2FD)
    ),
    TutorialStep(
        title = "Unirse a otro hogar",
        description = "Si alguien te compartió un código, ingrésalo en la sección inferior para unirte a su hogar. ¡Así de fácil!",
        icon = Icons.Default.GroupAdd,
        iconColor = Color(0xFF2E7D32),
        iconBgColor = Color(0xFFE8F5E9)
    )
)

// ═══════════════════════════════════════════════════════════════════════════════
// Lista de deseos
// ═══════════════════════════════════════════════════════════════════════════════

val wishlistTutorialSteps = listOf(
    TutorialStep(
        title = "Lista de Deseos",
        description = "Aquí registras los artículos que el hogar quiere comprar en el futuro.",
        icon = Icons.Default.Favorite,
        iconColor = Color(0xFFE91E63),
        iconBgColor = Color(0xFFFCE4EC)
    ),
    TutorialStep(
        title = "Agregar deseo",
        description = "Toca + para agregar un nuevo artículo con nombre, costo estimado y prioridad.",
        icon = Icons.Default.Add,
        iconColor = Color(0xFF1565C0),
        iconBgColor = Color(0xFFE3F2FD)
    ),
    TutorialStep(
        title = "Prioridad",
        description = "Clasifica cada artículo como Alta, Media o Baja prioridad para organizar mejor.",
        icon = Icons.Default.Star,
        iconColor = Color(0xFFF57F17),
        iconBgColor = Color(0xFFFFF8E1)
    ),
    TutorialStep(
        title = "Marcar como comprado",
        description = "Cuando adquieras un artículo, márcalo como comprado desde el menú de opciones.",
        icon = Icons.Default.CheckCircle,
        iconColor = Color(0xFF2E7D32),
        iconBgColor = Color(0xFFE8F5E9)
    )
)

// ═══════════════════════════════════════════════════════════════════════════════
// Sobrecarga de compatibilidad: acepta TutorialModule + callbacks
// ═══════════════════════════════════════════════════════════════════════════════

private fun stepsForModule(module: TutorialModule): List<TutorialStep> = when (module) {
    TutorialModule.DASHBOARD -> dashboardTutorialSteps
    TutorialModule.ACCOUNTS -> accountsTutorialSteps
    TutorialModule.TRANSACTIONS -> transactionsTutorialSteps
    TutorialModule.BUDGETS -> budgetsTutorialSteps
    TutorialModule.INVENTORY -> inventoryTutorialSteps
    TutorialModule.RECURRING_BILLS -> recurringBillsTutorialSteps
    TutorialModule.HOUSEHOLD -> householdTutorialSteps
    TutorialModule.INVITE_MEMBER -> inviteMemberTutorialSteps
    TutorialModule.WISHLIST -> wishlistTutorialSteps
}

@Composable
fun TutorialOverlay(
    module: TutorialModule,
    onComplete: () -> Unit,
    onSkip: () -> Unit = onComplete
) {
    TutorialOverlay(
        steps = stepsForModule(module),
        onDismiss = onComplete
    )
}

@Composable
fun TutorialOverlay(
    module: TutorialModule,
    onFinish: () -> Unit
) {
    TutorialOverlay(
        steps = stepsForModule(module),
        onDismiss = onFinish
    )
}
