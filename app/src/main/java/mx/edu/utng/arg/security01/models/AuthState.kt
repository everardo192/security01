package mx.edu.utng.arg.security01.models

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
    object Logout : AuthState()
    object TokenExpired : AuthState()
}