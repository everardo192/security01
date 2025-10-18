package mx.edu.utng.arg.security01.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.edu.utng.arg.security01.models.AuthState
import mx.edu.utng.arg.security01.models.User
import mx.edu.utng.arg.security01.repository.AuthRepository

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application)

    // CORRECCIÓN: Cambiar a StateFlow para compatibilidad con Compose
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // CORRECCIÓN: Estado específico para forgot password como StateFlow
    private val _forgotPasswordState = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (repository.isLoggedIn()) {
                    val user = repository.getCurrentUser()
                    if (user != null) {
                        _currentUser.value = user
                        // Validar token con servidor en segundo plano
                        validateToken()
                    } else {
                        // Datos inconsistentes, limpiar sesión
                        repository.logout()
                        _authState.value = AuthState.Idle
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error checking session", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = repository.login(email, password)
            result.onSuccess { user ->
                _currentUser.value = user
                _authState.value = AuthState.Success(user)
                _errorMessage.value = null
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Error desconocido")
                _errorMessage.value = exception.message
            }
            _isLoading.value = false
        }
    }

    fun register(email: String, password: String, name: String) {
        _authState.value = AuthState.Loading
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = repository.register(email, password, name)
            result.onSuccess { user ->
                _currentUser.value = user
                _authState.value = AuthState.Success(user)
                _errorMessage.value = null
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Error en registro")
                _errorMessage.value = exception.message
            }
            _isLoading.value = false
        }
    }

    fun validateToken() {
        viewModelScope.launch {
            val result = repository.validateToken()
            result.onSuccess { isValid ->
                if (!isValid) {
                    // Token inválido, forzar logout
                    _authState.value = AuthState.TokenExpired
                    logout()
                }
            }.onFailure {
                // En caso de error de conexión, mantenemos sesión local
                Log.w("AuthViewModel", "Error validating token, keeping local session")
            }
        }
    }

    fun refreshToken() {
        viewModelScope.launch {
            val result = repository.refreshToken()
            result.onSuccess { success ->
                if (!success) {
                    _authState.value = AuthState.TokenExpired
                    logout()
                }
            }.onFailure {
                _authState.value = AuthState.TokenExpired
                logout()
            }
        }
    }

    fun logout() {
        _authState.value = AuthState.Loading
        _isLoading.value = true

        viewModelScope.launch {
            val result = repository.logout()
            result.onSuccess {
                _currentUser.value = null
                _authState.value = AuthState.Logout
                _errorMessage.value = null
                _forgotPasswordState.value = ForgotPasswordState.Idle
            }.onFailure { exception ->
                // Aunque falle el logout del servidor, limpiamos localmente
                _currentUser.value = null
                _authState.value = AuthState.Logout
                _errorMessage.value = exception.message
                _forgotPasswordState.value = ForgotPasswordState.Idle
            }
            _isLoading.value = false
        }
    }

    fun forgotPassword(email: String) {
        _isLoading.value = true
        _forgotPasswordState.value = ForgotPasswordState.Loading
        _errorMessage.value = null

        viewModelScope.launch {
            val result = repository.forgotPassword(email)
            result.onSuccess {
                _forgotPasswordState.value = ForgotPasswordState.Success
                _errorMessage.value = "Se ha enviado un correo para recuperar tu contraseña"
            }.onFailure { exception ->
                _forgotPasswordState.value = ForgotPasswordState.Error(
                    exception.message ?: "Error al enviar correo de recuperación"
                )
                _errorMessage.value = exception.message
            }
            _isLoading.value = false
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
        _errorMessage.value = null
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordState.Idle
    }

    fun updateUserActivity() {
        repository.updateActivity()
    }

    fun isLoggedIn(): Boolean {
        return repository.isLoggedIn()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        repository.setBiometricEnabled(enabled)
    }

    fun isBiometricEnabled(): Boolean {
        return repository.isBiometricEnabled()
    }

    fun getSessionStats(): Map<String, Any> {
        return repository.getSessionStats()
    }

    // Estado para forgot password
    sealed class ForgotPasswordState {
        object Idle : ForgotPasswordState()
        object Loading : ForgotPasswordState()
        object Success : ForgotPasswordState()
        data class Error(val message: String) : ForgotPasswordState()
    }
}