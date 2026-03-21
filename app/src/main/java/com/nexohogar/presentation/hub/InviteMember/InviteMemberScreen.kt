package com.nexohogar.presentation.invitemember

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla de invitación de miembros al hogar.
 *
 * ESTADO: Placeholder — La funcionalidad completa (generación de código desde
 * Supabase) se implementará en la próxima iteración.
 *
 * FLUJO PLANEADO:
 *  1. El owner solicita un código de 8 caracteres (RPC: get_or_create_invite_code).
 *  2. El nuevo miembro ingresa el código en su app (RPC: join_household_by_code).
 *  3. Supabase valida el código, agrega al miembro en household_members y
 *     le asigna el rol 'user'.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteMemberScreen(
    onNavigateBack: () -> Unit
) {
    // TODO: Reemplazar con valor real desde ViewModel cuando esté implementado
    val inviteCode = "PRÓX-0000"
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invitar miembro") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Ícono decorativo
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(Color(0xFFE0F7FA), shape = RoundedCornerShape(44.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF00695C)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Invita a tu familia",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Comparte el código con la persona que quieres agregar.\nElla lo ingresará en su app para unirse a este hogar.",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Tarjeta con el código
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Código de invitación",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                        Text(
                            text = inviteCode,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(inviteCode))
                            copied = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar código",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (copied) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ Código copiado",
                    color = Color(0xFF2E7D32),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Banner informativo — funcionalidad pendiente
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚠️ Esta función estará disponible próximamente. El código de arriba es de muestra.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 13.sp,
                    color = Color(0xFF6D4C41),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
