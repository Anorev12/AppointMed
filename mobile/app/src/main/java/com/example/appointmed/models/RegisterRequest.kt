package com.example.appointmed.models

/**
 * Request body for POST /api/auth/register.
 * confirmPassword is intentionally NOT included — validated client-side only.
 */
data class RegisterRequest(
    val fullName: String,
    val dateOfBirth: String, // format: "YYYY-MM-DD"
    val contactNumber: String,
    val email: String,
    val password: String
)