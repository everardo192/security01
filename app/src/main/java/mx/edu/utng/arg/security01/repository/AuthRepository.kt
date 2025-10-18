package mx.edu.utng.arg.security01.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.edu.utng.arg.security01.models.LoginRequest
import mx.edu.utng.arg.security01.models.User
import mx.edu.utng.arg.security01.network.RetrofitClient
import mx.edu.utng.arg.security01.security.SecureStorage

class AuthRepository(private val context: Context) {

    private val secureStorage = SecureStorage(context)
    private val apiService by lazy { RetrofitClient.getApiService() }

    companion object {
        private const val TAG = "AuthRepository"
    }

    suspend fun login(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                // Validaciones
                if (email.isBlank() || password.isBlank()) {
                    return@withContext Result.failure(
                        Exception("El email y la contraseña son obligatorios")
                    )
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    return@withContext Result.failure(
                        Exception("El formato del email no es válido")
                    )
                }

                Log.d(TAG, "Intentando login para: ${email.replace(Regex("(?<=.).(?=.*@)"), "*")}")

                val loginRequest = LoginRequest(
                    email = email,
                    password = password,
                    deviceId = android.provider.Settings.Secure.getString(
                        context.contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID
                    )
                )

                val response = apiService.login(loginRequest)

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.success == true && loginResponse.user != null) {
                        val user = loginResponse.user
                        secureStorage.saveUserSession(user)
                        Log.d(TAG, "Login exitoso para: $email")
                        Result.success(user)
                    } else {
                        val errorMsg = loginResponse?.message ?: "Error en el servidor"
                        Log.w(TAG, "Login fallido: $errorMsg")
                        Result.failure(Exception(errorMsg))
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Credenciales incorrectas"
                        403 -> "Acceso denegado"
                        404 -> "Servicio no encontrado"
                        422 -> "Datos de entrada inválidos"
                        500 -> "Error interno del servidor"
                        503 -> "Servicio no disponible"
                        else -> "Error de conexión: ${response.code()}"
                    }
                    Log.e(TAG, "Error HTTP ${response.code()}: $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción en login", e)
                val errorMessage = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "Error de conexión. Verifica tu internet"
                    e.message?.contains("timeout") == true ->
                        "Tiempo de espera agotado"
                    else ->
                        "Error de conexión: ${e.localizedMessage}"
                }
                Result.failure(Exception(errorMessage))
            }
        }
    }

    suspend fun register(email: String, password: String, name: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                // Validaciones
                if (email.isBlank() || password.isBlank() || name.isBlank()) {
                    return@withContext Result.failure(
                        Exception("Todos los campos son obligatorios")
                    )
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    return@withContext Result.failure(
                        Exception("El formato del email no es válido")
                    )
                }

                if (password.length < 6) {
                    return@withContext Result.failure(
                        Exception("La contraseña debe tener al menos 6 caracteres")
                    )
                }

                val loginRequest = LoginRequest(
                    email = email,
                    password = password
                )

                val response = apiService.register(loginRequest)

                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse?.success == true && loginResponse.user != null) {
                        val user = loginResponse.user.copy(name = name)
                        secureStorage.saveUserSession(user)
                        Log.d(TAG, "Registro exitoso para: $email")
                        Result.success(user)
                    } else {
                        val errorMsg = loginResponse?.message ?: "Error en el registro"
                        Result.failure(Exception(errorMsg))
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        409 -> "El usuario ya existe"
                        else -> "Error en el registro: ${response.code()}"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción en registro", e)
                Result.failure(Exception("Error de conexión: ${e.localizedMessage}"))
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return secureStorage.isLoggedIn()
    }

    fun getCurrentUser(): User? {
        return secureStorage.getUserData()
    }

    suspend fun validateToken(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val token = secureStorage.getToken()
                if (token == null) {
                    return@withContext Result.success(false)
                }

                val response = apiService.validateToken("Bearer $token")
                if (response.isSuccessful && response.body()?.success == true) {
                    secureStorage.updateLastActivity()
                    Result.success(true)
                } else {
                    logout()
                    Result.success(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error validando token", e)
                Result.failure(e)
            }
        }
    }

    suspend fun refreshToken(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = secureStorage.getRefreshToken()
                if (refreshToken == null) {
                    return@withContext Result.success(false)
                }

                val response = apiService.refreshToken("Bearer $refreshToken")
                if (response.isSuccessful) {
                    response.body()?.user?.let { user ->
                        secureStorage.updateTokens(
                            user.token ?: "",
                            user.refreshToken
                        )
                        Result.success(true)
                    } ?: Result.success(false)
                } else {
                    logout()
                    Result.success(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refrescando token", e)
                Result.failure(e)
            }
        }
    }

    suspend fun logout(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val token = secureStorage.getToken()

                // Intentar cerrar sesión en el servidor
                if (token != null) {
                    try {
                        apiService.logout("Bearer $token")
                        Log.d(TAG, "Sesión cerrada en servidor")
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo cerrar sesión en servidor", e)
                        // Continuamos con el logout local aunque falle el del servidor
                    }
                }

                // Siempre limpiar datos locales
                secureStorage.clearSession()
                Log.d(TAG, "Sesión local limpiada")
                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error en logout", e)
                // Aún así limpiamos localmente
                secureStorage.clearSession()
                Result.failure(e)
            }
        }
    }

    suspend fun forgotPassword(email: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.forgotPassword(mapOf("email" to email))
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Error al enviar correo de recuperación"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en forgotPassword", e)
                Result.failure(Exception("Error de conexión"))
            }
        }
    }

    fun updateActivity() {
        secureStorage.updateLastActivity()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        secureStorage.setBiometricEnabled(enabled)
    }

    fun isBiometricEnabled(): Boolean {
        return secureStorage.isBiometricEnabled()
    }

    fun getSessionStats(): Map<String, Any> {
        return secureStorage.getSessionStats()
    }
}