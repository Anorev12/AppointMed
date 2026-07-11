package com.example.appointmed.features.patient.models

/** Payload for PUT /api/patient/appointments/{id}/reschedule (FR-011). */
data class AppointmentRescheduleRequest(
    val date: String, // "yyyy-MM-dd"
    val time: String  // "HH:mm"
)