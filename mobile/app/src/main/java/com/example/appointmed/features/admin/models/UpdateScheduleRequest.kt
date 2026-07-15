package com.example.appointmed.features.admin.models
data class UpdateScheduleRequest(
    val workingDays: List<String>,
    val startTime: String,
    val endTime: String
)