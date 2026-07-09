package com.example.appointmed.features.admin.repository
import com.example.appointmed.features.admin.models.AdminAppointment
import com.example.appointmed.features.admin.models.AdminDoctor
import com.example.appointmed.features.admin.models.AdminPatient
/**
 * Mock data mirroring the React AdminDashboard's INITIAL_PATIENTS /
 * INITIAL_DOCTORS / INITIAL_APPOINTMENTS. Only doctor creation is wired
 * to the real backend (/api/admin/doctors) — patients, the appointments
 * list, and doctor status toggling stay local until those endpoints exist.
 */
object AdminRepository {

    const val TODAY = "2026-07-10"

    val patients = mutableListOf(
        AdminPatient(1, "Juan Dela Cruz", "juan.delacruz@email.com", "0917 123 4567"),
        AdminPatient(2, "Maria Santos", "maria.santos@email.com", "0918 222 3333"),
        AdminPatient(3, "Ana Lim", "ana.lim@email.com", "0919 444 5555")
    )

    val doctors = mutableListOf(
        AdminDoctor(1, "Dr. Reyes", "Pediatrics", "active"),
        AdminDoctor(2, "Dr. Tan", "Internal Medicine", "active"),
        AdminDoctor(3, "Dr. Cruz", "Dermatology", "on leave")
    )

    val appointments = mutableListOf(
        AdminAppointment("APT-102938", "Juan Dela Cruz", "Dr. Reyes", "2026-07-10", "09:00", "confirmed"),
        AdminAppointment("APT-103012", "Maria Santos", "Dr. Reyes", "2026-07-10", "09:30", "pending"),
        AdminAppointment("APT-102890", "Ana Lim", "Dr. Tan", "2026-07-11", "10:00", "confirmed"),
        AdminAppointment("APT-101877", "Juan Dela Cruz", "Dr. Cruz", "2026-06-30", "10:30", "cancelled")
    )

    fun todayCount() = appointments.count { it.date == TODAY && it.status != "cancelled" }
    fun pendingCount() = appointments.count { it.status == "pending" }

    fun toggleDoctorStatus(id: Long) {
        doctors.find { it.id == id }?.let {
            it.status = if (it.status == "active") "on leave" else "active"
        }
    }

    fun overrideCancel(id: String) {
        appointments.find { it.id == id }?.status = "cancelled"
    }

    /** Called after a successful POST /api/admin/doctors, to reflect the new doctor locally. */
    fun addDoctor(id: Long, name: String, specialization: String) {
        doctors.add(AdminDoctor(id, name, specialization, "active"))
    }
}