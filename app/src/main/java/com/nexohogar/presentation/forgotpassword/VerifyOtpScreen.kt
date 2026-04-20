package com.nexohogar.presentation.forgotpassword

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexohogar.core.di.ServiceLocator

/**
 * SEC-05: onVerified ahora recibe el accessToken como parámetro
 * para pasarlo como argumento de navegación (sin singleton).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyOtpScreen(
    email: String,
    onVerified: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: VerifyOtpViewModel = viewModel(
        factory = VerifyOtpViewModel.Factory(ServiceLocator.authRepository)
    )
) {
    val state by viewModel.state.collectAsState()
    var code by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // UX: Auto-focus al entrar
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // UX: Auto-verificación al completar los 8 dígitos
    LaunchedEffect(code) {
        if (code.length == 8) {
            focusManager.clearFocus()
            viewModel.verifyOtp(email, code)
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            val token = state.accessToken ?: ""
            onVerified(token)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verificación") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "📧",
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Revisa tu correo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enviamos un código de 8 dígitos a",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { newValue ->
                    if (newValue.length <= 8 && newValue.all { it.isDigit() }) {
                        code = newValue
                        viewModel.clearError()
                    }
                },
                label = { Text("Código de verificación") },
                placeholder = { Text("00000000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    letterSpacing = 8.sp
                ),
                isError = state.error != null
            )

            if (state.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.verifyOtp(email, code) },
                modifier = Modifier.fillMaxWidth(),
                enabled = code.length == 8 && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Verificar código")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // UX: Temporizador de reenvío
            TextButton(
                onClick = { viewModel.resendOtp(email) },
                enabled = state.canResend && !state.isLoading
            ) {
                if (state.canResend) {
                    Text("Reenviar código")
                } else {
                    Text("Reenviar código en ${state.resendTimer}s")
                }
            }
        }
    }
}
