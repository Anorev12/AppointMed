package com.example.appointmed.features.doctor.models
data class DoctorAppointment(
    val dbId: Long,      // real DB id — needed for the cancel/complete API calls
    val id: String,      // human-readable reference shown to the doctor, e.g. "APT-000123"
    val patient: String,
    val date: String,
    val time: String,
    var status: String   // "confirmed" | "cancelled" | "completed" (lowercased for the adapter's checks)
)