package mx.edu.utng.arg.security01.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.edu.utng.arg.security01.models.AuthState
import mx.edu.utng.arg.security01.models.User
import mx.edu.utng.arg.security01.ui.components.LoadingButton
import mx.edu.utng.arg.security01.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AuthViewModel = viewModel(),
    onLogout: () -> Unit
) {
    // Estados y observables
    val authState by viewModel.authState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSessionInfo by remember { mutableStateOf(false) }

    // Observar cambios de estado
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Logout -> {
                onLogout()
            }
            else -> {}
        }
    }

    // Actualizar actividad del usuario
    DisposableEffect(Unit) {
        viewModel.updateUserActivity()
        onDispose { }
    }

    // UI Principal
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mi Perfil",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Botón de información de sesión
                    IconButton(onClick = { showSessionInfo = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Información de sesión",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Botón de logout
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de información del usuario
            currentUser?.let { user ->
                UserInfoCard(user = user)
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Sección de seguridad
            SecuritySection(viewModel = viewModel)
            Spacer(modifier = Modifier.height(32.dp))

            // Botones de acción
            ActionButtons(
                viewModel = viewModel,
                onShowSessionInfo = { showSessionInfo = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de cerrar sesión
            LoadingButton(
                text = "Cerrar Sesión",
                onClick = { showLogoutDialog = true },
                variant = mx.edu.utng.arg.security01.ui.components.ButtonVariant.Error,
                isLoading = authState is AuthState.Loading
            )
        }

        // Diálogo de confirmación de logout
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(
                        text = "Cerrar Sesión",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Text(
                        text = "¿Estás seguro de que deseas cerrar sesión?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logout()
                        }
                    ) {
                        Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo de información de sesión
        if (showSessionInfo) {
            SessionInfoDialog(
                viewModel = viewModel,
                onDismiss = { showSessionInfo = false }
            )
        }
    }
}

// CORRECCIÓN: Las funciones composables deben estar FUERA de la función principal HomeScreen
// y NO usar 'private' (no se puede usar private en funciones locales)

@Composable
fun UserInfoCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar del usuario
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre del usuario
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Email del usuario
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ID del usuario
            Text(
                text = "ID: ${user.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SecuritySection(viewModel: AuthViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Información de Seguridad",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Items de seguridad
            SecurityItem(
                title = "Sesión Encriptada",
                description = "Tus datos están protegidos con encriptación AES-256"
            )

            Spacer(modifier = Modifier.height(12.dp))

            SecurityItem(
                title = "Token de Autenticación",
                description = "Token JWT activo y verificado"
            )

            Spacer(modifier = Modifier.height(12.dp))

            SecurityItem(
                title = "Expiración de Sesión",
                description = "Tu sesión expirará después de 24 horas de inactividad"
            )
        }
    }
}

@Composable
fun SecurityItem(title: String, description: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.5f
        )
    }
}

@Composable
fun ActionButtons(
    viewModel: AuthViewModel,
    onShowSessionInfo: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Botón para validar token
        Button(
            onClick = { viewModel.validateToken() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Validar Token en Servidor")
        }

        // Botón para información de sesión
        Button(
            onClick = onShowSessionInfo,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Información de Sesión")
        }
    }
}

@Composable
fun SessionInfoDialog(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val sessionStats = remember { viewModel.getSessionStats() }
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Información de Sesión",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                sessionStats.forEach { (key, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (key) {
                                "sessionAge" -> "Tiempo de sesión:"
                                "inactivityTime" -> "Tiempo inactivo:"
                                "isValid" -> "Sesión válida:"
                                "isActive" -> "Sesión activa:"
                                else -> key
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = when {
                                key.contains("Time") || key.contains("Age") -> {
                                    "${value.toString().toLong() / 1000 / 60} min"
                                }
                                value is Boolean -> if (value) "Sí" else "No"
                                else -> value.toString()
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}