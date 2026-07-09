package com.example.appointmed.features.doctor.repository
import com.example.appointmed.features.doctor.models.DoctorAppointment
/**
 * Mock data for the doctor's appointments table — mirrors the React
 * DoctorDashboard's INITIAL_APPOINTMENTS. Confirm/decline updates only
 * local state; no PATCH /api/doctor/appointments endpoint exists yet.
 */
object DoctorAppointmentRepository {

    const val TODAY = "2026-07-10"

    val appointments = mutableListOf(
        DoctorAppointment("APT-102938", "Juan Dela Cruz", "2026-07-10", "09:00", "confirmed"),
        DoctorAppointment("APT-103012", "Maria Santos", "2026-07-10", "09:30", "pending"),
        DoctorAppointment("APT-102890", "Pedro Reyes", "2026-07-11", "10:00", "confirmed"),
        DoctorAppointment("APT-101877", "Ana Lim", "2026-06-30", "10:30", "cancelled")
    )

    fun todayCount() = appointments.count { it.date == TODAY && it.status != "cancelled" }
    fun pendingCount() = appointments.count { it.status == "pending" }

    fun updateStatus(id: String, status: String) {
        appointments.find { it.id == id }?.status = status
    }
}