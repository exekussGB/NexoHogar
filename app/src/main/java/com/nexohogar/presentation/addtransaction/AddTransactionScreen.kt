package com.nexohogar.presentation.addtransaction

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.Category
import com.nexohogar.presentation.components.LoadingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionType: String,
    viewModel: AddTransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.filteredCategories.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(key1 = transactionType) {
        val type = when (transactionType) {
            "income" -> TransactionType.INCOME
            "transfer" -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }
        viewModel.onTypeChange(type)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "Movimiento guardado con éxito", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val title = when (uiState.type) {
        TransactionType.INCOME -> "Nuevo Ingreso"
        TransactionType.EXPENSE -> "Nuevo Gasto"
        TransactionType.TRANSFER -> "Traspaso entre Cuentas"
    }

    val accountLabel = when (uiState.type) {
        TransactionType.INCOME -> "Cuenta Destino"
        else -> "Cuenta Origen"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.paymentAccounts.isEmpty() && !uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Crea una cuenta antes de agregar movimientos.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cuenta Origen / Pago
                    AccountDropdown(
                        label = accountLabel,
                        accounts = uiState.paymentAccounts,
                        selectedAccount = uiState.selectedPaymentAccount,
                        onAccountSelected = { viewModel.onPaymentAccountSelected(it) }
                    )

                    // Mostrar Cuenta Destino si es transferencia
                    if (uiState.type == TransactionType.TRANSFER) {
                        AccountDropdown(
                            label = "Cuenta Destino",
                            accounts = uiState.paymentAccounts,
                            selectedAccount = uiState.selectedToAccount,
                            onAccountSelected = { viewModel.onToAccountSelected(it) }
                        )
                    }

                    // Mostrar Categoría si NO es transferencia
                    if (uiState.type != TransactionType.TRANSFER) {
                        CategoryDropdown(
                            label = "Categoría",
                            categories = categories,
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = { viewModel.onCategorySelected(it) }
                        )
                    }

                    OutlinedTextField(
                        value = uiState.amount,
                        onValueChange = { viewModel.onAmountChange(it) },
                        label = { Text("Monto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.onDescriptionChange(it) },
                        label = { Text("Descripción (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    val isButtonEnabled = remember(uiState) {
                        val common = uiState.selectedPaymentAccount != null && uiState.amount.isNotBlank()
                        if (uiState.type == TransactionType.TRANSFER) {
                            common && uiState.selectedToAccount != null
                        } else {
                            common && uiState.selectedCategory != null
                        }
                    }

                    Button(
                        onClick = { viewModel.saveTransaction() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && isButtonEnabled
                    ) {
                        Text("Guardar Movimiento")
                    }
                }
            }

            if (uiState.isLoading) {
                LoadingOverlay()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDropdown(
    label: String,
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedAccount?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = {
                        onAccountSelected(account)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    label: String,
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCategory?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(category.name)
                            Text(
                                text = when (category.type) {
                                    "expense" -> "Gasto"
                                    "income" -> "Ingreso"
                                    else -> category.type
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}
