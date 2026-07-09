package com.example.appointmed.features.admin.models
data class AdminAppointment(
    val id: String,
    val patient: String,
    val doctor: String,
    val date: String,
    val time: String,
    var status: String // "confirmed" | "pending" | "cancelled"
)