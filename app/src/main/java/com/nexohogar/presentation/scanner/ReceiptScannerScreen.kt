package com.nexohogar.presentation.scanner

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.nexohogar.core.util.AppLogger
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.nexohogar.domain.model.ScannedReceiptItem
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import androidx.compose.runtime.getValue

private const val TAG = "ReceiptScanner"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerScreen(
    viewModel: ReceiptScannerViewModel,
    onNavigateBack: () -> Unit,
    onResult: (String?, Double?, String?, String?) -> Unit = { _, _, _, _ -> }
) {
    // Cuentas y categorías se cargan desde el ViewModel
    val accounts = viewModel.accounts.collectAsState().value
    val categories = viewModel.categories.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear Boleta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            val uiState by viewModel.uiState.collectAsState()
            when (uiState.step) {
                ScannerStep.CAMERA -> CameraStep(
                    onImageCaptured = { bitmap -> viewModel.processImage(bitmap) },
                    onNavigateBack = onNavigateBack
                )
                ScannerStep.PROCESSING -> ProcessingStep()
                ScannerStep.REVIEW -> ReviewStep(
                    uiState = uiState,
                    accounts = accounts,
                    categories = categories,
                    onToggleItem = viewModel::toggleItem,
                    onUpdateItem = viewModel::updateItem,
                    onRemoveItem = viewModel::removeItem,
                    onAddItem = viewModel::addItem,
                    onUpdateStore = viewModel::updateStore,
                    onUpdateDate = viewModel::updateDate,
                    onSetAccount = viewModel::setAccount,
                    onConfirmImport = viewModel::confirmImport,
                    onRetakePhoto = viewModel::retakePhoto,
                    onResult = onResult
                )
                ScannerStep.IMPORTING -> ImportingStep(
                    itemCount = uiState.items.count { it.isSelected }
                )
                ScannerStep.DONE -> DoneStep(
                    importResult = uiState.importResult,
                    onNavigateBack = onNavigateBack,
                    onReset = viewModel::reset
                )
                ScannerStep.ERROR -> ErrorStep(
                    error = uiState.error ?: "Error desconocido",
                    onRetry = viewModel::retakePhoto,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
}

@Composable
private fun CameraStep(
    onImageCaptured: (android.graphics.Bitmap) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // Configuración del Document Scanner de Google
    val options = remember {
        GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setPageLimit(1) // Solo una boleta a la vez
            .setGalleryImportAllowed(true)
            .build()
    }

    val scanner = remember {
        GmsDocumentScanning.getClient(options)
    }

    val haptic = LocalHapticFeedback.current

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
                val pages = scanResult?.pages
                if (pages != null && pages.isNotEmpty()) {
                    val page = pages[0]
                    val uri = page.imageUri
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onImageCaptured(bitmap)
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Scanner", "Error cargando imagen escaneada", e)
                    }
                }
            }
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        if (activity != null) {
            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    val request = IntentSenderRequest.Builder(intentSender).build()
                    scannerLauncher.launch(request)
                }
                .addOnFailureListener { e ->
                    AppLogger.e("Scanner", "Fallo al iniciar scanner nativo", e)
                }
        }
    }

    // Mientras se abre el scanner nativo, mostramos un indicador de carga
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ProcessingStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            "Analizando boleta...",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Reconociendo texto y productos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewStep(
    uiState: ReceiptScannerUiState,
    accounts: List<Pair<String, String>>,
    categories: List<Pair<String, String>>,
    onToggleItem: (Int) -> Unit,
    onUpdateItem: (Int, ScannedReceiptItem) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onAddItem: () -> Unit,
    onUpdateStore: (String) -> Unit,
    onUpdateDate: (String) -> Unit,
    onSetAccount: (String) -> Unit,
    onConfirmImport: () -> Unit,
    onRetakePhoto: () -> Unit,
    onResult: (String?, Double?, String?, String?) -> Unit
) {
    val clpFormat = remember {
        NumberFormat.getNumberInstance(Locale("es", "CL")).apply {
            maximumFractionDigits = 0
        }
    }

    val selectedItems = uiState.items.filter { it.isSelected }
    val calculatedTotal = selectedItems.sumOf { it.priceTotal ?: 0.0 }
    val totalMismatch = uiState.detectedTotal != null &&
            abs(calculatedTotal - uiState.detectedTotal) > 1.0

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Total comparison
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total seleccionado:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "$${clpFormat.format(calculatedTotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (totalMismatch) {
                        Spacer(Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "⚠️ El total no coincide",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Detectado: $${clpFormat.format(uiState.detectedTotal!!)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                TextButton(
                                    onClick = onAddItem,
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Ajustar", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Error message
                    uiState.error?.let { error ->
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRetakePhoto,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Retomar")
                        }
                        Button(
                            onClick = {
                                if (uiState.mode == ScannerMode.TRANSACTION) {
                                    onResult(
                                        uiState.store,
                                        uiState.detectedTotal ?: calculatedTotal,
                                        uiState.receiptDate,
                                        uiState.suggestedCategory
                                    )
                                } else {
                                    onConfirmImport()
                                }
                            },
                            modifier = Modifier.weight(2f),
                            enabled = (uiState.mode == ScannerMode.TRANSACTION && (uiState.detectedTotal != null || calculatedTotal > 0)) ||
                                    (selectedItems.isNotEmpty() && uiState.selectedAccountId != null)
                        ) {
                            Icon(
                                if (uiState.mode == ScannerMode.TRANSACTION) Icons.Default.Done else Icons.Default.Check,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (uiState.mode == ScannerMode.TRANSACTION) "Usar estos datos" else "Confirmar e importar"
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Información del Documento (Enfoque Inteligente) ──────────
            item {
                Text(
                    "📄 Información del Documento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = uiState.store,
                            onValueChange = onUpdateStore,
                            label = { Text("Comercio / Tienda") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.receiptDate,
                                onValueChange = onUpdateDate,
                                label = { Text("Fecha") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                            )
                            
                            if (uiState.issuerRut != null) {
                                OutlinedTextField(
                                    value = uiState.issuerRut,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("RUT Emisor") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                                )
                            }
                        }

                        if (uiState.documentNumber != null || uiState.documentType != null) {
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (uiState.documentType != null) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(uiState.documentType, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                                if (uiState.documentNumber != null) {
                                    Text(
                                        "N°: ${uiState.documentNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }
                        }
                        
                        if (uiState.suggestedCategory != null) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF673AB7),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Categoría sugerida: ${uiState.suggestedCategory}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF673AB7),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Account selector
            item {
                var accountExpanded by remember { mutableStateOf(false) }
                val selectedAccountName = accounts.find { it.first == uiState.selectedAccountId }?.second ?: ""

                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccountName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cuenta *") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) }
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        accounts.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    onSetAccount(id)
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Items header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Productos detectados (${uiState.items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${selectedItems.size} seleccionados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Item cards — ahora cada uno recibe la lista de categorías
            itemsIndexed(uiState.items) { index, item ->
                ReceiptItemCard(
                    item = item,
                    categories = categories,
                    onToggle = { onToggleItem(index) },
                    onUpdate = { updated -> onUpdateItem(index, updated) },
                    onRemove = { onRemoveItem(index) }
                )
            }

            // Spacer for FAB
            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptItemCard(
    item: ScannedReceiptItem,
    categories: List<Pair<String, String>>,
    onToggle: () -> Unit,
    onUpdate: (ScannedReceiptItem) -> Unit,
    onRemove: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle() 
                    }
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name.ifBlank { "(Sin nombre)" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = buildString {
                            val qtyDisplay = if (item.quantity == item.quantity.toInt().toDouble())
                                item.quantity.toInt().toString() else item.quantity.toString()
                            append("$qtyDisplay ${item.unit}")
                            item.pricePerUnit?.let { append(" × $${it.toInt()}") }
                            item.priceTotal?.let { append(" = $${it.toInt()}") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Mostrar categoría asignada en vista colapsada
                    if (!isExpanded && item.category != null) {
                        Text(
                            text = "📁 ${item.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Editar"
                    )
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Expandable edit fields
            if (isExpanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                // ─── Estado local para permitir edición libre ───────────
                var qtyText by remember(item.quantity) {
                    mutableStateOf(
                        if (item.quantity == item.quantity.toInt().toDouble())
                            item.quantity.toInt().toString()
                        else item.quantity.toString()
                    )
                }
                var unitPriceText by remember(item.pricePerUnit) {
                    mutableStateOf(item.pricePerUnit?.toInt()?.toString() ?: "")
                }
                var totalPriceText by remember(item.priceTotal) {
                    mutableStateOf(item.priceTotal?.toInt()?.toString() ?: "")
                }

                // ─── Fila 1: Cantidad + Unidad ─────────────────────────
                val unitOptions = listOf("un", "kg", "g", "ml", "L", "cc", "mt")
                var unitExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { value ->
                            qtyText = value
                            val qty = value.toDoubleOrNull()
                            if (qty != null && qty > 0) {
                                val newTotal = item.pricePerUnit?.let { it * qty }
                                onUpdate(item.copy(quantity = qty, priceTotal = newTotal ?: item.priceTotal))
                            }
                        },
                        label = { Text("Cantidad") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it },
                        modifier = Modifier.weight(0.8f)
                    ) {
                        OutlinedTextField(
                            value = item.unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidad") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            unitOptions.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = {
                                        onUpdate(item.copy(unit = unit))
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // ─── Fila 2: P. Unitario + P. Total ────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = unitPriceText,
                        onValueChange = { value ->
                            unitPriceText = value
                            val price = value.toDoubleOrNull()
                            if (price != null) {
                                val newTotal = price * item.quantity
                                onUpdate(item.copy(pricePerUnit = price, priceTotal = newTotal))
                            }
                        },
                        label = { Text("P. Unitario") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = totalPriceText,
                        onValueChange = { value ->
                            totalPriceText = value
                            val total = value.toDoubleOrNull()
                            if (total != null) {
                                val newUnit = if (item.quantity > 0) total / item.quantity else item.pricePerUnit
                                onUpdate(item.copy(priceTotal = total, pricePerUnit = newUnit ?: item.pricePerUnit))
                            }
                        },
                        label = { Text("P. Total") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ─── Categoría por producto ─────────────────────────────
                var categoryExpanded by remember { mutableStateOf(false) }
                val selectedCategoryName = if (item.categoryId != null) {
                    categories.find { it.first == item.categoryId }?.second ?: item.category ?: "Sin categoría"
                } else {
                    item.category ?: "Sin categoría"
                }

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) }
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        // Opción "Sin categoría"
                        DropdownMenuItem(
                            text = { Text("Sin categoría") },
                            onClick = {
                                onUpdate(item.copy(categoryId = null, category = null))
                                categoryExpanded = false
                            }
                        )
                        // Categorías existentes del hogar
                        categories.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    onUpdate(item.copy(categoryId = id, category = name))
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportingStep(itemCount: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            "Importando $itemCount productos...",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun DoneStep(
    importResult: Map<String, Any>?,
    onNavigateBack: () -> Unit,
    onReset: () -> Unit
) {
    val clpFormat = remember {
        NumberFormat.getNumberInstance(Locale("es", "CL")).apply {
            maximumFractionDigits = 0
        }
    }

    val productsImported = (importResult?.get("products_imported") as? Number)?.toInt() ?: 0
    val newProducts = (importResult?.get("new_products_created") as? Number)?.toInt() ?: 0
    val totalAmount = (importResult?.get("total_amount") as? Number)?.toDouble() ?: 0.0

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "¡Importación exitosa!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResultRow("Productos importados", "$productsImported")
                ResultRow("Productos nuevos creados", "$newProducts")
                ResultRow("Total", "$${clpFormat.format(totalAmount)}")
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Inventory, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Volver al inventario")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Escanear otra boleta")
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ErrorStep(
    error: String,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Error",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            error,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Reintentar")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}
