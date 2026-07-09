package com.example.appointmed.features.doctor.models
data class UpdateScheduleRequest(
    val workingDays: List<String>,
    val startTime: String,
    val endTime: String
)