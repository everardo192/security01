package mx.edu.utng.arg.security01.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.edu.utng.arg.security01.models.User

class SecureStorage(context: Context) {

    companion object {
        private const val PREFS_NAME = "secure_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_SESSION_TIMESTAMP = "session_timestamp"
        private const val KEY_LAST_ACTIVITY = "last_activity"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

        // Tiempo de expiración: 24 horas
        private const val SESSION_TIMEOUT = 24 * 60 * 60 * 1000L
        // Tiempo de inactividad: 30 minutos
        private const val INACTIVITY_TIMEOUT = 30 * 60 * 1000L
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Guardar sesión completa
    fun saveUserSession(user: User) {
        sharedPreferences.edit().apply {
            putString(KEY_TOKEN, user.token)
            putString(KEY_REFRESH_TOKEN, user.refreshToken)
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_NAME, user.name)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
            putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
            apply()
        }
    }

    // Obtener token
    fun getToken(): String? {
        return if (isSessionValid()) {
            updateLastActivity()
            sharedPreferences.getString(KEY_TOKEN, null)
        } else {
            clearSession()
            null
        }
    }

    // Obtener refresh token
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    // Obtener datos del usuario
    fun getUserData(): User? {
        if (!isSessionValid()) {
            clearSession()
            return null
        }

        updateLastActivity()

        val token = sharedPreferences.getString(KEY_TOKEN, null)
        val refreshToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
        val id = sharedPreferences.getString(KEY_USER_ID, null)
        val email = sharedPreferences.getString(KEY_USER_EMAIL, null)
        val name = sharedPreferences.getString(KEY_USER_NAME, null)

        return if (token != null && id != null && email != null && name != null) {
            User(id, email, name, token, refreshToken)
        } else {
            null
        }
    }

    // Verificar sesión activa
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) &&
                isSessionValid() &&
                !isInactivityTimeout()
    }

    // Verificar validez de sesión
    private fun isSessionValid(): Boolean {
        val sessionTimestamp = sharedPreferences.getLong(KEY_SESSION_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()
        val sessionAge = currentTime - sessionTimestamp
        return sessionAge < SESSION_TIMEOUT
    }

    // Verificar tiempo de inactividad
    private fun isInactivityTimeout(): Boolean {
        val lastActivity = sharedPreferences.getLong(KEY_LAST_ACTIVITY, 0L)
        val currentTime = System.currentTimeMillis()
        val inactivityTime = currentTime - lastActivity
        return inactivityTime > INACTIVITY_TIMEOUT
    }

    // Actualizar última actividad
    fun updateLastActivity() {
        sharedPreferences.edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
    }

    // Actualizar tokens
    fun updateTokens(token: String, refreshToken: String? = null) {
        sharedPreferences.edit().apply {
            putString(KEY_TOKEN, token)
            refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
            putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
            apply()
        }
    }

    // Configurar biometría
    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    // Limpiar sesión
    fun clearSession() {
        sharedPreferences.edit().apply {
            remove(KEY_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_SESSION_TIMESTAMP)
            remove(KEY_LAST_ACTIVITY)
            apply()
        }
    }

    // Obtener estadísticas de sesión
    fun getSessionStats(): Map<String, Any> {
        val sessionTimestamp = sharedPreferences.getLong(KEY_SESSION_TIMESTAMP, 0L)
        val lastActivity = sharedPreferences.getLong(KEY_LAST_ACTIVITY, 0L)
        val currentTime = System.currentTimeMillis()

        return mapOf(
            "sessionAge" to (currentTime - sessionTimestamp),
            "inactivityTime" to (currentTime - lastActivity),
            "isValid" to isSessionValid(),
            "isActive" to !isInactivityTimeout()
        )
    }
}