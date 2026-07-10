package com.example.appointmed.features.patient.models

/** Maps GET /api/doctors — one entry in the doctor roster patients can book with. */
data class DoctorApiResponse(
    val id: Long,
    val fullName: String,
    val email: String,
    val specialization: String
)