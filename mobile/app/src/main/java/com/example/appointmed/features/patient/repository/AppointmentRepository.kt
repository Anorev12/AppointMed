package com.example.appointmed.features.patient.repository
import com.example.appointmed.features.patient.models.Doctor
import com.example.appointmed.features.patient.models.Appointment
import com.example.appointmed.features.patient.models.TimeSlot
import kotlin.random.Random

/**
 * In-memory mock data, mirroring the React PatientDashboard's local state
 * (DOCTORS, SLOTS_BY_DOCTOR, INITIAL_APPOINTMENTS). No booking API exists
 * on the backend yet, so this is a placeholder — same pattern as the
 * forgot-password button being wired but not functional.
 *
 * Singleton object so Home/Book/History fragments all see the same data
 * without needing a shared ViewModel.
 */
object AppointmentRepository {

    val doctors = listOf(
        Doctor(1, "Dr. Reyes", "Pediatrics"),
        Doctor(2, "Dr. Tan", "Internal Medicine"),
        Doctor(3, "Dr. Cruz", "Dermatology")
    )

    private val slotsByDoctor = mapOf(
        1 to mutableListOf(
            TimeSlot("09:00", false),
            TimeSlot("09:30", true),
            TimeSlot("10:00", false),
            TimeSlot("10:30", false)
        ),
        2 to mutableListOf(
            TimeSlot("10:00", false),
            TimeSlot("10:30", false),
            TimeSlot("11:00", true),
            TimeSlot("11:30", false)
        ),
        3 to mutableListOf(
            TimeSlot("13:00", false),
            TimeSlot("13:30", false),
            TimeSlot("14:00", true),
            TimeSlot("14:30", false)
        )
    )

    fun slotsFor(doctorId: Int): List<TimeSlot> = slotsByDoctor[doctorId].orEmpty()

    val appointments = mutableListOf(
        Appointment("APT-102938", "Dr. Reyes", "Pediatrics", "2026-07-10", "09:00", "confirmed"),
        Appointment("APT-100221", "Dr. Cruz", "Dermatology", "2026-06-28", "14:00", "confirmed"),
        Appointment("APT-099120", "Dr. Tan", "Internal Medicine", "2026-06-14", "11:00", "cancelled")
    )

    fun upcoming(): List<Appointment> = appointments.filter { it.status == "confirmed" }

    fun book(doctorId: Int, time: String): Appointment {
        val doctor = doctors.first { it.id == doctorId }
        val ref = "APT-${Random.nextInt(100000, 999999)}"
        val appointment = Appointment(
            id = ref,
            doctor = doctor.name,
            specialization = doctor.specialization,
            date = "2026-07-14",
            time = time,
            status = "confirmed"
        )
        appointments.add(0, appointment)
        return appointment
    }

    fun cancel(id: String) {
        appointments.find { it.id == id }?.status = "cancelled"
    }
}