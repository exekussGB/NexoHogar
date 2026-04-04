package com.nexohogar.presentation.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexohogar.domain.model.InventoryCategory
import com.nexohogar.domain.model.InventoryMovement
import com.nexohogar.domain.model.Product
import com.nexohogar.domain.model.PurchaseSuggestion
import androidx.compose.ui.platform.testTag
import com.nexohogar.core.tutorial.TutorialManager
import com.nexohogar.core.tutorial.TutorialModule
import com.nexohogar.presentation.tutorial.TutorialOverlay
import com.nexohogar.presentation.tutorial.inventoryTutorialSteps
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import kotlin.compareTo
import kotlin.text.category


/** Returns user-friendly price label based on unit, e.g. "Precio por kg", "Precio por unidad" */
private fun pricePerUnitLabel(unit: String): String = when (unit.lowercase()) {
    "kg" -> "Precio por kg"
    "g", "gr" -> "Precio por g"
    "l", "lt", "litro", "litros" -> "Precio por litro"
    "ml" -> "Precio por ml"
    "unidad", "unidades", "un" -> "Precio por unidad"
    "docena" -> "Precio por docena"
    "paquete" -> "Precio por paquete"
    "caja" -> "Precio por caja"
    "botella" -> "Precio por botella"
    "lata" -> "Precio por lata"
    "sobre" -> "Precio por sobre"
    "rollo" -> "Precio por rollo"
    else -> "Precio por $unit"
}

// ─── Pantalla principal ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    tutorialManager: TutorialManager,
    onBack: () -> Unit,
    onNavigateToScanner: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var productForAction by remember { mutableStateOf<Product?>(null) }

    var showTutorial by remember {
        mutableStateOf(!tutorialManager.isTutorialCompleted(TutorialModule.INVENTORY))
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Productos", "Registrar", "Categorías", "Sugerencias")

    var selectedProductForHistory by remember { mutableStateOf<Product?>(null) }
    var productToConsume by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventario", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    if (onNavigateToScanner != null) {
                        IconButton(onClick = onNavigateToScanner) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Escanear boleta", tint = Color.White)
                        }
                    }
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
                    }
                }
            )
        }
        // Sin floatingActionButton — todo el ingreso es desde la pestaña Registrar
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = LightBlue,
                edgePadding = 0.dp,
                modifier = Modifier.testTag("inventory_list")
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = { Text(title, fontSize = 13.sp) },
                        modifier = when (i) {
                            1 -> Modifier.testTag("inventory_add_button")   // Registrar
                            2 -> Modifier.testTag("inventory_movements")    // Categorías
                            else -> Modifier
                        }
                    )
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
                uiState.error != null -> ErrorState(uiState.error!!) { viewModel.loadData() }
                else -> when (selectedTab) {
                    0 -> ProductsTab(
                        uiState = uiState,
                        onProductClick = { p ->
                            selectedProductForHistory = p
                            viewModel.loadMovementsForProduct(p)
                        },
                        onQuickConsume = { p -> productToConsume = p },
                        onSelectCategory = { viewModel.selectCategory(it) },
                        onAddToInventory = { selectedTab = 1 },
                        onProductAction = { productForAction = it }
                    )
                    1 -> RegisterTab(
                        viewModel = viewModel,
                        allProducts = uiState.products,
                        categories = uiState.categories,
                        onRegistered = { selectedTab = 0 }
                    )
                    2 -> CategoriesTab(
                        viewModel = viewModel,
                        categories = uiState.categories,
                        stats = uiState.categoryStats
                    )
                    3 -> SuggestionsTab(suggestions = uiState.suggestions)
                }
            }
        }
    }

    // ─── Hoja de historial de producto ─────────────────────────────────────────
    selectedProductForHistory?.let { product ->
        ProductHistorySheet(
            product = product,
            viewModel = viewModel,
            onDismiss = { selectedProductForHistory = null }
        )
    }

        // ─── Popup de acciones de producto ──────────────────────────────────────────
    productForAction?.let { product ->
        ProductActionPopup(
            product = product,
            onDismiss = { productForAction = null },
            onViewHistory = { p ->
                selectedProductForHistory = p
                viewModel.loadMovementsForProduct(p)
            },
            onQuickConsume = { p -> productToConsume = p }
        )
    }

    // ── Tutorial overlay ────────────────────────────────────────────────────
    if (showTutorial) {
        TutorialOverlay(
            steps = inventoryTutorialSteps,
            onDismiss = {
                tutorialManager.markTutorialCompleted(TutorialModule.INVENTORY)
                showTutorial = false
            }
        )
    }
}

// ─── Pestaña: Registrar ────────────────────────────────────────────────────────
// Permite registrar compras y consumos para productos existentes,
// Y también crear un producto nuevo directamente con su primera compra.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterTab(
    viewModel: InventoryViewModel,
    allProducts: List<Product>,
    categories: List<InventoryCategory>,
    onRegistered: () -> Unit
) {
    val movForm by viewModel.movementForm.collectAsState()
    val prodForm by viewModel.productForm.collectAsState()

    // true = crear producto nuevo, false = producto existente
    var isNewProduct by remember { mutableStateOf(false) }
    // Tipo de operación: "in" = Compra, "out" = Consumo
    var movementType by remember { mutableStateOf("in") }

    var productDropdownExpanded by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    val units = listOf("kg", "gramos", "unidades", "litros", "ml")

    // Cuando el tipo cambia a "Consumo", forzar modo producto existente
    LaunchedEffect(movementType) {
        if (movementType == "out") isNewProduct = false
        viewModel.onMovementTypeChange(movementType)
    }

    // Resetear formularios al cambiar modo
    LaunchedEffect(isNewProduct) {
        viewModel.resetProductForm()
        viewModel.resetMovementForm()
        viewModel.onMovementTypeChange(movementType)
    }

    // Volver a Productos al registrar exitosamente
    LaunchedEffect(movForm.success) {
        if (movForm.success) { viewModel.resetMovementForm(); onRegistered() }
    }
    LaunchedEffect(prodForm.success) {
        if (prodForm.success) { viewModel.resetProductForm(); onRegistered() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("¿Qué quieres registrar?", fontWeight = FontWeight.Bold, fontSize = 17.sp)

        // ── Selector: Compra / Consumo ────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val isCompra = movementType == "in"
            Card(
                modifier = Modifier.weight(1f).clickable { movementType = "in" },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isCompra) GreenIn else Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(if (isCompra) 4.dp else 1.dp)
            ) {
                Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null,
                        tint = if (isCompra) Color.White else GreenIn, modifier = Modifier.size(32.dp))
                    Text("Compra", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        color = if (isCompra) Color.White else GreenIn)
                    Text("Agregar stock", fontSize = 11.sp,
                        color = if (isCompra) Color.White.copy(alpha = 0.8f) else Color.Gray)
                }
            }

            val isConsumo = movementType == "out"
            Card(
                modifier = Modifier.weight(1f).clickable { movementType = "out" },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isConsumo) RedOut else Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(if (isConsumo) 4.dp else 1.dp)
            ) {
                Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.RemoveShoppingCart, contentDescription = null,
                        tint = if (isConsumo) Color.White else RedOut, modifier = Modifier.size(32.dp))
                    Text("Consumo", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        color = if (isConsumo) Color.White else RedOut)
                    Text("Descontar stock", fontSize = 11.sp,
                        color = if (isConsumo) Color.White.copy(alpha = 0.8f) else Color.Gray)
                }
            }
        }

        // ── Toggle: Producto existente / Producto nuevo (solo en Compra) ──────
        if (movementType == "in") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { isNewProduct = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (!isNewProduct) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (!isNewProduct) PrimaryBlue else Color.Gray
                    )
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (!isNewProduct) PrimaryBlue else Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text("Existente", fontSize = 13.sp,
                        color = if (!isNewProduct) PrimaryBlue else Color.Gray)
                }
                OutlinedButton(
                    onClick = { isNewProduct = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isNewProduct) GreenIn.copy(alpha = 0.1f) else Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (isNewProduct) GreenIn else Color.Gray
                    )
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isNewProduct) GreenIn else Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text("Nuevo producto", fontSize = 13.sp,
                        color = if (isNewProduct) GreenIn else Color.Gray)
                }
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // FORMULARIO: Producto existente
        // ══════════════════════════════════════════════════════════════════════
        if (!isNewProduct) {
            // Selector de producto
            ExposedDropdownMenuBox(
                expanded = productDropdownExpanded,
                onExpandedChange = { productDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = movForm.selectedProduct?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Producto *") },
                    placeholder = { Text("Selecciona un producto") },
                    leadingIcon = {
                        Icon(Icons.Default.Inventory2, contentDescription = null,
                            tint = if (movementType == "in") GreenIn else RedOut)
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = productDropdownExpanded,
                    onDismissRequest = { productDropdownExpanded = false }) {
                    if (allProducts.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Sin productos — ve a Registrar > Nuevo producto", color = Color.Gray, fontSize = 13.sp) },
                            onClick = { productDropdownExpanded = false }
                        )
                    }
                    allProducts.forEach { product ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(product.name, fontWeight = FontWeight.Medium)
                                    Text("Stock: ${String.format("%.2f", product.currentStock)} ${product.unit}",
                                        fontSize = 11.sp, color = Color.Gray)
                                }
                            },
                            onClick = {
                                viewModel.onMovementProductSelect(product)
                                productDropdownExpanded = false
                            }
                        )
                    }

                }
            }

            // Cantidad
            OutlinedTextField(
                value = movForm.quantity,
                onValueChange = viewModel::onMovementQuantityChange,
                label = { Text("Cantidad *") },
                suffix = { Text(movForm.selectedProduct?.unit ?: "") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Detalles de compra (solo en "in")
            if (movementType == "in") {
                HorizontalDivider(color = GreenIn.copy(alpha = 0.2f))
                Text("Detalles de la compra (opcional)", fontSize = 8.sp, color = Color.Gray)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = movForm.pricePerUnit,
                        onValueChange = viewModel::onMovementPricePerUnitChange,
                        label = { Text(pricePerUnitLabel(movForm.selectedProduct?.unit ?: "unidad")) },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = movForm.priceTotal,
                        onValueChange = viewModel::onMovementPriceTotalChange,
                        label = { Text("Precio total") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = movForm.brand,
                    onValueChange = viewModel::onMovementBrandChange,
                    label = { Text("Marca") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = movForm.store,
                    onValueChange = viewModel::onMovementStoreChange,
                    label = { Text("Tienda / Local") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Fecha
            OutlinedTextField(
                value = movForm.movementDate,
                onValueChange = viewModel::onMovementDateChange,
                label = { Text("Fecha (yyyy-MM-dd)") },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            // Error
            movForm.error?.let {
                ErrorBanner(it)
            }

            // Botón registrar
            Button(
                onClick = { viewModel.submitMovement() },
                enabled = !movForm.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (movementType == "in") GreenIn else RedOut
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (movForm.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(
                        if (movementType == "in") Icons.Default.ShoppingCart else Icons.Default.RemoveShoppingCart,
                        contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (movementType == "in") "Registrar compra" else "Registrar consumo",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // FORMULARIO: Nuevo producto (solo visible en Compra)
        // ══════════════════════════════════════════════════════════════════════
        if (isNewProduct && movementType == "in") {

            // Nombre del producto
            OutlinedTextField(
                value = prodForm.name,
                onValueChange = viewModel::onProductNameChange,
                label = { Text("Nombre del producto *") },
                leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            // Categoría — cargada desde backend
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = prodForm.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría (opcional)") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Sin categoría") },
                        onClick = { viewModel.onProductCategoryChange(""); categoryDropdownExpanded = false }
                    )
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!cat.icon.isNullOrBlank()) {
                                        Text(cat.icon, fontSize = 16.sp)
                                    }
                                    Text(cat.name)
                                }
                            },
                            onClick = { viewModel.onProductCategoryChange(cat.name); categoryDropdownExpanded = false }
                        )
                    }
                    // ── Botón: Crear nueva categoría inline ──
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Crear nueva categoría",
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        },
                        onClick = {
                            categoryDropdownExpanded = false
                            showCreateCategoryDialog = true
                        }
                    )
                }
            }

            // Unidad de medida
            ExposedDropdownMenuBox(
                expanded = unitDropdownExpanded,
                onExpandedChange = { unitDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = prodForm.unit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unidad de medida *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = unitDropdownExpanded,
                    onDismissRequest = { unitDropdownExpanded = false }) {
                    units.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit) },
                            onClick = { viewModel.onProductUnitChange(unit); unitDropdownExpanded = false }
                        )
                    }
                }
            }

            // Marca
            OutlinedTextField(
                value = prodForm.brand,
                onValueChange = viewModel::onProductBrandChange,
                label = { Text("Marca (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // ── Stock inicial ───
            HorizontalDivider(color = GreenIn.copy(alpha = 0.2f))
            Text("Stock inicial (opcional)", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            Text("¿Tienes este producto en casa? Ingresa la cantidad actual.", fontSize = 11.sp, color = Color.Gray)

            OutlinedTextField(
                value = prodForm.initialQuantity,
                onValueChange = viewModel::onProductInitialQuantityChange,
                label = { Text("Cantidad inicial") },
                placeholder = { Text("Ej: 2.5") },
                suffix = { Text(prodForm.unit) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Registrar como compra (toggle) — solo visible si hay cantidad ───
            val hasQuantity = prodForm.initialQuantity.toDoubleOrNull()?.let { it > 0 } ?: false
            if (hasQuantity) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onProductRegisterAsPurchaseChange(!prodForm.registerAsPurchase) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = prodForm.registerAsPurchase,
                        onCheckedChange = viewModel::onProductRegisterAsPurchaseChange,
                        colors = CheckboxDefaults.colors(checkedColor = GreenIn)
                    )
                    Column {
                        Text("Registrar como compra", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Incluir en gastos del hogar", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            // ── Detalles de compra — solo visible si registerAsPurchase y hay cantidad ───
            if (hasQuantity && prodForm.registerAsPurchase) {
                HorizontalDivider(color = GreenIn.copy(alpha = 0.2f))
                Text("Detalles de la compra", fontSize = 8.sp, color = GreenIn, fontWeight = FontWeight.SemiBold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prodForm.pricePerUnit,
                        onValueChange = viewModel::onProductPricePerUnitChange,
                        label = { Text(pricePerUnitLabel(prodForm.unit)) },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = prodForm.priceTotal,
                        onValueChange = viewModel::onProductPriceTotalChange,
                        label = { Text("Precio total") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = prodForm.store,
                    onValueChange = viewModel::onProductStoreChange,
                    label = { Text("Tienda / Local") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Error
            prodForm.error?.let {
                ErrorBanner(it)
            }

            // Botón crear — texto dinámico según si hay cantidad y si es compra
            val hasQuantityForButton = prodForm.initialQuantity.toDoubleOrNull()?.let { it > 0 } ?: false
            Button(
                onClick = { viewModel.submitProduct() },
                enabled = !prodForm.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenIn),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (prodForm.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(
                        when {
                            hasQuantityForButton && prodForm.registerAsPurchase -> Icons.Default.AddShoppingCart
                            hasQuantityForButton -> Icons.Default.Inventory2
                            else -> Icons.Default.Add
                        },
                        contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            hasQuantityForButton && prodForm.registerAsPurchase -> "Crear producto y registrar compra"
                            hasQuantityForButton -> "Crear producto con stock inicial"
                            else -> "Crear producto"
                        },
                        fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
            }
        }
    }

    // ── Diálogo para crear categoría desde el formulario de nuevo producto ──
    if (showCreateCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newCatIcon by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateCategoryDialog = false },
            title = {
                Text("Nueva categoría", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Nombre *") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCatIcon,
                        onValueChange = { newCatIcon = it },
                        label = { Text("Ícono (emoji, opcional)") },
                        placeholder = { Text("Ej: 🥩 🧴 🍎") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            viewModel.onCategoryNameChange(newCatName.trim())
                            viewModel.onCategoryIconChange(newCatIcon.trim())
                            viewModel.submitCategory()
                            viewModel.onProductCategoryChange(newCatName.trim())
                            showCreateCategoryDialog = false
                        }
                    },
                    enabled = newCatName.isNotBlank()
                ) {
                    Text("Crear", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCategoryDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ─── Estado de error ──────────────────────────────────────────────────────────
@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null,
                tint = Color(0xFFC62828), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, textAlign = TextAlign.Center, color = Color.Gray, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                Text("Reintentar")
            }
        }
    }
}
