package com.example.appointmed.models

data class Appointment(
    val id: String,
    val doctor: String,
    val specialization: String,
    val date: String,
    val time: String,
    var status: String // "confirmed" | "cancelled"
)