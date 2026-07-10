package com.example.appointmed.features.patient.models

data class AppointmentBookRequest(
    val doctorId: Long,
    val date: String, // "yyyy-MM-dd"
    val time: String  // "HH:mm"
)