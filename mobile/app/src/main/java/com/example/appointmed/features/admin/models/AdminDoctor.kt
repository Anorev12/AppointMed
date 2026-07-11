package com.example.appointmed.features.admin.models
data class AdminDoctor(
    val id: Long,
    val name: String,
    val specialization: String,
    var status: String // "active" | "on leave"
)
