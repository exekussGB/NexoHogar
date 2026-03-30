package com.nexohogar.presentation.login

import android.util.Patterns
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nexohogar.presentation.components.LoadingOverlay

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val state by viewModel.loginState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    fun validateEmail(): Boolean {
        emailError = when {
            email.isBlank() -> "El correo es obligatorio"
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Correo inválido"
            else -> null
        }
        return emailError == null
    }

    fun validatePassword(): Boolean {
        passwordError = when {
            password.isBlank() -> "La contraseña es obligatoria"
            password.length < 6 -> "Mínimo 6 caracteres"
            else -> null
        }
        return passwordError == null
    }

    LaunchedEffect(state) {
        if (state is LoginState.Success) onLoginSuccess()
        else if (state is LoginState.Error) {
            Toast.makeText(context, (state as LoginState.Error).message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "NexoHogar", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) validateEmail()
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) validatePassword()
                },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val emailValid = validateEmail()
                    val passValid = validatePassword()
                    if (emailValid && passValid) {
                        viewModel.login(email.trim(), password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is LoginState.Loading
            ) { Text("Iniciar Sesión") }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onNavigateToRegister, modifier = Modifier.fillMaxWidth()) {
                Text("¿No tienes cuenta? Regístrate")
            }

            TextButton(onClick = onNavigateToForgotPassword, modifier = Modifier.fillMaxWidth()) {
                Text("¿Olvidaste tu contraseña?")
            }
        }

        if (state is LoginState.Loading) LoadingOverlay()
    }
}
