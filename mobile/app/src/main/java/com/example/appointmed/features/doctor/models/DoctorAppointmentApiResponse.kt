package com.example.appointmed.features.doctor.models

/** Maps the JSON shape returned by /api/doctor/appointments and its cancel/complete actions. */
data class DoctorAppointmentApiResponse(
    val id: Long,
    val reference: String,
    val patientName: String,
    val date: String,
    val time: String,
    val status: String // "CONFIRMED" | "CANCELLED" | "COMPLETED"
)
