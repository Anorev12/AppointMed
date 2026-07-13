package com.example.appointmed.features.patient.models
data class PasswordChangeRequest(
    val oldPassword: String,
    val newPassword: String,
    val confirmPassword: String
)