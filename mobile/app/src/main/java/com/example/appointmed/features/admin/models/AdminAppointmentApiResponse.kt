package com.example.appointmed.features.admin.models

/** Maps the JSON shape returned by /api/admin/appointments and /api/admin/patients/{id}/appointments. */
data class AdminAppointmentApiResponse(
    val id: Long,
    val reference: String,
    val doctorId: Long,
    val doctorName: String,
    val specialization: String?,
    val patientName: String,
    val date: String,
    val time: String,
    val status: String // "CONFIRMED" | "CANCELLED" | "COMPLETED"
)
