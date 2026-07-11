package com.example.appointmed.features.admin.models

/** Maps the JSON shape returned by /api/admin/patients. */
data class AdminPatientApiResponse(
    val id: Long,
    val fullName: String,
    val email: String,
    val contactNumber: String?
)