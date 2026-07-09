package com.example.appointmed.features.admin.models
data class DoctorCreateRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val specialization: String
)