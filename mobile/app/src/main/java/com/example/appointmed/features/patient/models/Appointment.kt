package com.example.appointmed.features.patient.models

data class Appointment(
    val dbId: Long,     // real DB id — needed for the cancel/reschedule API calls
    val id: String,     // human-readable reference shown to the patient, e.g. "APT-000123"
    val doctorId: Long, // needed to re-query open slots when rescheduling
    val doctor: String,
    val specialization: String,
    val date: String,
    val time: String,
    var status: String,  // "confirmed" | "cancelled" | "completed" (lowercased for the adapter's checks)
    val needsReschedule: Boolean = false // FR-020: doctor went unavailable on this date after booking
)