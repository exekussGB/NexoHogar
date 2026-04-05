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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexohogar.core.util.DateFormatter
import com.nexohogar.domain.model.Transaction
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.util.*
import androidx.compose.ui.platform.testTag
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
    onAddTransactionClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var showTutorial by remember {
        mutableStateOf(!tutorialManager.isTutorialCompleted(TutorialModule.TRANSACTIONS))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Movimientos") },
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
            // ── Filter chips ────────────────────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            onLoadMore = { viewModel.loadMoreTransactions() }
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
    onLoadMore: () -> Unit
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
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onItemClick(transaction)
                            false  // no descartar visualmente, solo navegar
                        } else false
                    },
                    positionalThreshold = { it * 0.5f }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color by animateColorAsState(
                            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                                Color(0xFF1565C0) else Color.Transparent,
                            label = "swipe_tx_color"
                        )
                        Box(
                            Modifier.fillMaxSize().background(color, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Ver detalle",
                                tint = Color.White, modifier = Modifier.padding(end = 16.dp))
                        }
                    },
                    enableDismissFromStartToEnd = false,
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
