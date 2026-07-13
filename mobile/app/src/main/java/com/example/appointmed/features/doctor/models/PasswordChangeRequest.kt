package com.example.appointmed.features.doctor.models

data class PasswordChangeRequest(
    val oldPassword: String,
    val newPassword: String,
    val confirmPassword: String
)