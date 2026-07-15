package com.example.appointmed.features.admin.models
data class AvailabilityResponse(
    val workingDays: List<String>,
    val startTime: String,
    val endTime: String,
    val unavailableDates: List<String>
)
