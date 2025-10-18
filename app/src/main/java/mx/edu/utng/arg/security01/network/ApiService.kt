package mx.edu.utng.arg.security01.network

import mx.edu.utng.arg.security01.models.LoginRequest
import mx.edu.utng.arg.security01.models.LoginResponse
import mx.edu.utng.arg.security01.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @GET("auth/validate")
    suspend fun validateToken(@Header("Authorization") token: String): Response<LoginResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Header("Authorization") refreshToken: String): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Unit>

    @GET("user/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<User>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: Map<String, String>): Response<Unit>
}