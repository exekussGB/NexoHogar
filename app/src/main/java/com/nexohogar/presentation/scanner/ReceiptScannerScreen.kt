package com.nexohogar.presentation.scanner

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.nexohogar.core.util.AppLogger
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
    onNavigateBack: () -> Unit
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
                    onRetakePhoto = viewModel::retakePhoto
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
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    onImageCaptured(bitmap)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error loading image from gallery", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera preview
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            AppLogger.e(TAG, "Camera bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Instruction overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )
                ) {
                    Text(
                        text = "📸 Tomar foto de la boleta",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Capture button
                IconButton(
                    onClick = {
                        val capture = imageCapture ?: return@IconButton
                        val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                        capture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                    if (bitmap != null) {
                                        onImageCaptured(bitmap)
                                    }
                                    file.delete()
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    AppLogger.e(TAG, "Photo capture failed", exception)
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Tomar foto",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Gallery button
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") }
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Seleccionar de galería")
                }
            }
        }
    } else {
        // No camera permission
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Se necesita permiso de cámara para escanear boletas",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Dar permiso")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Seleccionar de galería")
            }
        }
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
    onRetakePhoto: () -> Unit
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "⚠️ Total detectado en boleta:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "$${clpFormat.format(uiState.detectedTotal!!)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
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
                            onClick = onConfirmImport,
                            modifier = Modifier.weight(2f),
                            enabled = selectedItems.isNotEmpty() && uiState.selectedAccountId != null
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Confirmar e importar")
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
            // Store & date header
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = uiState.store,
                            onValueChange = onUpdateStore,
                            label = { Text("Tienda") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) }
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.receiptDate,
                            onValueChange = onUpdateDate,
                            label = { Text("Fecha (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                        )
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
                    onCheckedChange = { onToggle() }
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
