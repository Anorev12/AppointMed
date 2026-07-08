package com.example.appointmed.models

/**
 * Request body for POST /api/auth/login
 * Matches exactly what the Spring Boot backend expects.
 */
data class LoginRequest(
    val email: String,
    val password: String
)