package com.nexohogar.presentation.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.core.tutorial.TutorialManager
import com.nexohogar.core.tutorial.TutorialModule
import com.nexohogar.domain.model.WishlistItem
import com.nexohogar.presentation.tutorial.TutorialOverlay
import com.nexohogar.presentation.tutorial.TutorialStep
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    viewModel: WishlistViewModel,
    tutorialManager: TutorialManager,
    availableBalance: Double = 0.0,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))
    var showTutorial by remember {
        mutableStateOf(!tutorialManager.isTutorialCompleted(TutorialModule.WISHLIST))
    }

    val wishlistTutorialSteps = remember {
        listOf(
            TutorialStep(
                title = "Lista de Deseos",
                description = "Registra artículos que quieres comprar para tu hogar. Toca + para agregar uno.",
                icon = Icons.Default.Favorite,
                iconColor = Color(0xFFD32F2F),
                iconBgColor = Color(0xFFFFEBEE)
            ),
            TutorialStep(
                title = "Organizado por prioridad",
                description = "Los artículos se agrupan en Alta, Media y Baja prioridad para que compres primero lo más importante.",
                icon = Icons.Default.Star,
                iconColor = Color(0xFFF57F17),
                iconBgColor = Color(0xFFFFF3E0)
            ),
            TutorialStep(
                title = "Acciones rápidas",
                description = "Toca los 3 puntos de cualquier artículo para editarlo, marcarlo como comprado o eliminarlo.",
                icon = Icons.Default.MoreVert,
                iconColor = Color(0xFF1565C0),
                iconBgColor = Color(0xFFE3F2FD)
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Deseos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showTutorial = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Ayuda")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onShowCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar deseo")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadItems() }) { Text("Reintentar") }
                    }
                }
                uiState.items.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No hay deseos todavía",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Toca + para agregar algo que quieras comprar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    val pending   = uiState.items.filter { !it.isPurchased }
                    val purchased = uiState.items.filter {  it.isPurchased }

                    val highItems   = pending.filter { it.priority == "HIGH" }
                    val mediumItems = pending.filter { it.priority == "MEDIUM" }
                    val lowItems    = pending.filter { it.priority != "HIGH" && it.priority != "MEDIUM" }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 92.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement   = Arrangement.spacedBy(6.dp)
                    ) {
                        // ── Alta Prioridad ───────────────────────────────────
                        if (highItems.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                PriorityHeader(label = "🔴  Alta Prioridad", color = Color(0xFFD32F2F))
                            }
                            items(highItems) { item ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.markAsPurchased(item)
                                            true
                                        } else false
                                    },
                                    positionalThreshold = { it * 0.4f }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color by animateColorAsState(
                                            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                                                Color(0xFF4CAF50) else Color.Transparent,
                                            label = "swipe_color"
                                        )
                                        Box(
                                            Modifier.fillMaxSize().background(color, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Comprado",
                                                tint = Color.White, modifier = Modifier.padding(end = 16.dp))
                                        }
                                    },
                                    enableDismissFromStartToEnd = false,
                                    enableDismissFromEndToStart = true
                                ) {
                                    WishlistSquareCard(
                                        item             = item,
                                        format           = clpFormat,
                                        availableBalance = availableBalance,
                                        onEdit           = { viewModel.onShowEditDialog(item) },
                                        onMarkBought     = { viewModel.markAsPurchased(item) },
                                        onDelete         = { viewModel.deleteItem(item) }
                                    )
                                }
                            }
                        }

                        // ── Media Prioridad ──────────────────────────────────
                        if (mediumItems.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                PriorityHeader(label = "🟠  Media Prioridad", color = Color(0xFFF57F17))
                            }
                            items(mediumItems) { item ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.markAsPurchased(item)
                                            true
                                        } else false
                                    },
                                    positionalThreshold = { it * 0.4f }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color by animateColorAsState(
                                            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                                                Color(0xFF4CAF50) else Color.Transparent,
                                            label = "swipe_color"
                                        )
                                        Box(
                                            Modifier.fillMaxSize().background(color, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Comprado",
                                                tint = Color.White, modifier = Modifier.padding(end = 16.dp))
                                        }
                                    },
                                    enableDismissFromStartToEnd = false,
                                    enableDismissFromEndToStart = true
                                ) {
                                    WishlistSquareCard(
                                        item             = item,
                                        format           = clpFormat,
                                        availableBalance = availableBalance,
                                        onEdit           = { viewModel.onShowEditDialog(item) },
                                        onMarkBought     = { viewModel.markAsPurchased(item) },
                                        onDelete         = { viewModel.deleteItem(item) }
                                    )
                                }
                            }
                        }

                        // ── Baja Prioridad ───────────────────────────────────
                        if (lowItems.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                PriorityHeader(label = "🟢  Baja Prioridad", color = Color(0xFF388E3C))
                            }
                            items(lowItems) { item ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.markAsPurchased(item)
                                            true
                                        } else false
                                    },
                                    positionalThreshold = { it * 0.4f }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color by animateColorAsState(
                                            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                                                Color(0xFF4CAF50) else Color.Transparent,
                                            label = "swipe_color"
                                        )
                                        Box(
                                            Modifier.fillMaxSize().background(color, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Comprado",
                                                tint = Color.White, modifier = Modifier.padding(end = 16.dp))
                                        }
                                    },
                                    enableDismissFromStartToEnd = false,
                                    enableDismissFromEndToStart = true
                                ) {
                                    WishlistSquareCard(
                                        item             = item,
                                        format           = clpFormat,
                                        availableBalance = availableBalance,
                                        onEdit           = { viewModel.onShowEditDialog(item) },
                                        onMarkBought     = { viewModel.markAsPurchased(item) },
                                        onDelete         = { viewModel.deleteItem(item) }
                                    )
                                }
                            }
                        }

                        // ── Comprados ────────────────────────────────────────
                        if (purchased.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                PriorityHeader(label = "✓  Comprados", color = Color(0xFF757575))
                            }
                            items(purchased) { item ->
                                WishlistSquareCard(
                                    item             = item,
                                    format           = clpFormat,
                                    availableBalance = 0.0,
                                    onEdit           = {},
                                    onMarkBought     = {},
                                    onDelete         = { viewModel.deleteItem(item) }
                                )
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    // ── Diálogo de creación ──────────────────────────────────────────────────
    if (uiState.showCreateDialog) {
        WishlistItemDialog(
            title     = "Agregar deseo",
            isLoading = uiState.isCreating,
            error     = uiState.createError,
            onDismiss = { viewModel.onDismissCreateDialog() },
            onConfirm = { name, price, description, priority ->
                viewModel.createItem(name, price, description, priority)
            }
        )
    }

    // ── Diálogo de edición ───────────────────────────────────────────────────
    val itemToEdit = uiState.itemToEdit
    if (itemToEdit != null) {
        WishlistItemDialog(
            title           = "Editar deseo",
            initialName     = itemToEdit.name,
            initialPrice    = itemToEdit.price,
            initialDesc     = itemToEdit.description ?: "",
            initialPriority = itemToEdit.priority,
            isLoading       = uiState.isEditing,
            error           = uiState.editError,
            onDismiss       = { viewModel.onDismissEditDialog() },
            onConfirm       = { name, price, description, priority ->
                viewModel.updateItem(itemToEdit.id, name, price, description, priority)
            }
        )
    }

    // ── Tutorial ─────────────────────────────────────────────────────────────
    if (showTutorial) {
        TutorialOverlay(
            steps     = wishlistTutorialSteps,
            onDismiss = {
                tutorialManager.markTutorialCompleted(TutorialModule.WISHLIST)
                showTutorial = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Encabezado de sección por prioridad
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PriorityHeader(label: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text       = label,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = color
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card cuadrada pequeña para cada deseo
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WishlistSquareCard(
    item            : WishlistItem,
    format          : NumberFormat,
    availableBalance: Double = 0.0,
    onEdit          : () -> Unit,
    onMarkBought: () -> Unit,
    onDelete    : () -> Unit
) {
    val priorityColor = when (item.priority) {
        "HIGH"   -> Color(0xFFD32F2F)
        "MEDIUM" -> Color(0xFFF57F17)
        else     -> Color(0xFF388E3C)
    }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier
            .fillMaxWidth(),
        shape     = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (item.isPurchased)
                Color(0xFFF5F5F5)
            else
                Color.White
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {

            // ── Barra de prioridad (top) ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        if (item.isPurchased) Color(0xFFBDBDBD) else priorityColor,
                        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .align(Alignment.TopCenter)
            )

            // ── Contenido ────────────────────────────────────────────────────
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 5.dp)
            ) {
                // Espacio para la barra de color
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text     = item.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color    = if (item.isPurchased) Color(0xFF9E9E9E) else Color(0xFF212121),
                )

                val price = item.price
                if (price != null && price > 0) {
                    Text(
                        text  = format.format(price.toLong()),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (item.isPurchased)
                            Color(0xFF9E9E9E)
                        else
                            MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Chip "💰 Alcanza!" para alta prioridad asequibles
                if (!item.isPurchased && item.priority == "HIGH" &&
                    price != null && price > 0 && availableBalance >= price) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "💰 Alcanza!",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.height(18.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.14f)
                        )
                    )
                }
            }

            // ── Menú de opciones / ícono comprado ────────────────────────────
            if (item.isPurchased) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Comprado",
                    tint     = Color(0xFF4CAF50).copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(11.dp)
                )
            } else {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick  = { expanded = true },
                        modifier = Modifier.size(21.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            modifier = Modifier.size(11.dp),
                            tint     = Color(0xFF757575)
                        )
                    }
                    DropdownMenu(
                        expanded         = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text        = { Text("Editar", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(16.dp)) },
                            onClick     = { expanded = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text        = { Text("Marcar comprado", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp)) },
                            onClick     = { expanded = false; onMarkBought() }
                        )
                        DropdownMenuItem(
                            text        = { Text("Eliminar", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) },
                            onClick     = { expanded = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Diálogo de creación / edición
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WishlistItemDialog(
    title           : String,
    initialName     : String  = "",
    initialPrice    : Double? = null,
    initialDesc     : String  = "",
    initialPriority : String  = "MEDIUM",
    isLoading       : Boolean,
    error           : String?,
    onDismiss       : () -> Unit,
    onConfirm       : (String, Double?, String?, String) -> Unit
) {
    var name      by remember { mutableStateOf(initialName) }
    var priceText by remember { mutableStateOf(
        if (initialPrice != null && initialPrice > 0) initialPrice.toLong().toString() else ""
    ) }
    var desc      by remember { mutableStateOf(initialDesc) }
    var priority  by remember { mutableStateOf(initialPriority) }

    val priorityOptions = listOf("HIGH" to "Alta", "MEDIUM" to "Media", "LOW" to "Baja")

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text(title) },
        text    = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Nombre *") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value           = priceText,
                    onValueChange   = { priceText = it.filter { c -> c.isDigit() } },
                    label           = { Text("Costo estimado (CLP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = desc,
                    onValueChange = { desc = it },
                    label         = { Text("Notas (opcional)") },
                    maxLines      = 2,
                    modifier      = Modifier.fillMaxWidth()
                )

                Text("Prioridad", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorityOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = priority == value,
                            onClick  = { priority = value },
                            label    = { Text(label) }
                        )
                    }
                }

                if (error != null) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = {
                    val price = priceText.toLongOrNull()?.toDouble()
                    onConfirm(name.trim(), price, desc.trim().ifBlank { null }, priority)
                },
                enabled  = name.isNotBlank() && !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
