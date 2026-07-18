package com.example.appointmed.features.admin.models

data class NotificationTemplateResponse(
    val type: String,
    val label: String,
    val subjectTemplate: String,
    val customMessage: String,
    val availablePlaceholders: String
)