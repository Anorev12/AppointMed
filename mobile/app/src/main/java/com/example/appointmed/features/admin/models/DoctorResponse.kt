package com.example.appointmed.features.admin.models
data class DoctorResponse(
    val id: Long,
    val fullName: String,
    val email: String,
    val specialization: String,
    val status: String = "ACTIVE" // "ACTIVE" | "ON_LEAVE"
)