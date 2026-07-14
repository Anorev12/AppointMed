package com.example.appointmed.features.patient.models

/** Maps the JSON shape returned by /api/patient/appointments and the booking endpoint. */
data class AppointmentApiResponse(
    val id: Long,
    val reference: String,
    val doctorId: Long,
    val doctorName: String,
    val specialization: String,
    val date: String,
    val time: String,
    val status: String, // "CONFIRMED" | "CANCELLED" | "COMPLETED"
    val needsReschedule: Boolean = false
)