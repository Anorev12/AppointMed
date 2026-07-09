package com.example.appointmed.features.patient.models
data class Appointment(
    val id: String,
    val doctor: String,
    val specialization: String,
    val date: String,
    val time: String,
    var status: String // "confirmed" | "cancelled"
)