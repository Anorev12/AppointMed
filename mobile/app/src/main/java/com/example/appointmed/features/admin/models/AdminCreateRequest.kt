package com.example.appointmed.features.admin.models

data class AdminCreateRequest(
    val fullName: String,
    val email: String,
    val password: String
)
