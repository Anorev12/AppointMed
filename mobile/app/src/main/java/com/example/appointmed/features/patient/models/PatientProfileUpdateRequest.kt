package com.example.appointmed.features.patient.models

data class PatientProfileUpdateRequest(
    val fullName: String,
    val contactNumber: String?,
    val medicalHistory: String?
)