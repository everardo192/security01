package mx.edu.utng.arg.security01.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
import mx.edu.utng.arg.security01.security.SecureStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://api.tudominio.com/api/v1/"
    private const val TAG = "RetrofitClient"

    private lateinit var apiService: ApiService
    private var secureStorage: SecureStorage? = null

    fun initialize(context: Context) {
        secureStorage = SecureStorage(context)
        apiService = createRetrofit().create(ApiService::class.java)
    }

    fun getApiService(): ApiService {
        if (!this::apiService.isInitialized) {
            throw IllegalStateException("RetrofitClient no ha sido inicializado. Llama a initialize() primero.")
        }
        return apiService
    }

    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            val filteredMessage = filterSensitiveData(message)
            Log.d(TAG, filteredMessage)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                // Interceptor para agregar headers automáticamente
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "SecurityApp/1.0.0")

                // Agregar token de autorización si está disponible
                secureStorage?.getToken()?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }

                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .authenticator { route, response ->
                // Evitar loops infinitos - si ya intentamos refresh y falló
                if (response.request.header("Authorization") != null &&
                    response.priorResponse?.code == 401) {
                    Log.w(TAG, "Token refresh failed multiple times, clearing session")
                    secureStorage?.clearSession()
                    return@authenticator null
                }

                // Sincronizar para evitar múltiples refreshes simultáneos
                synchronized(this) {
                    // Verificar si otro thread ya actualizó el token
                    val currentToken = secureStorage?.getToken()
                    val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

                    // Si el token cambió, reintentar con el nuevo token
                    if (currentToken != null && currentToken != requestToken) {
                        return@authenticator response.request.newBuilder()
                            .header("Authorization", "Bearer $currentToken")
                            .build()
                    }

                    // Intentar refresh del token
                    secureStorage?.getRefreshToken()?.let { refreshToken ->
                        try {
                            Log.d(TAG, "Attempting to refresh token")
                            val refreshResponse = runBlocking {
                                apiService.refreshToken("Bearer $refreshToken")
                            }

                            if (refreshResponse.isSuccessful) {
                                refreshResponse.body()?.user?.let { user ->
                                    if (!user.token.isNullOrEmpty()) {
                                        secureStorage?.updateTokens(
                                            user.token,
                                            user.refreshToken
                                        )
                                        Log.d(TAG, "Token refreshed successfully")

                                        // Retornar la request con el nuevo token
                                        return@authenticator response.request.newBuilder()
                                            .header("Authorization", "Bearer ${user.token}")
                                            .build()
                                    } else {
                                        Log.e(TAG, "Refresh response token is null or empty")
                                        secureStorage?.clearSession()
                                    }
                                }
                            } else {
                                Log.e(TAG, "Token refresh failed with code: ${refreshResponse.code()}")
                                secureStorage?.clearSession()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error refreshing token", e)
                            secureStorage?.clearSession()
                        }
                    } ?: run {
                        Log.w(TAG, "No refresh token available")
                        secureStorage?.clearSession()
                    }

                    // Si llegamos aquí, el refresh falló
                    null
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun filterSensitiveData(message: String): String {
        var filtered = message

        // Ocultar passwords
        if (filtered.contains("\"password\"")) {
            filtered = filtered.replace(
                Regex("\"password\"\\s*:\\s*\"[^\"]*\""),
                "\"password\":\"***HIDDEN***\""
            )
        }

        // Ocultar tokens
        if (filtered.contains("Authorization")) {
            filtered = filtered.replace(
                Regex("Bearer [A-Za-z0-9._-]+"),
                "Bearer ****"
            )
        }

        if (filtered.contains("\"token\"")) {
            filtered = filtered.replace(
                Regex("\"token\"\\s*:\\s*\"([^\"]{4})[^\"]*([^\"]{4})\""),
                "\"token\":\"$1****$2\""
            )
        }

        if (filtered.contains("\"refreshToken\"")) {
            filtered = filtered.replace(
                Regex("\"refreshToken\"\\s*:\\s*\"([^\"]{4})[^\"]*([^\"]{4})\""),
                "\"refreshToken\":\"$1****$2\""
            )
        }

        // Ocultar emails en responses
        if (filtered.contains("\"email\"")) {
            filtered = filtered.replace(
                Regex("\"email\"\\s*:\\s*\"([^@]{2})[^\"]*@[^\"]*\""),
                "\"email\":\"$1****@domain.com\""
            )
        }

        return filtered
    }
}