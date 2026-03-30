package com.nexohogar.presentation.forgotpassword

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexohogar.presentation.components.LoadingOverlay

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVerifyOtp: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state) {
        when (state) {
            is ForgotPasswordState.Success -> {
                onNavigateToVerifyOtp(email)
            }
            is ForgotPasswordState.Error ->
                Toast.makeText(context, (state as ForgotPasswordState.Error).message, Toast.LENGTH_SHORT).show()
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Recuperar contraseña", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Ingresa tu correo y te enviaremos un código para restablecer tu contraseña.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.sendRecoveryEmail(email) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is ForgotPasswordState.Loading
            ) { Text("Enviar código de recuperación") }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                Text("Volver al inicio de sesión")
            }
        }

        if (state is ForgotPasswordState.Loading) LoadingOverlay()
    }
}
