package com.nexohogar.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexohogar.core.util.PasswordValidator
import com.nexohogar.core.util.PasswordValidator.PasswordStrength

/**
 * Indicador visual de fortaleza de contraseña.
 * Muestra una barra de progreso con color y texto descriptivo.
 */
@Composable
fun PasswordStrengthIndicator(
    password: String,
    modifier: Modifier = Modifier
) {
    val result = remember(password) { PasswordValidator.validate(password) }

    if (password.isEmpty()) return

    val targetProgress = result.strength.score / 5f
    val progress by animateFloatAsState(targetValue = targetProgress, label = "progress")

    val color by animateColorAsState(
        targetValue = when (result.strength) {
            PasswordStrength.EMPTY -> Color.Transparent
            PasswordStrength.VERY_WEAK -> Color(0xFFD32F2F)
            PasswordStrength.WEAK -> Color(0xFFFF5722)
            PasswordStrength.FAIR -> Color(0xFFFFC107)
            PasswordStrength.STRONG -> Color(0xFF4CAF50)
            PasswordStrength.VERY_STRONG -> Color(0xFF2E7D32)
        },
        label = "color"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Barra de progreso
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = result.strength.label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }

        // Sugerencias
        if (result.suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            result.suggestions.forEach { suggestion ->
                Text(
                    text = "• $suggestion",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
