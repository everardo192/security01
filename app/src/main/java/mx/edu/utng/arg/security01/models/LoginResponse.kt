package mx.edu.utng.arg.security01.models

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: User? = null,
    val expiresIn: Long? = null
)