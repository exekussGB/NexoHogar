package com.nexohogar.presentation.household

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Diálogo 1: Confirmación inicial para eliminar hogar.
 */
@Composable
fun DeleteHouseholdFirstDialog(
    householdName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "¿Eliminar hogar?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Estás a punto de eliminar el hogar \"$householdName\". " +
                            "Esta acción es IRREVERSIBLE y eliminará:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Todas las cuentas y transacciones", style = MaterialTheme.typography.bodySmall)
                Text("• Todos los presupuestos", style = MaterialTheme.typography.bodySmall)
                Text("• Todo el inventario", style = MaterialTheme.typography.bodySmall)
                Text("• Todos los gastos recurrentes", style = MaterialTheme.typography.bodySmall)
                Text("• Todos los miembros serán desvinculados", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Continuar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Diálogo 2: Confirmación con nombre del hogar.
 */
@Composable
fun DeleteHouseholdConfirmDialog(
    householdName: String,
    isDeleting: Boolean,
    errorMessage: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var typedName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val namesMatch = typedName.trim().equals(householdName.trim(), ignoreCase = true)

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Confirmar eliminación",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Para confirmar, escribe el nombre exacto del hogar:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"$householdName\"",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = typedName,
                    onValueChange = { typedName = it },
                    label = { Text("Nombre del hogar") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isDeleting,
                    isError = typedName.isNotEmpty() && !namesMatch
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(typedName) },
                enabled = namesMatch && !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Eliminar definitivamente")
                }
            }
        },
        dismissButton = {
            if (!isDeleting) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}
