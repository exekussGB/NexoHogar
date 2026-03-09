package com.nexohogar.presentation.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexohogar.domain.model.Transaction
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onTransactionClick: (Transaction) -> Unit,
    onAddTransactionClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransactionClick) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Movimiento")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is TransactionsUiState.Loading -> {
                    LoadingOverlay()
                }
                is TransactionsUiState.Success -> {
                    TransactionsList(state.transactions, onTransactionClick)
                }
                is TransactionsUiState.Error -> {
                    ErrorState(state.message) {
                        viewModel.loadTransactions()
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionsList(
    transactions: List<Transaction>,
    onItemClick: (Transaction) -> Unit
) {
    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No se encontraron movimientos.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(transactions) { transaction ->
                TransactionItem(transaction, onItemClick)
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
    
    val icon = if (transaction.amount < 0) {
        Icons.Default.ArrowDownward
    } else {
        Icons.Default.ArrowUpward
    }
    
    val iconColor = if (transaction.amount < 0) {
        Color(0xFFF44336)
    } else {
        Color(0xFF4CAF50)
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
                    text = transaction.createdAt.ifBlank { "Fecha no disponible" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
