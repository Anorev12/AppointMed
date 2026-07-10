package com.example.appointmed.features.patient.models

data class PatientProfileResponse(
    val id: Long,
    val fullName: String,
    val email: String,
    val contactNumber: String?,
    val dateOfBirth: String?,
    val medicalHistory: String?
)