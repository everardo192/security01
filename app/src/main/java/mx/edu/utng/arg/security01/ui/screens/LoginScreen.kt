package mx.edu.utng.arg.security01.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.edu.utng.arg.security01.R
import mx.edu.utng.arg.security01.models.AuthState
import mx.edu.utng.arg.security01.ui.components.CustomTextField
import mx.edu.utng.arg.security01.ui.components.ErrorDialog
import mx.edu.utng.arg.security01.ui.components.LoadingButton
import mx.edu.utng.arg.security01.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    // Estados locales
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Observar ViewModel
    val authState by viewModel.authState.collectAsState()

    // Efectos
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                onLoginSuccess()
            }
            is AuthState.Error -> {
                errorMessage = (authState as AuthState.Error).message
                showErrorDialog = true
                viewModel.resetAuthState()
            }
            else -> {}
        }
    }

    // Función de validación
    fun validateFields(): Boolean {
        var isValid = true

        // Validar email
        when {
            email.isBlank() -> {
                emailError = "El email es obligatorio"
                isValid = false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailError = "Formato de email inválido"
                isValid = false
            }
            else -> emailError = null
        }

        // Validar password
        when {
            password.isBlank() -> {
                passwordError = "La contraseña es obligatoria"
                isValid = false
            }
            password.length < 6 -> {
                passwordError = "Mínimo 6 caracteres"
                isValid = false
            }
            else -> passwordError = null
        }

        return isValid
    }

    // Función de login
    fun performLogin() {
        if (validateFields()) {
            viewModel.login(email, password)
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // CORRECCIÓN: Reemplazar Image por Icon de Material
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Logo de seguridad",
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Bienvenido",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Inicia sesión para continuar",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Campos de texto
            CustomTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                label = "Correo electrónico",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                isError = emailError != null,
                errorMessage = emailError,
                enabled = authState !is AuthState.Loading
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                label = "Contraseña",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = { performLogin() },
                isError = passwordError != null,
                errorMessage = passwordError,
                enabled = authState !is AuthState.Loading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de login
            LoadingButton(
                text = "Iniciar Sesión",
                onClick = { performLogin() },
                isLoading = authState is AuthState.Loading,
                enabled = authState !is AuthState.Loading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Enlaces adicionales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onNavigateToForgotPassword,
                    enabled = authState !is AuthState.Loading
                ) {
                    Text("¿Olvidaste tu contraseña?")
                }

                TextButton(
                    onClick = onNavigateToRegister,
                    enabled = authState !is AuthState.Loading
                ) {
                    Text("Crear cuenta")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Información de versión
            Text(
                text = "Versión 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Diálogo de error
        if (showErrorDialog) {
            ErrorDialog(
                title = "Error de autenticación",
                message = errorMessage,
                onDismiss = { showErrorDialog = false }
            )
        }
    }
}