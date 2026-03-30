// ═══════════════════════════════════════════════════════════════════════════════
// CAMBIOS EN HubScreen.kt — Integración del Tutorial
// ═══════════════════════════════════════════════════════════════════════════════
//
// Agregar estos cambios al HubScreen existente:
//
// 1. Agregar imports:
//    import androidx.compose.ui.platform.LocalContext
//    import com.nexohogar.presentation.tutorial.TutorialOverlay
//    import com.nexohogar.presentation.tutorial.TutorialPreferences
//    import com.nexohogar.presentation.tutorial.hubTutorialSteps
//
// 2. Dentro de la función HubScreen, agregar al inicio:

/*
    val context = LocalContext.current
    val tutorialPrefs = remember { TutorialPreferences(context) }
    var showTutorial by remember { mutableStateOf(!tutorialPrefs.isHubTutorialShown()) }
*/

// 3. Al final del composable (antes del cierre de la función), agregar:

/*
    // ── Tutorial del Hub ──────────────────────────────────────────────────
    if (showTutorial) {
        TutorialOverlay(
            steps = hubTutorialSteps,
            onDismiss = {
                showTutorial = false
                tutorialPrefs.setHubTutorialShown()
            }
        )
    }
*/

// ═══════════════════════════════════════════════════════════════════════════════
// CAMBIOS SIMILARES PARA InventoryScreen.kt:
// ═══════════════════════════════════════════════════════════════════════════════
//
// 1. Agregar imports:
//    import androidx.compose.ui.platform.LocalContext
//    import com.nexohogar.presentation.tutorial.TutorialOverlay
//    import com.nexohogar.presentation.tutorial.TutorialPreferences
//    import com.nexohogar.presentation.tutorial.inventoryTutorialSteps
//
// 2. En la función InventoryScreen, agregar al inicio:

/*
    val context = LocalContext.current
    val tutorialPrefs = remember { TutorialPreferences(context) }
    var showInventoryTutorial by remember { mutableStateOf(!tutorialPrefs.isInventoryTutorialShown()) }
*/

// 3. Al final del InventoryScreen (dentro del Scaffold, antes del cierre):

/*
    if (showInventoryTutorial) {
        TutorialOverlay(
            steps = inventoryTutorialSteps,
            onDismiss = {
                showInventoryTutorial = false
                tutorialPrefs.setInventoryTutorialShown()
            }
        )
    }
*/
