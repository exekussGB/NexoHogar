package com.nexohogar.presentation.tutorial

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexohogar.core.tutorial.TutorialModule
import com.nexohogar.core.tutorial.TutorialStep
import com.nexohogar.core.tutorial.TutorialSteps

/**
 * Overlay de tutorial que oscurece el fondo y muestra pasos explicativos.
 * Se coloca como capa superior sobre cualquier pantalla.
 */
@Composable
fun TutorialOverlay(
    module: TutorialModule,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val steps = remember { TutorialSteps.getSteps(module) }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentStep = steps[currentStepIndex]
    val isLastStep = currentStepIndex == steps.size - 1
    val progress = (currentStepIndex + 1).toFloat() / steps.size

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "overlay_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Consumir clicks para bloquear interacción */ }
    ) {
        // Botón de saltar (esquina superior derecha)
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Saltar tutorial",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Saltar", color = Color.White)
        }

        // Indicador de progreso y paso
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text(
                text = module.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.width(120.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${currentStepIndex + 1} / ${steps.size}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        // Card central con la explicación
        AnimatedContent(
            targetState = currentStepIndex,
            modifier = Modifier.align(Alignment.Center),
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
            },
            label = "step_transition"
        ) { stepIndex ->
            val step = steps[stepIndex]
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Ícono circular del paso
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${stepIndex + 1}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón de siguiente / completar
                    Button(
                        onClick = {
                            if (isLastStep) {
                                onComplete()
                            } else {
                                currentStepIndex++
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLastStep) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("¡Entendido!")
                        } else {
                            Text("Siguiente")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}
