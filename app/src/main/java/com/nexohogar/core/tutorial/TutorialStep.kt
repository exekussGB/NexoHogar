package com.nexohogar.core.tutorial

/**
 * Un paso individual del tutorial.
 */
data class TutorialStep(
    val title: String,
    val description: String,
    val targetTag: String  // Modifier.testTag() value para ubicar el elemento en la UI
)
