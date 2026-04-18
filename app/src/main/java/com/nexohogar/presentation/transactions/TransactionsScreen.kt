package com.nexohogar.presentation.transactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexohogar.core.util.DateFormatter
import com.nexohogar.domain.model.Transaction
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.nexohogar.core.tutorial.TutorialManager
import com.nexohogar.core.tutorial.TutorialModule
import com.nexohogar.presentation.tutorial.TutorialOverlay
import androidx.compose.runtime.snapshotFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    tutorialManager: TutorialManager,
    onTransactionClick: (Transaction) -> Unit,
    onAddTransactionClick: () -> Unit,
    isSuperUser: Boolean = false,
    onEditTransaction: (Transaction) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()

    // Refrescar al volver a la pantalla
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.applyFilter()
    }

    var showTutorial by remember {
        mutableStateOf(!tutorialManager.isTutorialCompleted(TutorialModule.TRANSACTIONS))
    }

    // ── Date Range Picker Dialog ───────────────────────────────────────────
    var showDatePicker by remember { mutableStateOf(false) }

    // BUG FIX 1b: Estado para navegación de meses con flechas
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance()) }

    if (showDatePicker) {
        // BUG FIX 1: Configurar DateRangePicker con navegación sin límites
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = (dateRange?.first ?: Calendar.getInstance().apply {
                add(Calendar.YEAR, -1)
            }.timeInMillis),
            initialSelectedEndDateMillis = (dateRange?.second ?: Calendar.getInstance().timeInMillis),
            yearRange = IntRange(2020, Calendar.getInstance().get(Calendar.YEAR) + 1)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            viewModel.setDateFilter(start, end)
                        }
                        showDatePicker = false
                    }
                ) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.height(500.dp),
                title = { Text("Selecciona un rango de fechas", modifier = Modifier.padding(16.dp)) }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Movimientos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        // FAB removido — el botón "+" ahora está en la BottomNavBar global
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Filter chips + calendar button ────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("transactions_filters"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        TransactionFilter.ALL to "Todos",
                        TransactionFilter.EXPENSE to "Gastos",
                        TransactionFilter.INCOME to "Ingresos",
                        TransactionFilter.TRANSFER to "Transferencias"
                    )
                    items(filters) { (filter, label) ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(label) }
                        )
                    }
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Filtrar por fecha")
                }
            }

            // ── Month Navigator with arrows ─────────────────────────────────────
            // BUG FIX 1b: Navegador de meses con flechas para filtrar por mes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Flecha izquierda: mes anterior
                IconButton(
                    onClick = {
                        selectedMonth.add(Calendar.MONTH, -1)
                        val startOfMonth = selectedMonth.apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }.timeInMillis
                        val endOfMonth = selectedMonth.apply {
                            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                        }.timeInMillis
                        viewModel.setDateFilter(startOfMonth, endOfMonth)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Mes anterior", modifier = Modifier.size(20.dp))
                }

                // Mes y año actual
                val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "CL"))
                Text(
                    text = monthFormat.format(selectedMonth.time).replaceFirstChar { it.uppercase() },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Flecha derecha: mes siguiente
                IconButton(
                    onClick = {
                        selectedMonth.add(Calendar.MONTH, 1)
                        val startOfMonth = selectedMonth.apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }.timeInMillis
                        val endOfMonth = selectedMonth.apply {
                            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                        }.timeInMillis
                        viewModel.setDateFilter(startOfMonth, endOfMonth)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Mes siguiente", modifier = Modifier.size(20.dp))
                }
            }

            // Chip para mostrar rango actual y limpiar filtro
            if (dateRange != null) {
                val format = SimpleDateFormat("dd MMM", Locale("es", "CL"))
                AssistChip(
                    onClick = {
                        viewModel.clearDateFilter()
                        selectedMonth = Calendar.getInstance()
                    },
                    label = { Text("${format.format(Date(dateRange!!.first))} – ${format.format(Date(dateRange!!.second))}") },
                    trailingIcon = { Icon(Icons.Default.Close, "Quitar filtro", modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // ── Content ─────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is TransactionsUiState.Loading -> {
                        LoadingOverlay()
                    }
                    is TransactionsUiState.Success -> {
                        val isLoadingMore by viewModel.isLoadingMore.collectAsState()
                        TransactionsList(
                            transactions = state.transactions,
                            hasMoreData = state.hasMoreData,
                            isLoadingMore = isLoadingMore,
                            onItemClick = onTransactionClick,
                            onLoadMore = { viewModel.loadMoreTransactions() },
                            isSuperUser = isSuperUser,
                            onEditItem = onEditTransaction
                        )
                    }
                    is TransactionsUiState.Error -> {
                        ErrorState(state.message) {
                            viewModel.loadTransactions()
                        }
                    }
                }
            }
        }
        // ── Tutorial overlay ────────────────────────────────────────────────────
        if (showTutorial) {
            TutorialOverlay(
                module = TutorialModule.TRANSACTIONS,
                onComplete = {
                    tutorialManager.markTutorialCompleted(TutorialModule.TRANSACTIONS)
                    showTutorial = false
                },
                onSkip = {
                    tutorialManager.markTutorialCompleted(TutorialModule.TRANSACTIONS)
                    showTutorial = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsList(
    transactions: List<Transaction>,
    hasMoreData: Boolean,
    isLoadingMore: Boolean,
    onItemClick: (Transaction) -> Unit,
    onLoadMore: () -> Unit,
    isSuperUser: Boolean = false,
    onEditItem: (Transaction) -> Unit = {}
) {
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No se encontraron movimientos.")
        }
    } else {
        val listState = rememberLazyListState()

        // Detectar cuando llega al final para cargar más
        LaunchedEffect(listState) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                .collect { lastIndex ->
                    if (lastIndex != null && lastIndex >= transactions.size - 3 && hasMoreData && !isLoadingMore) {
                        onLoadMore()
                    }
                }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("transactions_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(transactions) { transaction ->
                val haptic = LocalHapticFeedback.current
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        when (value) {
                            // ← Swipe izquierda: ver detalle
                            SwipeToDismissBoxValue.EndToStart -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onItemClick(transaction)
                                false  // no descartar visualmente, solo navegar
                            }
                            // → Swipe derecha: editar (todos los usuarios)
                            SwipeToDismissBoxValue.StartToEnd -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEditItem(transaction)
                                false
                            }
                            else -> false
                        }
                    },
                    positionalThreshold = { it * 0.3f }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val direction = dismissState.dismissDirection
                        // ← Swipe izquierda: azul (detalle)
                        // → Swipe derecha: naranja (editar) — solo si es super user
                        val color by animateColorAsState(
                            when (direction) {
                                SwipeToDismissBoxValue.EndToStart -> Color(0xFF1565C0)
                                SwipeToDismissBoxValue.StartToEnd -> Color(0xFFE65100)
                                else -> Color.Transparent
                            },
                            label = "swipe_tx_color"
                        )
                        Box(
                            Modifier.fillMaxSize().background(color, RoundedCornerShape(8.dp))
                        ) {
                            // Ícono derecho: ver detalle
                            if (direction == SwipeToDismissBoxValue.EndToStart) {
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = "Ver detalle",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 16.dp)
                                )
                            }
                            // Ícono izquierdo: editar
                            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 16.dp)
                                )
                            }
                        }
                    },
                    enableDismissFromStartToEnd = true,  // Todos pueden editar con swipe derecha
                    enableDismissFromEndToStart = true
                ) {
                    TransactionItem(transaction, onItemClick)
                }
            }
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    onItemClick: (Transaction) -> Unit
) {
    val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    val icon = when (transaction.type.lowercase()) {
        "expense"  -> Icons.Default.ArrowUpward
        "transfer" -> Icons.Default.SwapHoriz
        else       -> Icons.Default.ArrowDownward
    }

    val iconColor = when (transaction.type.lowercase()) {
        "expense"  -> Color(0xFFF44336)
        "transfer" -> Color(0xFF2196F3)
        else       -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick(transaction) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description?.ifBlank { "Sin descripción" } ?: "Sin descripción",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = DateFormatter.formatForDisplay(transaction.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                transaction.createdByName?.let { name ->
                    Text(
                        text = "por $name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = clpFormat.format(transaction.amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}
