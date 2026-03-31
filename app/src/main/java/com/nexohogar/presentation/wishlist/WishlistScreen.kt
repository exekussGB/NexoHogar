package com.nexohogar.presentation.wishlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.nexohogar.core.tutorial.TutorialManager
import com.nexohogar.core.tutorial.TutorialModule
import com.nexohogar.domain.model.WishlistItem
import com.nexohogar.presentation.tutorial.TutorialOverlay
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    viewModel: WishlistViewModel,
    tutorialManager: TutorialManager,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))
    var showTutorial by remember {
        mutableStateOf(!tutorialManager.isTutorialCompleted(TutorialModule.WISHLIST))
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
            FloatingActionButton(
                onClick = { viewModel.onShowCreateDialog() }
            ) {
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

                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (pending.isNotEmpty()) {
                            item {
                                Text(
                                    "Pendientes",
                                    style      = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.primary,
                                    modifier   = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            items(pending) { item ->
                                WishlistItemCard(
                                    item          = item,
                                    format        = clpFormat,
                                    onEdit        = { viewModel.onShowEditDialog(item) },
                                    onMarkBought  = { viewModel.markAsPurchased(item) },
                                    onDelete      = { viewModel.deleteItem(item) }
                                )
                            }
                        }

                        if (purchased.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Comprados",
                                    style      = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier   = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            items(purchased) { item ->
                                WishlistItemCard(
                                    item         = item,
                                    format       = clpFormat,
                                    onEdit       = {},
                                    onMarkBought = {},
                                    onDelete     = { viewModel.deleteItem(item) }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // ── Diálogo de creación ──────────────────────────────────────────────────
    if (uiState.showCreateDialog) {
        WishlistItemDialog(
            title       = "Agregar deseo",
            isLoading   = uiState.isCreating,
            error       = uiState.createError,
            onDismiss   = { viewModel.onDismissCreateDialog() },
            onConfirm   = { name, cost, notes, priority ->
                viewModel.createItem(name, cost, notes, priority)
            }
        )
    }

    // ── Diálogo de edición ──────────────────────────────────────────────────
    val itemToEdit = uiState.itemToEdit
    if (itemToEdit != null) {
        WishlistItemDialog(
            title         = "Editar deseo",
            initialName   = itemToEdit.name,
            initialCost   = itemToEdit.estimatedCost,
            initialNotes  = itemToEdit.notes ?: "",
            initialPriority = itemToEdit.priority,
            isLoading     = uiState.isEditing,
            error         = uiState.editError,
            onDismiss     = { viewModel.onDismissEditDialog() },
            onConfirm     = { name, cost, notes, priority ->
                viewModel.updateItem(itemToEdit.id, name, cost, notes, priority)
            }
        )
    }

    // ── Tutorial ─────────────────────────────────────────────────────────────
    if (showTutorial) {
        TutorialOverlay(
            module    = TutorialModule.WISHLIST,
            onFinish  = {
                tutorialManager.markTutorialCompleted(TutorialModule.WISHLIST)
                showTutorial = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WishlistItemCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WishlistItemCard(
    item        : WishlistItem,
    format      : NumberFormat,
    onEdit      : () -> Unit,
    onMarkBought: () -> Unit,
    onDelete    : () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val priorityColor = when (item.priority) {
        1 -> Color(0xFFD32F2F) // rojo alta
        2 -> Color(0xFFF57F17) // naranja media
        else -> Color(0xFF388E3C)  // verde baja
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de prioridad
            Surface(
                shape = MaterialTheme.shapes.small,
                color = priorityColor.copy(alpha = 0.1f),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text     = item.priorityLabel,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = priorityColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = item.name,
                    style = MaterialTheme.typography.bodyLarge.let {
                        if (item.isPurchased) it.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else it
                    },
                    fontWeight = FontWeight.Medium
                )
                if (item.estimatedCost > 0) {
                    Text(
                        text  = format.format(item.estimatedCost),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!item.notes.isNullOrBlank()) {
                    Text(
                        text  = item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!item.isPurchased) {
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                    }
                    DropdownMenu(
                        expanded         = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text        = { Text("Editar") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick     = { expanded = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text        = { Text("Marcar como comprado") },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                            onClick     = { expanded = false; onMarkBought() }
                        )
                        DropdownMenuItem(
                            text        = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick     = { expanded = false; onDelete() }
                        )
                    }
                }
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Comprado",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WishlistItemDialog (Create / Edit)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WishlistItemDialog(
    title          : String,
    initialName    : String  = "",
    initialCost    : Long    = 0L,
    initialNotes   : String  = "",
    initialPriority: Int     = 2,
    isLoading      : Boolean,
    error          : String?,
    onDismiss      : () -> Unit,
    onConfirm      : (String, Long, String?, Int) -> Unit
) {
    var name        by remember { mutableStateOf(initialName) }
    var costText    by remember { mutableStateOf(if (initialCost > 0) initialCost.toString() else "") }
    var notes       by remember { mutableStateOf(initialNotes) }
    var priority    by remember { mutableStateOf(initialPriority) }

    val priorityOptions = listOf(1 to "Alta", 2 to "Media", 3 to "Baja")

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
                    value         = costText,
                    onValueChange = { costText = it.filter { c -> c.isDigit() } },
                    label         = { Text("Costo estimado (CLP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
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
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = {
                    val cost = costText.toLongOrNull() ?: 0L
                    onConfirm(name.trim(), cost, notes.trim().ifBlank { null }, priority)
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
