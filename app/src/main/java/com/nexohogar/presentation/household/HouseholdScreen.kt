package com.nexohogar.presentation.household

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nexohogar.domain.model.Household
import com.nexohogar.presentation.components.LoadingOverlay

// ---------------------------------------------------------------------------
// Gradientes predefinidos para fondos de hogares
// ---------------------------------------------------------------------------
data class HouseholdGradient(
    val name: String,
    val colors: List<Color>
)

val householdGradients = listOf(
    HouseholdGradient("Azul Clásico",    listOf(Color(0xFF1565C0), Color(0xFF42A5F5))),
    HouseholdGradient("Verde Bosque",     listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))),
    HouseholdGradient("Atardecer",        listOf(Color(0xFFE65100), Color(0xFFFF9800))),
    HouseholdGradient("Morado Real",      listOf(Color(0xFF6A1B9A), Color(0xFFBA68C8))),
    HouseholdGradient("Rojo Pasión",      listOf(Color(0xFFC62828), Color(0xFFEF5350))),
    HouseholdGradient("Océano",           listOf(Color(0xFF0277BD), Color(0xFF4FC3F7))),
    HouseholdGradient("Esmeralda",        listOf(Color(0xFF00695C), Color(0xFF4DB6AC))),
    HouseholdGradient("Dorado",           listOf(Color(0xFFFF8F00), Color(0xFFFFD54F))),
    HouseholdGradient("Nocturno",         listOf(Color(0xFF1A237E), Color(0xFF5C6BC0))),
    HouseholdGradient("Rosa Suave",       listOf(Color(0xFFAD1457), Color(0xFFF48FB1)))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdScreen(
    viewModel: HouseholdViewModel,
    onHouseholdSelected: (String) -> Unit,
    onSessionExpired: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAppearancePicker by remember { mutableStateOf<String?>(null) } // householdId

    // Navegar al dashboard cuando se crea exitosamente
    LaunchedEffect(uiState.createSuccess) {
        if (uiState.createSuccess) {
            viewModel.clearCreateSuccess()
            val newHousehold = uiState.households.lastOrNull()
            if (newHousehold != null) {
                onHouseholdSelected(newHousehold.id)
            }
        }
    }

    // Redirigir al login cuando la sesión expira definitivamente
    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) {
            viewModel.clearSessionExpired()
            onSessionExpired()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Hogares") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { viewModel.onShowJoinDialog() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = "Unirse con código")
                }
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Crear Hogar")
                }
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

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadHouseholds() }
                    )
                }

                uiState.households.isEmpty() -> {
                    EmptyHouseholdState(
                        onCreateClick = { showCreateDialog = true },
                        onJoinClick = { viewModel.onShowJoinDialog() }
                    )
                }

                else -> {
                    HouseholdList(
                        households = uiState.households,
                        onHouseholdClick = { onHouseholdSelected(it.id) },
                        onChangeAppearance = { householdId -> showAppearancePicker = householdId }
                    )
                }
            }

            // Overlay de subida de imagen
            if (uiState.isUploadingImage) {
                LoadingOverlay()
            }
        }
    }

    // ── Diálogo de creación ──────────────────────────────────────────────────
    if (showCreateDialog) {
        CreateHouseholdDialog(
            isCreating = uiState.isCreating,
            errorMessage = uiState.createError,
            onConfirm = { name -> viewModel.createHousehold(name) },
            onDismiss = {
                showCreateDialog = false
                viewModel.clearCreateError()
            }
        )
    }

    // ── Diálogo de apariencia (gradiente + imagen) ───────────────────────────
    showAppearancePicker?.let { householdId ->
        val household = uiState.households.firstOrNull { it.id == householdId }
        AppearancePickerDialog(
            currentGradientIndex = household?.gradientIndex ?: 0,
            currentImageUrl = household?.imageUri,
            onSelectGradient = { index ->
                viewModel.updateGradient(householdId, index)
                showAppearancePicker = null
            },
            onSelectImage = { imageBytes, mimeType ->
                viewModel.uploadImage(householdId, imageBytes, mimeType)
                showAppearancePicker = null
            },
            onDismiss = { showAppearancePicker = null }
        )
    }

    // ── Diálogo de unión con código ──────────────────────────────────────────
    if (uiState.showJoinDialog) {
        JoinHouseholdDialog(
            code = uiState.joinCode,
            isJoining = uiState.isJoining,
            errorMessage = uiState.joinError,
            onCodeChange = { viewModel.onJoinCodeChange(it) },
            onConfirm = { viewModel.joinHousehold() },
            onDismiss = { viewModel.onDismissJoinDialog() }
        )
    }

    // ── Snackbar de error de upload ──────────────────────────────────────────
    uiState.uploadError?.let { error ->
        LaunchedEffect(error) {
            // Show briefly then clear
            kotlinx.coroutines.delay(3000)
            viewModel.clearUploadError()
        }
    }
}

// ---------------------------------------------------------------------------
// Lista de hogares (visual grid)
// ---------------------------------------------------------------------------

@Composable
fun HouseholdList(
    households: List<Household>,
    onHouseholdClick: (Household) -> Unit,
    onChangeAppearance: (String) -> Unit = {}
) {
    if (households.size == 1) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            HouseholdItem(
                household = households.first(),
                onClick = onHouseholdClick,
                onChangeAppearance = { onChangeAppearance(households.first().id) }
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(households.size) { index ->
                val h = households[index]
                HouseholdItem(
                    household = h,
                    onClick = onHouseholdClick,
                    onChangeAppearance = { onChangeAppearance(h.id) }
                )
            }
        }
    }
}

@Composable
fun HouseholdItem(
    household: Household,
    onClick: (Household) -> Unit,
    onChangeAppearance: () -> Unit = {}
) {
    val gradient = householdGradients[household.gradientIndex.coerceIn(0, householdGradients.size - 1)]
    val hasImage = !household.imageUri.isNullOrBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick(household) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasImage) {
                // Fondo con imagen del usuario
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(household.imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Fondo del hogar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fondo con gradiente
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = Brush.linearGradient(gradient.colors))
                )
                // House icon centered
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center)
                )
            }

            // Botón para cambiar fondo (arriba a la derecha)
            IconButton(
                onClick = onChangeAppearance,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(36.dp)
            ) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = "Cambiar fondo",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Bottom scrim with name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = household.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    household.description?.let { desc ->
                        if (desc.isNotBlank()) {
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Diálogo de apariencia (gradiente + imagen)
// ---------------------------------------------------------------------------
@Composable
fun AppearancePickerDialog(
    currentGradientIndex: Int,
    currentImageUrl: String?,
    onSelectGradient: (Int) -> Unit,
    onSelectImage: (ByteArray, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null && bytes.isNotEmpty()) {
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    onSelectImage(bytes, mimeType)
                }
            } catch (_: Exception) {
                // Silently fail — user can try again
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personalizar fondo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // ── Sección de imagen ────────────────────────────────
                Text(
                    "Imagen personalizada",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Elegir imagen de galería")
                }

                if (!currentImageUrl.isNullOrBlank()) {
                    Text(
                        "✓ Imagen actual aplicada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider()

                // ── Sección de gradientes ────────────────────────────
                Text(
                    "O elige un color de fondo",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                // Grid de gradientes 5x2
                for (row in householdGradients.chunked(5)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEachIndexed { _, gradient ->
                            val index = householdGradients.indexOf(gradient)
                            val isSelected = index == currentGradientIndex && currentImageUrl.isNullOrBlank()
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(gradient.colors),
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectGradient(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxSize()
                                    ) {}
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Seleccionado",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Show name of currently selected gradient
                if (currentImageUrl.isNullOrBlank()) {
                    Text(
                        text = householdGradients[currentGradientIndex.coerceIn(0, householdGradients.size - 1)].name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// ---------------------------------------------------------------------------
// Estado vacío
// ---------------------------------------------------------------------------

@Composable
fun EmptyHouseholdState(
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No tienes hogares aún",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Crea un nuevo hogar o únete a uno existente con un código de invitación",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crear Hogar")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onJoinClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.VpnKey, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("¿Tienes código de invitación?")
        }
    }
}

// ---------------------------------------------------------------------------
// Diálogos
// ---------------------------------------------------------------------------

@Composable
fun CreateHouseholdDialog(
    isCreating: Boolean,
    errorMessage: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var householdName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Nuevo Hogar") },
        text = {
            Column {
                OutlinedTextField(
                    value = householdName,
                    onValueChange = { householdName = it },
                    label = { Text("Nombre del hogar") },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(householdName) },
                enabled = !isCreating && householdName.isNotBlank()
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Crear")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun JoinHouseholdDialog(
    code: String,
    isJoining: Boolean,
    errorMessage: String?,
    onCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isJoining) onDismiss() },
        title = { Text("Unirse a un hogar") },
        text = {
            Column {
                Text(
                    text = "Ingresa el código de invitación que te compartieron",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { onCodeChange(it.uppercase()) },
                    label = { Text("Código de invitación") },
                    singleLine = true,
                    enabled = !isJoining,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    placeholder = { Text("Ej: ABC12345") }
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isJoining && code.isNotBlank()
            ) {
                if (isJoining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Unirse")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isJoining
            ) {
                Text("Cancelar")
            }
        }
    )
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
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}
