package com.nexohogar.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexohogar.core.result.AppResult
import com.nexohogar.core.tenant.TenantContext
import com.nexohogar.data.local.SessionManager
import com.nexohogar.domain.repository.HouseholdRepository
import com.nexohogar.presentation.household.DeleteHouseholdConfirmDialog
import com.nexohogar.presentation.household.DeleteHouseholdFirstDialog
import com.nexohogar.presentation.household.DeleteHouseholdViewModel
import com.nexohogar.service.FcmTokenManager
import androidx.compose.ui.platform.testTag
import com.nexohogar.core.tutorial.TutorialManager
import com.nexohogar.core.tutorial.TutorialModule
import com.nexohogar.presentation.tutorial.TutorialOverlay
import com.nexohogar.core.di.ServiceLocator
import com.nexohogar.data.local.ThemePreferences
import com.nexohogar.data.local.NotificationPreferences
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    sessionManager: SessionManager,
    deleteHouseholdViewModel: DeleteHouseholdViewModel,
    householdRepository: HouseholdRepository,
    tenantContext: TenantContext,
    tutorialManager: TutorialManager,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onChangeHousehold: () -> Unit,
    onViewMembers: () -> Unit,
    onNavigateToTutorial: () -> Unit,
    onHouseholdDeleted: () -> Unit
) {
    val session = remember { sessionManager.fetchSession() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showTutorial by remember {
        mutableStateOf(!tutorialManager.isTutorialCompleted(TutorialModule.HOUSEHOLD))
    }

    // ── Theme & Notifications state ─────────────────────────────────────
    val themePreferences = remember { ServiceLocator.themePreferences }
    val notificationPreferences = remember { ServiceLocator.notificationPreferences }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    // ── Datos del hogar cargados internamente ───────────────────────────────
    val householdId = remember { tenantContext.getCurrentHouseholdId() ?: "" }
    var householdName by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    var dataLoaded by remember { mutableStateOf(false) }

    // Cargar nombre del hogar y rol del usuario al entrar
    LaunchedEffect(householdId) {
        if (householdId.isBlank()) return@LaunchedEffect

        // Obtener nombre del hogar
        when (val result = householdRepository.getHouseholds()) {
            is AppResult.Success -> {
                householdName = result.data
                    .firstOrNull { it.id == householdId }
                    ?.name ?: ""
            }
            is AppResult.Error -> { /* nombre queda vacío */ }
            is AppResult.Loading -> { /* esperando */ }
        }

        // Obtener rol del usuario actual
        val currentUserId = tenantContext.getCurrentUserId()
        if (currentUserId != null) {
            when (val result = householdRepository.getHouseholdMembers(householdId)) {
                is AppResult.Success -> {
                    userRole = result.data
                        .firstOrNull { it.userId == currentUserId }
                        ?.role ?: ""
                }
                is AppResult.Error -> { /* rol queda vacío */ }
                is AppResult.Loading -> { /* esperando */ }
            }
        }

        dataLoaded = true
    }

    // ── Estado del flujo de eliminación ──────────────────────────────────────
    val deleteState by deleteHouseholdViewModel.uiState.collectAsState()

    // Cuando se elimina exitosamente → navegar a pantalla de hogares
    LaunchedEffect(deleteState.isDeleted) {
        if (deleteState.isDeleted) {
            onHouseholdDeleted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Opciones") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Sección: Cuenta ──────────────────────────────────────────────
            if (session != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = session.email.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Cuenta activa",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = session.email,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // ── Sección: Hogar ───────────────────────────────────────────────
            SectionLabel("Hogar")
            Box(modifier = Modifier.testTag("household_info")) {
                SettingsItem(
                    icon      = Icons.Default.SwitchAccount,
                    iconColor = Color(0xFF1565C0),
                    title     = "Cambiar hogar",
                    subtitle  = "Seleccionar otro hogar",
                    onClick   = onChangeHousehold
                )
            }
            Box(modifier = Modifier.testTag("household_members")) {
                SettingsItem(
                    icon      = Icons.Default.Group,
                    iconColor = Color(0xFF00695C),
                    title     = "Ver miembros",
                    subtitle  = "Usuarios y solicitudes pendientes",
                    onClick   = onViewMembers
                )
            }


            // ── Eliminar hogar (solo super_user, y solo si ya se cargaron datos) ─
            if (dataLoaded && userRole == "super_user") {
                SettingsItem(
                    icon      = Icons.Default.DeleteForever,
                    iconColor = MaterialTheme.colorScheme.error,
                    title     = "Eliminar hogar",
                    subtitle  = "Eliminar \"$householdName\" y todos sus datos",
                    onClick   = {
                        deleteHouseholdViewModel.startDeleteFlow(
                            householdId = householdId,
                            householdName = householdName
                        )
                    }
                )
            }

            // ── Sección: Aplicación ──────────────────────────────────────────
            SectionLabel("Aplicación")
            SettingsItem(
                icon      = Icons.Default.School,
                iconColor = Color(0xFF1565C0),
                title     = "Tutoriales",
                subtitle  = "Repasa cómo funciona cada módulo",
                onClick   = onNavigateToTutorial
            )
            SettingsItem(
                icon      = Icons.Default.Palette,
                iconColor = Color(0xFF6A1B9A),
                title     = "Apariencia",
                subtitle  = when (themePreferences.themeMode) {
                    "light" -> "Tema claro"
                    "dark"  -> "Tema oscuro"
                    else    -> "Seguir al sistema"
                },
                onClick   = { showThemeDialog = true }
            )
            SettingsItem(
                icon      = Icons.Default.Notifications,
                iconColor = Color(0xFFF57F17),
                title     = "Notificaciones",
                subtitle  = run {
                    val count = listOf(
                        notificationPreferences.householdEnabled,
                        notificationPreferences.billsEnabled,
                        notificationPreferences.budgetEnabled,
                        notificationPreferences.inventoryEnabled,
                        notificationPreferences.generalEnabled
                    ).count { it }
                    "$count de 5 categorías activas"
                },
                onClick   = { showNotificationsDialog = true }
            )

            // ── Sección: Acerca de ───────────────────────────────────────────
            SectionLabel("Acerca de")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NexoHogar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gestión financiera familiar · Versión 1.2.6",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Controla ingresos, gastos, transferencias e inventario, añade presupuestos y tus cuentas del hogar en un solo lugar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Desarrollado por: ExEkUsS",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Contacto: contactonexohogar@proton.me",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Cerrar sesión ────────────────────────────────────────────────
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // ── Diálogo de confirmación de logout ────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon   = { Icon(Icons.Filled.Logout, contentDescription = null) },
            title  = { Text("Cerrar sesión") },
            text   = { Text("¿Seguro que quieres cerrar sesión? Deberás ingresar tus credenciales de nuevo.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        // Desregistrar token FCM antes de limpiar sesión
                        FcmTokenManager.unregisterToken(context)
                        sessionManager.clearSession()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sí, cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ── Diálogos de eliminación de hogar ─────────────────────────────────────
    if (deleteState.showFirstDialog) {
        DeleteHouseholdFirstDialog(
            householdName = deleteState.householdName,
            onConfirm     = { deleteHouseholdViewModel.confirmFirstStep() },
            onDismiss     = { deleteHouseholdViewModel.cancelDelete() }
        )
    }

    if (deleteState.showConfirmDialog) {
        DeleteHouseholdConfirmDialog(
            householdName = deleteState.householdName,
            isDeleting    = deleteState.isDeleting,
            errorMessage  = deleteState.errorMessage,
            onConfirm     = { typedName -> deleteHouseholdViewModel.confirmDelete(typedName) },
            onDismiss     = { deleteHouseholdViewModel.cancelDelete() }
        )
    }
    // ── Tutorial overlay ────────────────────────────────────────────────────
    if (showTutorial) {
        TutorialOverlay(
            module = TutorialModule.HOUSEHOLD,
            onComplete = {
                tutorialManager.markTutorialCompleted(TutorialModule.HOUSEHOLD)
                showTutorial = false
            },
            onSkip = {
                tutorialManager.markTutorialCompleted(TutorialModule.HOUSEHOLD)
                showTutorial = false
            }
        )
    }

    // ── Diálogo de tema ─────────────────────────────────────────────────────
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = themePreferences.themeMode,
            onSelect    = { mode -> themePreferences.setTheme(mode) },
            onDismiss   = { showThemeDialog = false }
        )
    }

    // ── Diálogo de notificaciones ───────────────────────────────────────────
    if (showNotificationsDialog) {
        NotificationSettingsDialog(
            preferences = notificationPreferences,
            onDismiss   = { showNotificationsDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes auxiliares
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text  = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick  = onClick,
        enabled  = enabled,
        colors   = CardDefaults.cardColors(
            containerColor         = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape  = MaterialTheme.shapes.small,
                color  = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) iconColor else iconColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f)
                )
            }
            if (enabled) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Diálogo: Selección de tema
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ThemeSelectionDialog(
    currentMode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        Triple("system", "Automático",  "Sigue la configuración del dispositivo"),
        Triple("light",  "Tema claro",  "Fondo blanco, texto oscuro"),
        Triple("dark",   "Tema oscuro", "Fondo oscuro, texto claro")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.Palette, contentDescription = null) },
        title = { Text("Apariencia", fontWeight = FontWeight.Bold) },
        text  = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { (mode, label, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentMode == mode,
                                onClick  = { onSelect(mode) },
                                role     = Role.RadioButton
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick  = null
                        )
                        Column {
                            Text(
                                text       = label,
                                style      = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text  = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Diálogo: Preferencias de notificaciones
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NotificationSettingsDialog(
    preferences: NotificationPreferences,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(Icons.Default.Notifications, contentDescription = null) },
        title = { Text("Notificaciones", fontWeight = FontWeight.Bold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text  = "Elige qué notificaciones quieres recibir:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                NotificationToggleRow(
                    icon    = Icons.Default.Home,
                    label   = "Hogar y miembros",
                    subtitle = "Solicitudes de unión, aceptación/rechazo",
                    checked = preferences.householdEnabled,
                    onToggle = { preferences.setHousehold(it) }
                )
                NotificationToggleRow(
                    icon    = Icons.Default.Repeat,
                    label   = "Gastos recurrentes",
                    subtitle = "Alertas de vencimiento próximo",
                    checked = preferences.billsEnabled,
                    onToggle = { preferences.setBills(it) }
                )
                NotificationToggleRow(
                    icon    = Icons.Default.AccountBalanceWallet,
                    label   = "Presupuestos",
                    subtitle = "Alertas cuando llegas al límite",
                    checked = preferences.budgetEnabled,
                    onToggle = { preferences.setBudget(it) }
                )
                NotificationToggleRow(
                    icon    = Icons.Default.Inventory,
                    label   = "Inventario",
                    subtitle = "Alertas de stock bajo",
                    checked = preferences.inventoryEnabled,
                    onToggle = { preferences.setInventory(it) }
                )
                NotificationToggleRow(
                    icon    = Icons.Default.Info,
                    label   = "General",
                    subtitle = "Otras notificaciones de la app",
                    checked = preferences.generalEnabled,
                    onToggle = { preferences.setGeneral(it) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Listo") }
        }
    )
}

@Composable
private fun NotificationToggleRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint     = if (checked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = label,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle
        )
    }
}
