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
import com.nexohogar.domain.model.InventoryMovement
import com.nexohogar.domain.model.Product
import com.nexohogar.domain.model.PurchaseSuggestion

// ─── Colores de marca ──────────────────────────────────────────────────────────
private val PrimaryBlue = Color(0xFF1565C0)
private val LightBlue   = Color(0xFFE3F2FD)
private val GreenIn     = Color(0xFF2E7D32)
private val RedOut      = Color(0xFFC62828)

// ─── Pantalla principal ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Productos", "Registrar", "Categorías", "Sugerencias")

    var showAddProductSheet by remember { mutableStateOf(false) }
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
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddProductSheet = true },
                    containerColor = PrimaryBlue
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar producto", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = LightBlue, edgePadding = 0.dp) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = { Text(title, fontSize = 13.sp) }
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
                        onSelectCategory = { viewModel.selectCategory(it) }
                    )
                    1 -> RegisterMovementTab(viewModel = viewModel)
                    2 -> CategoryStatsTab(stats = uiState.categoryStats)
                    3 -> SuggestionsTab(suggestions = uiState.suggestions)
                }
            }
        }
    }

    // ─── Hoja de agregar producto ───────────────────────────────────────────────
    if (showAddProductSheet) {
        AddProductSheet(
            viewModel = viewModel,
            onDismiss = {
                showAddProductSheet = false
                viewModel.resetProductForm()
            }
        )
    }

    // ─── Hoja de historial de producto ─────────────────────────────────────────
    selectedProductForHistory?.let { product ->
        ProductHistorySheet(
            product = product,
            viewModel = viewModel,
            onDismiss = { selectedProductForHistory = null }
        )
    }

    // ─── Diálogo de consumo rápido ──────────────────────────────────────────────
    productToConsume?.let { product ->
        QuickConsumeDialog(
            product = product,
            onConfirm = { qty ->
                viewModel.quickConsume(product.id, qty)
                productToConsume = null
            },
            onDismiss = { productToConsume = null }
        )
    }
}

// ─── Pestaña: Productos ────────────────────────────────────────────────────────
@Composable
private fun ProductsTab(
    uiState: InventoryUiState,
    onProductClick: (Product) -> Unit,
    onQuickConsume: (Product) -> Unit,
    onSelectCategory: (String?) -> Unit
) {
    val products = uiState.filteredProducts
    val categories = uiState.availableCategories

    Column(Modifier.fillMaxSize()) {
        // Filtros de categoría (solo si hay categorías)
        if (categories.isNotEmpty()) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text("Todos (${uiState.products.size})") }
                )
                categories.forEach { cat ->
                    val count = uiState.products.count { it.category == cat }
                    FilterChip(
                        selected = uiState.selectedCategory == cat,
                        onClick = { onSelectCategory(if (uiState.selectedCategory == cat) null else cat) },
                        label = { Text("$cat ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f)
                        )
                    )
                }
            }
            HorizontalDivider()
        }

        if (products.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory2, contentDescription = null,
                        tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    if (uiState.selectedCategory != null) {
                        Text("Sin productos en \"${uiState.selectedCategory}\"", color = Color.Gray, fontSize = 16.sp)
                    } else {
                        Text("Sin productos aún", color = Color.Gray, fontSize = 16.sp)
                        Text("Toca + para agregar el primero", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onClick = { onProductClick(product) },
                        onQuickConsume = { onQuickConsume(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    onQuickConsume: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícono de stock
            val stockColor = when {
                product.currentStock <= 0 -> Color(0xFFC62828)
                product.currentStock < 1  -> Color(0xFFE65100)
                else -> Color(0xFF2E7D32)
            }
            val stockIcon = when {
                product.currentStock <= 0 -> Icons.Default.RemoveShoppingCart
                product.currentStock < 1  -> Icons.Default.Warning
                else -> Icons.Default.Inventory2
            }
            Icon(stockIcon, contentDescription = null, tint = stockColor, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))

            // Info del producto
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "${String.format("%.2f", product.currentStock)} ${product.unit}",
                    fontSize = 13.sp,
                    color = stockColor
                )
                // Categoría y marca
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!product.category.isNullOrBlank()) {
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                product.category,
                                fontSize = 10.sp,
                                color = PrimaryBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!product.brand.isNullOrBlank()) {
                        Text(product.brand, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            // Botón consumo rápido
            IconButton(onClick = onQuickConsume) {
                Icon(
                    Icons.Default.RemoveCircleOutline,
                    contentDescription = "Registrar consumo",
                    tint = RedOut,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─── Diálogo de consumo rápido ─────────────────────────────────────────────────
@Composable
private fun QuickConsumeDialog(
    product: Product,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var qty by remember { mutableStateOf("") }
    val qtyDouble = qty.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Consumir: ${product.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Stock actual: ${String.format("%.2f", product.currentStock)} ${product.unit}",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Cantidad (${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = qtyDouble != null && qtyDouble <= 0
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { qtyDouble?.let { if (it > 0) onConfirm(it) } },
                enabled = qtyDouble != null && qtyDouble > 0
            ) {
                Text("Confirmar", color = RedOut, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ─── Pestaña: Registrar movimiento ────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterMovementTab(viewModel: InventoryViewModel) {
    val form by viewModel.movementForm.collectAsState()
    val products by viewModel.uiState.collectAsState()

    var productDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(form.success) {
        if (form.success) viewModel.resetMovementForm()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Registrar movimiento", fontWeight = FontWeight.Bold, fontSize = 17.sp)

        // Tipo de movimiento
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = form.movementType == "in",
                onClick = { viewModel.onMovementTypeChange("in") },
                label = { Text("📥 Compra / Entrada") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GreenIn.copy(alpha = 0.15f)
                )
            )
            FilterChip(
                selected = form.movementType == "out",
                onClick = { viewModel.onMovementTypeChange("out") },
                label = { Text("📤 Consumo / Salida") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RedOut.copy(alpha = 0.15f)
                )
            )
        }

        // Selector de producto
        ExposedDropdownMenuBox(
            expanded = productDropdownExpanded,
            onExpandedChange = { productDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = form.selectedProduct?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Producto *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = productDropdownExpanded,
                onDismissRequest = { productDropdownExpanded = false }
            ) {
                products.products.forEach { product ->
                    DropdownMenuItem(
                        text = { Text("${product.name} (${product.unit})") },
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
            value = form.quantity,
            onValueChange = viewModel::onMovementQuantityChange,
            label = { Text("Cantidad *") },
            suffix = { Text(form.selectedProduct?.unit ?: "") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        // Campos solo para compras
        if (form.movementType == "in") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.pricePerUnit,
                    onValueChange = viewModel::onMovementPricePerUnitChange,
                    label = { Text("Precio por unidad") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = form.priceTotal,
                    onValueChange = viewModel::onMovementPriceTotalChange,
                    label = { Text("Precio total") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = form.brand,
                onValueChange = viewModel::onMovementBrandChange,
                label = { Text("Marca") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.store,
                onValueChange = viewModel::onMovementStoreChange,
                label = { Text("Tienda / Local") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Fecha
        OutlinedTextField(
            value = form.movementDate,
            onValueChange = viewModel::onMovementDateChange,
            label = { Text("Fecha (yyyy-MM-dd)") },
            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        // Error
        form.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        // Botón
        Button(
            onClick = { viewModel.submitMovement() },
            enabled = !form.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            if (form.isSubmitting) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
            } else {
                Text(
                    if (form.movementType == "in") "Registrar compra" else "Registrar consumo",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (form.success) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenIn)
                    Spacer(Modifier.width(8.dp))
                    Text("¡Movimiento registrado con éxito!", color = GreenIn)
                }
            }
        }
    }
}

// ─── Pestaña: Categorías ──────────────────────────────────────────────────────
@Composable
private fun CategoryStatsTab(stats: List<CategoryStat>) {
    if (stats.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.BarChart, contentDescription = null,
                    tint = PrimaryBlue, modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Sin estadísticas aún", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Asigna categorías a tus productos y registra compras con precio para ver cuánto gastas en cada categoría.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val totalSpent = stats.sumOf { it.totalSpent }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total gastado registrado", color = Color.White, fontSize = 13.sp)
                    Text(
                        "$${String.format("%,.0f", totalSpent)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Text(
                        "${stats.size} ${if (stats.size == 1) "categoría" else "categorías"}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        items(stats, key = { it.category }) { stat ->
            CategoryStatCard(stat = stat, totalSpent = totalSpent)
        }
    }
}

@Composable
private fun CategoryStatCard(stat: CategoryStat, totalSpent: Double) {
    val percentage = if (totalSpent > 0) (stat.totalSpent / totalSpent * 100).toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Category, contentDescription = null,
                        tint = PrimaryBlue, modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(stat.category, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            "${stat.productCount} ${if (stat.productCount == 1) "producto" else "productos"}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$${String.format("%,.0f", stat.totalSpent)}",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        fontSize = 16.sp
                    )
                    Text(
                        "${String.format("%.1f", percentage)}%",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = PrimaryBlue,
                trackColor = LightBlue
            )
        }
    }
}

// ─── Pestaña: Sugerencias ─────────────────────────────────────────────────────
@Composable
private fun SuggestionsTab(suggestions: List<PurchaseSuggestion>) {
    if (suggestions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null,
                    tint = PrimaryBlue, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Sin sugerencias por ahora", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Las sugerencias aparecen automáticamente cuando el stock de un producto esté por debajo del 50% de tu consumo mensual.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "💡 Basado en tu consumo del último mes",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(suggestions, key = { it.product.id }) { suggestion ->
            SuggestionCard(suggestion)
        }
    }
}

@Composable
private fun SuggestionCard(suggestion: PurchaseSuggestion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null,
                    tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(suggestion.product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (!suggestion.product.category.isNullOrBlank()) {
                        Text(suggestion.product.category, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Comprar: ${String.format("%.2f", suggestion.suggestedQuantity)} ${suggestion.product.unit}",
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue
            )
            if (suggestion.estimatedCost != null) {
                Text(
                    "Costo estimado: $${String.format("%.0f", suggestion.estimatedCost)}",
                    fontSize = 13.sp,
                    color = Color(0xFF2E7D32)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(suggestion.reason, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// ─── Sheet: Agregar producto ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductSheet(viewModel: InventoryViewModel, onDismiss: () -> Unit) {
    val form by viewModel.productForm.collectAsState()
    val units = listOf("kg", "gramos", "unidades", "litros", "ml")
    var unitDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(form.success) {
        if (form.success) onDismiss()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Nuevo producto", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            OutlinedTextField(
                value = form.name,
                onValueChange = viewModel::onProductNameChange,
                label = { Text("Nombre del producto *") },
                leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            // Categoría
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = form.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría (opcional)") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sin categoría") },
                        onClick = {
                            viewModel.onProductCategoryChange("")
                            categoryDropdownExpanded = false
                        }
                    )
                    INVENTORY_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                viewModel.onProductCategoryChange(cat)
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Unidad de medida
            ExposedDropdownMenuBox(
                expanded = unitDropdownExpanded,
                onExpandedChange = { unitDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = form.unit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unidad de medida *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = unitDropdownExpanded,
                    onDismissRequest = { unitDropdownExpanded = false }
                ) {
                    units.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit) },
                            onClick = {
                                viewModel.onProductUnitChange(unit)
                                unitDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = form.brand,
                onValueChange = viewModel::onProductBrandChange,
                label = { Text("Marca (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Cantidad inicial
            OutlinedTextField(
                value = form.initialQuantity,
                onValueChange = viewModel::onProductInitialQuantityChange,
                label = { Text("Cantidad inicial (opcional)") },
                placeholder = { Text("Ej: 2.5") },
                suffix = { Text(form.unit) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            form.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Button(
                onClick = { viewModel.submitProduct() },
                enabled = !form.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (form.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Agregar producto", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Sheet: Historial de producto ────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductHistorySheet(
    product: Product,
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.movementsState.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Stock: ${String.format("%.2f", product.currentStock)} ${product.unit}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                if (!product.category.isNullOrBlank()) {
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            product.category,
                            fontSize = 10.sp,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            when {
                state.isLoading -> Box(
                    Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = PrimaryBlue) }

                state.movements.isEmpty() -> Box(
                    Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Sin movimientos registrados", color = Color.Gray) }

                else -> LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.movements, key = { it.id }) { movement ->
                        MovementRow(movement = movement, unit = product.unit)
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementRow(movement: InventoryMovement, unit: String) {
    val isIn = movement.movementType == "in"
    val color = if (isIn) GreenIn else RedOut
    val sign = if (isIn) "+" else "-"
    val label = if (isIn) "Compra" else "Consumo"

    Row(
        Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, color = color, fontWeight = FontWeight.SemiBold)
            Text(movement.movementDate, fontSize = 11.sp, color = Color.Gray)
            if (!movement.store.isNullOrBlank()) {
                Text("📍 ${movement.store}", fontSize = 11.sp, color = Color.Gray)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "$sign${String.format("%.2f", movement.quantity)} $unit",
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 14.sp
            )
            if (movement.priceTotal != null) {
                Text("$${String.format("%.0f", movement.priceTotal)}", fontSize = 11.sp, color = Color.Gray)
            }
        }
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
