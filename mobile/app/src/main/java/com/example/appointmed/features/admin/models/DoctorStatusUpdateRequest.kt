package com.example.appointmed.features.admin.models

data class DoctorStatusUpdateRequest(
    val status: String // "ACTIVE" | "ON_LEAVE"
)