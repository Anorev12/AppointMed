package com.example.appointmed.models

/**
 * Response body returned by POST /api/auth/login (and used for
 * POST /api/auth/register as well, since both return the same shape).
 */
data class LoginResponse(
    val token: String,
    val id: Long,
    val fullName: String,
    val email: String,
    val contactNumber: String,
    val dateOfBirth: String,
    val role: String
)