package mx.edu.utng.arg.security01.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.edu.utng.arg.security01.ui.components.CustomTextField
import mx.edu.utng.arg.security01.ui.components.ErrorDialog
import mx.edu.utng.arg.security01.ui.components.LoadingButton
import mx.edu.utng.arg.security01.ui.components.SuccessDialog
import mx.edu.utng.arg.security01.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // CORRECCIÓN: Usar el estado específico de forgot password
    val forgotPasswordState by viewModel.forgotPasswordState.collectAsState()

    // Observar cambios en el estado de forgot password
    LaunchedEffect(forgotPasswordState) {
        when (forgotPasswordState) {
            is AuthViewModel.ForgotPasswordState.Success -> {
                showSuccessDialog = true
                viewModel.resetForgotPasswordState()
            }
            is AuthViewModel.ForgotPasswordState.Error -> {
                errorMessage = (forgotPasswordState as AuthViewModel.ForgotPasswordState.Error).message
                showErrorDialog = true
                viewModel.resetForgotPasswordState()
            }
            else -> {}
        }
    }

    fun validateEmail(): Boolean {
        return when {
            email.isBlank() -> {
                emailError = "El email es obligatorio"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailError = "Formato de email inválido"
                false
            }
            else -> {
                emailError = null
                true
            }
        }
    }

    fun sendRecoveryEmail() {
        if (validateEmail()) {
            viewModel.forgotPassword(email)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar Contraseña") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icono e instrucciones
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Recuperar Contraseña",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Ingresa tu correo electrónico y te enviaremos un enlace para restablecer tu contraseña.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Campo de email
                CustomTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                    },
                    label = "Correo electrónico",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    onImeAction = { sendRecoveryEmail() },
                    isError = emailError != null,
                    errorMessage = emailError,
                    enabled = forgotPasswordState !is AuthViewModel.ForgotPasswordState.Loading
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Botón de enviar
                LoadingButton(
                    text = "Enviar Enlace",
                    onClick = { sendRecoveryEmail() },
                    isLoading = forgotPasswordState is AuthViewModel.ForgotPasswordState.Loading,
                    enabled = forgotPasswordState !is AuthViewModel.ForgotPasswordState.Loading
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Enlace para volver al login
                TextButton(
                    onClick = onNavigateToLogin,
                    enabled = forgotPasswordState !is AuthViewModel.ForgotPasswordState.Loading
                ) {
                    Text("Volver al Inicio de Sesión")
                }
            }

            // Diálogo de error
            if (showErrorDialog) {
                ErrorDialog(
                    title = "Error",
                    message = errorMessage,
                    onDismiss = { showErrorDialog = false }
                )
            }

            // Diálogo de éxito
            if (showSuccessDialog) {
                SuccessDialog(
                    title = "Correo Enviado",
                    message = "Se ha enviado un enlace de recuperación a tu correo electrónico.",
                    onDismiss = {
                        showSuccessDialog = false
                        onNavigateToLogin()
                    }
                )
            }
        }
    }
}