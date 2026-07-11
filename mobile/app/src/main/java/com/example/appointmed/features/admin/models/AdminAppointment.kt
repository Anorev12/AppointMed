package com.example.appointmed.features.admin.models
data class AdminAppointment(
    val dbId: Long,       // real DB id — needed for the override-cancel API call
    val id: String,       // human-readable reference shown to the admin, e.g. "APT-000123"
    val patient: String,
    val doctor: String,
    val date: String,
    val time: String,
    var status: String    // "confirmed" | "cancelled" | "completed" (lowercased for the adapter's checks)
)
