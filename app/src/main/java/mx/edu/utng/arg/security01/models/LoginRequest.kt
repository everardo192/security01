package mx.edu.utng.arg.security01.models

data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String? = null
)