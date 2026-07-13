package com.example.appointmed.core.utils
/**
 * Shared display formatter used across all three dashboards (admin,
 * doctor, patient) so every appointment time reads the same way everywhere.
 *
 * Times are always transmitted/stored as 24-hour "HH:mm" strings (so
 * sorting and comparisons stay simple); this only formats for display.
 */
fun formatTime12h(time24: String?): String {
    if (time24.isNullOrBlank() || !time24.contains(":")) return time24 ?: ""

    val parts = time24.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return time24
    val minute = parts.getOrNull(1) ?: "00"

    val period = if (hour >= 12) "PM" else "AM"
    val hour12 = if (hour % 12 == 0) 12 else hour % 12

    return "$hour12:$minute $period"
}
