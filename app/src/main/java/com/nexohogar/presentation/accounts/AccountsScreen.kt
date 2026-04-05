package com.nexohogar.presentation.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.core.util.DateFormatter
import com.nexohogar.domain.model.AccountBalance
import com.nexohogar.domain.model.Transaction
import com.nexohogar.presentation.components.LoadingOverlay
import java.text.NumberFormat
import java.util.*
import com.nexohogar.core.tutorial.TutorialManager
import com.nexohogar.core.tutorial.TutorialModule
import com.nexohogar.presentation.tutorial.TutorialOverlay

// ---------------------------------------------------------------------------
// Account icon option for the icon picker
// ---------------------------------------------------------------------------

data class AccountIconOption(
    val name: String,
    val icon: ImageVector
)

val availableAccountIcons: List<AccountIconOption> = listOf(
    // Finance
    AccountIconOption("Wallet", Icons.Default.Wallet),
    AccountIconOption("CreditCard", Icons.Default.CreditCard),
    AccountIconOption("AccountBalance", Icons.Default.AccountBalance),
    AccountIconOption("AccountBalanceWallet", Icons.Default.AccountBalanceWallet),
    AccountIconOption("Savings", Icons.Default.Savings),
    AccountIconOption("AttachMoney", Icons.Default.AttachMoney),
    AccountIconOption("Payments", Icons.Default.Payments),
    // Home
    AccountIconOption("Home", Icons.Default.Home),
    AccountIconOption("Kitchen", Icons.Default.Kitchen),
    AccountIconOption("Bed", Icons.Default.Bed),
    AccountIconOption("Bathtub", Icons.Default.Bathtub),
    AccountIconOption("Chair", Icons.Default.Chair),
    AccountIconOption("Garage", Icons.Default.Garage),
    // Transport
    AccountIconOption("DirectionsCar", Icons.Default.DirectionsCar),
    AccountIconOption("TwoWheeler", Icons.Default.TwoWheeler),
    AccountIconOption("Flight", Icons.Default.Flight),
    AccountIconOption("Train", Icons.Default.Train),
    // Shopping
    AccountIconOption("ShoppingCart", Icons.Default.ShoppingCart),
    AccountIconOption("Store", Icons.Default.Store),
    AccountIconOption("LocalMall", Icons.Default.LocalMall),
    AccountIconOption("ShoppingBag", Icons.Default.ShoppingBag),
    // Food
    AccountIconOption("Restaurant", Icons.Default.Restaurant),
    AccountIconOption("LocalCafe", Icons.Default.LocalCafe),
    AccountIconOption("Fastfood", Icons.Default.Fastfood),
    AccountIconOption("LocalGroceryStore", Icons.Default.LocalGroceryStore),
    // Health
    AccountIconOption("LocalHospital", Icons.Default.LocalHospital),
    AccountIconOption("FitnessCenter", Icons.Default.FitnessCenter),
    AccountIconOption("MedicalServices", Icons.Default.MedicalServices),
    // Education
    AccountIconOption("School", Icons.Default.School),
    AccountIconOption("MenuBook", Icons.Default.MenuBook),
    // Entertainment
    AccountIconOption("SportsEsports", Icons.Default.SportsEsports),
    AccountIconOption("Movie", Icons.Default.Movie),
    AccountIconOption("MusicNote", Icons.Default.MusicNote),
    // Services
    AccountIconOption("Phone", Icons.Default.Phone),
    AccountIconOption("Wifi", Icons.Default.Wifi),
    AccountIconOption("ElectricBolt", Icons.Default.ElectricBolt),
    AccountIconOption("WaterDrop", Icons.Default.WaterDrop),
    // Other
    AccountIconOption("Pets", Icons.Default.Pets),
    AccountIconOption("ChildCare", Icons.Default.ChildCare),
    AccountIconOption("Work", Icons.Default.Work),
    AccountIconOption("Star", Icons.Default.Star),
    AccountIconOption("Favorite", Icons.Default.Favorite)
)

/**
 * Returns the ImageVector for a given icon name, or null if not found.
 */
fun getIconByName(name: String?): ImageVector? {
    if (name == null) return null
    return availableAccountIcons.firstOrNull { it.name == name }?.icon
}

// ---------------------------------------------------------------------------
// Icon mapping by account subtype (with custom icon override)
// ---------------------------------------------------------------------------

@Composable
fun getAccountIcon(account: AccountBalance): Pair<ImageVector, Color> {
    // 🆕 If the account has a custom icon, use it
    val customIcon = getIconByName(account.icon)
    if (customIcon != null) {
        // Use a neutral accent color for custom icons
        val customColor = when (account.accountType.lowercase()) {
            "asset"     -> Color(0xFF1565C0)
            "liability" -> Color(0xFFC62828)
            "income"    -> Color(0xFF2E7D32)
            "expense"   -> Color(0xFFE65100)
            else        -> Color(0xFF757575)
        }
        return customIcon to customColor
    }

    // Fall back to subtype-based mapping
    val subtypeLower = account.accountType.lowercase()
    val typeLower = account.accountType.lowercase()

    return when {
        subtypeLower == "bank" || subtypeLower == "banco" -> Icons.Default.AccountBalance to Color(0xFF1565C0)
        subtypeLower == "cash" || subtypeLower == "efectivo" -> Icons.Default.Wallet to Color(0xFF2E7D32)
        subtypeLower == "credit_card" || subtypeLower == "tarjeta" -> Icons.Default.CreditCard to Color(0xFFC62828)
        subtypeLower == "savings" || subtypeLower == "ahorro" || account.isSavings -> Icons.Default.Star to Color(0xFF7B1FA2)
        subtypeLower == "investment" || subtypeLower == "inversión" || subtypeLower == "inversion" -> Icons.Default.TrendingUp to Color(0xFFFF8F00)
        // Fall back to type-based
        typeLower == "asset" -> Icons.Default.AccountBalanceWallet to Color(0xFF1565C0)
        typeLower == "liability" -> Icons.Default.CreditCard to Color(0xFFC62828)
        typeLower == "income" -> Icons.Default.TrendingUp to Color(0xFF2E7D32)
        typeLower == "expense" -> Icons.Default.TrendingDown to Color(0xFFE65100)
        else -> Icons.Default.AccountBalanceWallet to Color(0xFF757575)
    }
}

// ---------------------------------------------------------------------------
// Icon Picker Grid composable
// ---------------------------------------------------------------------------

@Composable
fun IconPickerGrid(
    selectedIcon: String?,
    onIconSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedIconVector = getIconByName(selectedIcon)

    Column {
        // ── Header colapsable ────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ícono seleccionado o placeholder
                Icon(
                    imageVector = selectedIconVector ?: Icons.Default.EmojiEmotions,
                    contentDescription = null,
                    tint = if (selectedIcon != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (selectedIcon != null) "Ícono: $selectedIcon" else "Ícono personalizado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedIcon != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Cerrar" else "Abrir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── Grid expandible ──────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // "Sin ícono" option to clear
                    item {
                        val isSelected = selectedIcon == null
                        Surface(
                            modifier = Modifier
                                .size(42.dp)
                                .clickable {
                                    onIconSelected(null)
                                    expanded = false
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected)
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = "Sin ícono",
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    items(availableAccountIcons) { option ->
                        val isSelected = selectedIcon == option.name
                        Surface(
                            modifier = Modifier
                                .size(42.dp)
                                .clickable {
                                    onIconSelected(option.name)
                                    expanded = false
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected)
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    option.icon,
                                    contentDescription = option.name,
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Main screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel      : AccountsViewModel,
    tutorialManager: TutorialManager? = null,
    onNavigateBack : (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    // Tutorial
    var showTutorial by remember {
        mutableStateOf(tutorialManager?.let { !it.isTutorialCompleted(TutorialModule.ACCOUNTS) } ?: false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Cuentas") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar cuenta")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> LoadingOverlay()
                uiState.error != null -> ErrorState(uiState.error!!) { viewModel.loadAccounts() }
                else -> AccountsList(
                    sharedAccounts   = uiState.sharedAccounts,
                    personalAccounts = uiState.personalAccounts,
                    savingsAccounts  = uiState.savingsAccounts,
                    onDeleteClick    = { accountId -> viewModel.showDeleteConfirm(accountId) },
                    onAccountClick   = { account -> viewModel.selectAccount(account) },
                    isSuperUser      = uiState.isSuperUser,
                    onEditClick      = { accountId -> viewModel.showEditDialog(accountId) }
                )
            }
        }
    }

    // ── Diálogo crear cuenta ───────────────────────────────────────────────
    if (uiState.showCreateDialog) {
        CreateAccountDialog(
            name        = uiState.newAccountName,
            subtype     = uiState.newAccountSubtype,
            isShared    = uiState.newAccountIsShared,
            isCreating  = uiState.isCreating,
            error       = uiState.error,
            hasInitialBalance = uiState.newAccountHasInitialBalance,
            initialBalance    = uiState.newAccountInitialBalance,
            icon              = uiState.newAccountIcon,                    // 🆕
            onNameChange    = { viewModel.onNameChange(it) },
            onSubtypeChange = { viewModel.onSubtypeChange(it) },
            onIsSharedChange = { viewModel.onIsSharedChange(it) },
            onHasInitialBalanceChange = { viewModel.onHasInitialBalanceChange(it) },
            onIsSavingsChange         = { viewModel.onIsSavingsChange(it) },
            isSavings                 = uiState.newAccountIsSavings,
            onInitialBalanceChange    = { viewModel.onInitialBalanceChange(it) },
            onIconChange              = { viewModel.onIconChange(it) },    // 🆕
            onDismiss   = { viewModel.dismissCreateDialog() },
            onCreate    = { viewModel.createAccount() }
        )
    }

    // ── Confirmación de eliminación ────────────────────────────────────────
    if (uiState.showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = { Text("Eliminar cuenta") },
            text  = { Text("¿Estás seguro de que deseas eliminar esta cuenta? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteAccount() },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirm() }) { Text("Cancelar") }
            }
        )
    }

    // ✅ BottomSheet con detalle de cuenta y movimientos recientes
    uiState.selectedAccount?.let { account ->
        AccountDetailSheet(
            account = account,
            transactions = uiState.selectedAccountTransactions,
            isLoading = uiState.isLoadingTransactions,
            onDismiss = { viewModel.dismissAccountDetail() }
        )
    }

    // ── Diálogo editar cuenta ─────────────────────────────────────────────
    if (uiState.showEditDialog != null) {
        EditAccountDialog(
            name           = uiState.editAccountName,
            isSavings      = uiState.editAccountIsSavings,
            isShared       = uiState.editAccountIsShared,
            isSaving       = uiState.isSavingEdit,
            error          = uiState.error,
            icon           = uiState.editAccountIcon,                      // 🆕
            onNameChange   = { viewModel.onEditNameChange(it) },
            onIsSavingsChange = { viewModel.onEditIsSavingsChange(it) },
            onIsSharedChange  = { viewModel.onEditIsSharedChange(it) },
            onIconChange      = { viewModel.onEditIconChange(it) },        // 🆕
            onDismiss      = { viewModel.dismissEditDialog() },
            onSave         = { viewModel.saveEditAccount() }
        )
    }

    // ── Tutorial overlay ────────────────────────────────────────────────
    if (showTutorial) {
        TutorialOverlay(
            module = TutorialModule.ACCOUNTS,
            onComplete = {
                tutorialManager?.markTutorialCompleted(TutorialModule.ACCOUNTS)
                showTutorial = false
            },
            onSkip = {
                tutorialManager?.markTutorialCompleted(TutorialModule.ACCOUNTS)
                showTutorial = false
            }
        )
    }
}

// ---------------------------------------------------------------------------
// BottomSheet con detalle de cuenta y últimos movimientos
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDetailSheet(
    account: AccountBalance,
    transactions: List<Transaction>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    val (icon, iconColor) = getAccountIcon(account)

    val typeLabel = when (account.accountType.lowercase()) {
        "asset"     -> "Activo"
        "liability" -> "Pasivo / Deuda"
        "income"    -> "Fuente de ingresos"
        "expense"   -> "Categoría de gasto"
        else        -> account.accountType
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Cabecera de la cuenta ────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = account.accountName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = typeLabel,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Saldo ─────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = iconColor.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Saldo actual", fontSize = 13.sp, color = Color.Gray)
                    Text(
                        text = clpFormat.format(account.movementBalance),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = if (account.movementBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // ── Últimos movimientos ─────────────────────────────────────
            Text(
                "Últimos movimientos",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = iconColor)
                    }
                }
                transactions.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Sin movimientos registrados",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(transactions, key = { it.id }) { transaction ->
                            AccountTransactionRow(transaction = transaction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountTransactionRow(transaction: Transaction) {
    val isIncome = transaction.type.lowercase() == "income"
    val isTransfer = transaction.type.lowercase() == "transfer"
    val color = when {
        isIncome    -> Color(0xFF4CAF50)
        isTransfer  -> Color(0xFF2196F3)
        else        -> Color(0xFFF44336)
    }
    val icon = when {
        isIncome    -> Icons.Default.ArrowDownward
        isTransfer  -> Icons.Default.SwapHoriz
        else        -> Icons.Default.ArrowUpward
    }
    val typeLabel = when (transaction.type.lowercase()) {
        "income"    -> "Ingreso"
        "transfer"  -> "Transferencia"
        else        -> "Gasto"
    }

    val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description ?: typeLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = DateFormatter.formatForDisplay(transaction.createdAt),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = clpFormat.format(transaction.amount),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = color
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Diálogo de creación de cuenta
// ---------------------------------------------------------------------------

data class AccountTypeOption(
    val label         : String,
    val accountType   : String,
    val accountSubtype: String,
    val icon          : ImageVector,
    val iconColor     : Color
)

val accountTypeOptions = listOf(
    AccountTypeOption("Billetera / Efectivo", "asset",     "cash",        Icons.Default.Wallet,              Color(0xFF2E7D32)),
    AccountTypeOption("Cuenta Bancaria",       "asset",     "bank",        Icons.Default.AccountBalance,      Color(0xFF1565C0)),
    AccountTypeOption("Tarjeta de Crédito",    "liability", "credit_card", Icons.Default.CreditCard,          Color(0xFFC62828)),
    AccountTypeOption("Categoría de Gasto",    "expense",   "other",       Icons.Default.TrendingDown,        Color(0xFFE65100)),
    AccountTypeOption("Fuente de Ingreso",     "income",    "other",       Icons.Default.TrendingUp,          Color(0xFF2E7D32)),
    AccountTypeOption("Otro",                  "asset",     "other",       Icons.Default.AccountBalanceWallet, Color(0xFF757575))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountDialog(
    name            : String,
    subtype         : String,
    isShared        : Boolean,
    isCreating      : Boolean,
    error           : String?,
    hasInitialBalance: Boolean = false,
    initialBalance  : String = "",
    isSavings       : Boolean = false,
    icon            : String? = null,                    // 🆕 Custom icon
    onNameChange    : (String) -> Unit,
    onSubtypeChange : (String) -> Unit,
    onIsSharedChange: (Boolean) -> Unit,
    onIsSavingsChange: (Boolean) -> Unit = {},
    onHasInitialBalanceChange: (Boolean) -> Unit = {},
    onInitialBalanceChange   : (String) -> Unit = {},
    onIconChange    : (String?) -> Unit = {},            // 🆕 Custom icon
    onDismiss       : () -> Unit,
    onCreate        : () -> Unit
) {
    val selectedOption = accountTypeOptions.firstOrNull { it.accountSubtype == subtype }
        ?: accountTypeOptions[0]
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Nueva cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Nombre
                OutlinedTextField(
                    value         = name,
                    onValueChange = onNameChange,
                    label         = { Text("Nombre de la cuenta") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    enabled       = !isCreating
                )

                // 🆕 Icon picker (BEFORE account type selector)
                IconPickerGrid(
                    selectedIcon = icon,
                    onIconSelected = onIconChange
                )

                // Tipo de cuenta — selector visual con íconos
                Text(
                    text = "Tipo de cuenta",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    accountTypeOptions.forEach { option ->
                        val isSelected = selectedOption.accountSubtype == option.accountSubtype
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isCreating) {
                                    onSubtypeChange(option.accountSubtype)
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected)
                                option.iconColor.copy(alpha = 0.12f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected)
                                androidx.compose.foundation.BorderStroke(2.dp, option.iconColor)
                            else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) option.iconColor else Color.Gray,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) option.iconColor else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = option.iconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Compartida / Personal
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = if (isShared) "Cuenta compartida" else "Cuenta personal",
                        modifier = Modifier.weight(1f),
                        style    = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked         = isShared,
                        onCheckedChange = onIsSharedChange,
                        enabled         = !isCreating
                    )
                }


                // Toggle de cuenta de ahorro
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = if (isSavings) "Cuenta de ahorro" else "Cuenta normal",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text  = "Las cuentas de ahorro no suman en el balance del hogar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked         = isSavings,
                        onCheckedChange = onIsSavingsChange,
                        enabled         = !isCreating
                    )
                }

                // ── Saldo inicial ─────────────────────────────────────────
                HorizontalDivider()
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked         = hasInitialBalance,
                        onCheckedChange = onHasInitialBalanceChange,
                        enabled         = !isCreating
                    )
                    Text(
                        text     = "Agregar saldo inicial",
                        modifier = Modifier.weight(1f),
                        style    = MaterialTheme.typography.bodyMedium
                    )
                }
                if (hasInitialBalance) {
                    OutlinedTextField(
                        value         = initialBalance,
                        onValueChange = onInitialBalanceChange,
                        label         = { Text("Saldo inicial (CLP)") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        enabled       = !isCreating,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix        = { Text("$") }
                    )
                }

                if (error != null) {
                    Text(
                        text  = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isCreating) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creando cuenta...")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCreate,
                enabled = !isCreating && name.isNotBlank()
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancelar") }
        }
    )
}

// ---------------------------------------------------------------------------
// Diálogo de edición de cuenta
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountDialog(
    name            : String,
    isSavings       : Boolean,
    isShared        : Boolean,
    isSaving        : Boolean,
    error           : String?,
    icon            : String? = null,                    // 🆕 Custom icon
    onNameChange    : (String) -> Unit,
    onIsSavingsChange: (Boolean) -> Unit,
    onIsSharedChange: (Boolean) -> Unit,
    onIconChange    : (String?) -> Unit = {},            // 🆕 Custom icon
    onDismiss       : () -> Unit,
    onSave          : () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Editar cuenta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // 🆕 Icon picker (BEFORE name field)
                IconPickerGrid(
                    selectedIcon = icon,
                    onIconSelected = onIconChange
                )

                OutlinedTextField(
                    value         = name,
                    onValueChange = onNameChange,
                    label         = { Text("Nombre de la cuenta") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    enabled       = !isSaving
                )

                // Compartida / Personal
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = if (isShared) "Cuenta compartida" else "Cuenta personal",
                        modifier = Modifier.weight(1f),
                        style    = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked         = isShared,
                        onCheckedChange = onIsSharedChange,
                        enabled         = !isSaving
                    )
                }

                // Cuenta de ahorro
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = if (isSavings) "Cuenta de ahorro" else "Cuenta normal",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text  = "Las cuentas de ahorro no suman en el balance del hogar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked         = isSavings,
                        onCheckedChange = onIsSavingsChange,
                        enabled         = !isSaving
                    )
                }

                if (error != null) {
                    Text(
                        text  = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isSaving) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardando...")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSaving && name.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancelar") }
        }
    )
}

// ---------------------------------------------------------------------------
// Lista de cuentas con swipe support
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsList(
    sharedAccounts  : List<AccountBalance>,
    personalAccounts: List<AccountBalance>,
    savingsAccounts : List<AccountBalance> = emptyList(),
    onDeleteClick   : (String) -> Unit,
    onAccountClick  : (AccountBalance) -> Unit,
    isSuperUser     : Boolean = false,
    onEditClick     : (String) -> Unit = {}
) {
    if (sharedAccounts.isEmpty() && personalAccounts.isEmpty() && savingsAccounts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No tienes cuentas registradas.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Usa el botón + para agregar una.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sharedAccounts.isNotEmpty()) {
                item {
                    Text(
                        text  = "Cuentas compartidas",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(sharedAccounts, key = { it.accountId }) { account ->
                    SwipeableAccountItem(
                        account = account,
                        onDeleteClick = onDeleteClick,
                        onAccountClick = onAccountClick,
                        isSuperUser = isSuperUser,
                        onEditClick = onEditClick
                    )
                }
            }
            if (personalAccounts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text  = "Cuentas personales",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(personalAccounts, key = { it.accountId }) { account ->
                    SwipeableAccountItem(
                        account = account,
                        onDeleteClick = onDeleteClick,
                        onAccountClick = onAccountClick,
                        isSuperUser = isSuperUser,
                        onEditClick = onEditClick
                    )
                }
            }
            if (savingsAccounts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text  = "🐷 Cuentas de ahorro",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF6A1B9A),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text  = "No suman en el balance del hogar",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(savingsAccounts, key = { it.accountId }) { account ->
                    SwipeableAccountItem(
                        account = account,
                        onDeleteClick = onDeleteClick,
                        onAccountClick = onAccountClick,
                        isSuperUser = isSuperUser,
                        onEditClick = onEditClick
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Swipeable account item wrapper
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableAccountItem(
    account       : AccountBalance,
    onDeleteClick : (String) -> Unit,
    onAccountClick: (AccountBalance) -> Unit,
    isSuperUser   : Boolean = false,
    onEditClick   : (String) -> Unit = {}
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe left → Delete
                    onDeleteClick(account.accountId)
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe right → Edit (todos los usuarios)
                    onEditClick(account.accountId)
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection

            val backgroundColor by animateColorAsState(
                when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFF44336)
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFFFF9800)
                    else -> Color.Transparent
                },
                label = "swipeBgColor"
            )

            val iconVector = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                else -> null
            }

            val alignment = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                iconVector?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        AccountItem(
            account = account,
            onDeleteClick = onDeleteClick,
            onAccountClick = onAccountClick
        )
    }
}

// ---------------------------------------------------------------------------
// Account item card with icon
// ---------------------------------------------------------------------------

@Composable
fun AccountItem(
    account       : AccountBalance,
    onDeleteClick : (String) -> Unit,
    onAccountClick: (AccountBalance) -> Unit
) {
    val clpFormat = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    val (icon, iconColor) = getAccountIcon(account)

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onAccountClick(account) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector      = icon,
                contentDescription = null,
                tint             = iconColor,
                modifier         = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = account.accountName,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = when (account.accountType.lowercase()) {
                        "asset"     -> "Activo"
                        "liability" -> "Pasivo / Deuda"
                        "income"    -> "Fuente de ingresos"
                        "expense"   -> "Categoría de gasto"
                        else        -> account.accountType
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text       = clpFormat.format(account.movementBalance),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = if (account.movementBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onDeleteClick(account.accountId) }) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint               = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier              = Modifier.fillMaxSize(),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}
