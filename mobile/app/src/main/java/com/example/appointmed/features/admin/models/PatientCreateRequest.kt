package com.example.appointmed.features.admin.models

data class PatientCreateRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val contactNumber: String
)