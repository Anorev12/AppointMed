package com.example.appointmed.features.doctor.models
/**
 * Response body for GET /api/doctor/profile.
 * Mirrors the backend's DoctorResponse DTO — the one source of truth for
 * specialization, since the login/register AuthResponse never includes it.
 */
data class DoctorProfileResponse(
    val id: Long,
    val fullName: String,
    val email: String,
    val specialization: String?,
    val status: String
)
