package com.nexohogar.presentation.addmovement

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.domain.model.Account
import com.nexohogar.domain.model.Category
import com.nexohogar.domain.model.RecurringBill
import com.nexohogar.presentation.addmovement.AddMovementViewModel
import com.nexohogar.presentation.addmovement.TransactionType
import com.nexohogar.presentation.components.LoadingOverlay
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionType: String,
    viewModel: AddMovementViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState    by viewModel.uiState.collectAsState()
    val categories by viewModel.filteredCategories.collectAsState()
    val context    = LocalContext.current

    // Auto-focus on amount field
    val amountFocusRequester = remember { FocusRequester() }

    LaunchedEffect(key1 = transactionType) {
        val type = when (transactionType) {
            "income"   -> TransactionType.INCOME
            "transfer" -> TransactionType.TRANSFER
            else       -> TransactionType.EXPENSE
        }
        viewModel.onTypeChange(type)
    }

    // Request focus on amount field after short delay
    LaunchedEffect(Unit) {
        delay(300)
        try {
            amountFocusRequester.requestFocus()
        } catch (_: Exception) { }
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
        TransactionType.INCOME   -> "Nuevo Ingreso"
        TransactionType.EXPENSE  -> "Nuevo Gasto"
        TransactionType.TRANSFER -> "Traspaso entre Cuentas"
    }

    val accountLabel = when (uiState.type) {
        TransactionType.INCOME -> "Cuenta Destino"
        else                   -> "Cuenta Origen"
    }

    // ── Diálogo de nueva categoría ───────────────────────────────────────────
    if (uiState.showCreateCategoryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissCreateCategoryDialog() },
            title = { Text("Nueva Categoría") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text  = "Tipo: ${if (uiState.type == TransactionType.INCOME) "Ingreso" else "Gasto"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value         = uiState.newCategoryName,
                        onValueChange = { viewModel.onNewCategoryNameChange(it) },
                        label         = { Text("Nombre de la categoría") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick  = { viewModel.createCategory() },
                    enabled  = uiState.newCategoryName.isNotBlank() && !uiState.isSavingCategory
                ) {
                    if (uiState.isSavingCategory) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Crear")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissCreateCategoryDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ── Scaffold principal ───────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.accounts.isEmpty() && !uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text  = "Crea una cuenta antes de agregar movimientos.",
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
                    // Cuenta Origen / Destino
                    AccountDropdown(
                        label             = accountLabel,
                        accounts          = uiState.accounts,
                        selectedAccount   = uiState.selectedFromAccount,
                        onAccountSelected = { viewModel.onFromAccountSelected(it) }
                    )

                    // Segunda cuenta si es transferencia
                    if (uiState.type == TransactionType.TRANSFER) {
                        AccountDropdown(
                            label             = "Cuenta Destino",
                            accounts          = uiState.accounts,
                            selectedAccount   = uiState.selectedToAccount,
                            onAccountSelected = { viewModel.onToAccountSelected(it) }
                        )
                    }

                    // Categoría (solo income/expense)
                    if (uiState.type != TransactionType.TRANSFER) {
                        CategoryDropdown(
                            label              = "Categoría",
                            categories         = categories,
                            selectedCategory   = uiState.selectedCategory,
                            onCategorySelected = { viewModel.onCategorySelected(it) },
                            onCreateCategory   = { viewModel.onShowCreateCategoryDialog() }
                        )
                    }

                    // ── Enlazar cuenta recurrente (solo para gastos con cuentas pendientes) ──
                    if (uiState.type == TransactionType.EXPENSE && uiState.recurringBills.isNotEmpty()) {
                        RecurringBillDropdown(
                            bills              = uiState.recurringBills,
                            selectedBill       = uiState.linkedRecurringBill,
                            onBillSelected     = { viewModel.onRecurringBillSelected(it) },
                            onClearSelection   = { viewModel.onRecurringBillSelected(null) }
                        )
                    }

                    OutlinedTextField(
                        value         = uiState.amount,
                        onValueChange = { newValue ->
                            // Validate decimal format: digits, optional dot, up to 2 decimals
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                viewModel.onAmountChange(newValue)
                            }
                        },
                        label         = { Text("Monto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier      = Modifier
                            .fillMaxWidth()
                            .focusRequester(amountFocusRequester)
                    )

                    OutlinedTextField(
                        value         = uiState.description,
                        onValueChange = { if (it.length <= 200) viewModel.onDescriptionChange(it) },
                        label         = { Text("Descripción (opcional)") },
                        modifier      = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(
                                text = "${uiState.description.length}/200",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.description.length >= 190)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    val isButtonEnabled = remember(uiState) {
                        val common = uiState.selectedFromAccount != null && uiState.amount.isNotBlank()
                        if (uiState.type == TransactionType.TRANSFER) {
                            common && uiState.selectedToAccount != null
                        } else {
                            common && uiState.selectedCategory != null
                        }
                    }

                    Button(
                        onClick  = { viewModel.saveTransaction() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled  = !uiState.isLoading && isButtonEnabled
                    ) {
                        Text("Guardar Movimiento")
                    }
                }
            }

            if (uiState.isLoading) LoadingOverlay()
        }
    }
}

// ── AccountDropdown ──────────────────────────────────────────────────────────

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
        expanded         = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier         = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value            = selectedAccount?.name ?: "",
            onValueChange    = {},
            readOnly         = true,
            label            = { Text(label) },
            trailingIcon     = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors           = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier         = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text    = { Text(account.name) },
                    onClick = {
                        onAccountSelected(account)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ── CategoryDropdown ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    label: String,
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    onCreateCategory: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded         = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier         = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value            = selectedCategory?.name ?: "",
            onValueChange    = {},
            readOnly         = true,
            label            = { Text(label) },
            trailingIcon     = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors           = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier         = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Opción para crear nueva categoría
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Add,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text  = "Nueva categoría…",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onCreateCategory()
                }
            )
            HorizontalDivider()
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!category.icon.isNullOrBlank()) {
                                Text(category.icon, fontSize = 16.sp)
                            }
                            Column {
                                Text(category.name)
                                Text(
                                    text  = when (category.type) {
                                        "expense" -> "Gasto"
                                        "income"  -> "Ingreso"
                                        else      -> category.type
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
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

// ── RecurringBillDropdown ────────────────────────────────────────────────────
//
// Aparece solo si hay cuentas recurrentes pendientes de pago y el tipo es EXPENSE.
// Permite al usuario vincular este gasto a una cuenta recurrente para marcarla
// automáticamente como pagada al guardar.
//

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillDropdown(
    bills: List<RecurringBill>,
    selectedBill: RecurringBill?,
    onBillSelected: (RecurringBill) -> Unit,
    onClearSelection: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text  = "¿Corresponde a una cuenta recurrente?",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded         = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier         = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value            = selectedBill?.name ?: "No vincular",
                onValueChange    = {},
                readOnly         = true,
                label            = { Text("Cuenta recurrente (opcional)") },
                trailingIcon = {
                    if (selectedBill != null) {
                        IconButton(onClick = { onClearSelection(); expanded = false }) {
                            Icon(
                                imageVector        = Icons.Default.Close,
                                contentDescription = "Quitar vínculo",
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                colors           = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier         = Modifier.menuAnchor().fillMaxWidth(),
                supportingText   = if (selectedBill != null) {
                    { Text("✓ Se marcará como pagada al guardar") }
                } else null
            )
            ExposedDropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Opción "no vincular"
                DropdownMenuItem(
                    text    = { Text("No vincular", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = { onClearSelection(); expanded = false }
                )
                HorizontalDivider()
                bills.forEach { bill ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(bill.name)
                                Text(
                                    text  = "Vence día ${bill.dueDayOfMonth} · ${"$%,d".format(bill.amountClp)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onBillSelected(bill)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
