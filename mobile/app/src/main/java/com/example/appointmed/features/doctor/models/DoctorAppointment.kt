package com.example.appointmed.features.doctor.models
data class DoctorAppointment(
    val id: String,
    val patient: String,
    val date: String,
    val time: String,
    var status: String // "confirmed" | "pending" | "cancelled"
)