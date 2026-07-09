package com.example.appointmed.features.auth.api
import com.example.appointmed.features.auth.models.LoginRequest
import com.example.appointmed.features.auth.models.LoginResponse
import com.example.appointmed.features.auth.models.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Defines the authentication endpoints exposed by the Spring Boot backend.
 * Base URL (http://10.0.2.2:8080/) is configured separately in RetrofitClient.
 *
 * Both functions are `suspend` so they must be called from a Coroutine
 * (e.g. viewModelScope or lifecycleScope), keeping network calls off
 * the main thread automatically.
 *
 * Response<LoginResponse> (instead of a bare LoginResponse) is used so we
 * can inspect the HTTP status code manually — this lets us distinguish
 * between success (200) and error responses (401, 409, 400) and read
 * the error body accordingly in the Activities.
 */
interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

}