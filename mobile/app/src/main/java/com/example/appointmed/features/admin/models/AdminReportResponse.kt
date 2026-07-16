package com.example.appointmed.features.admin.models


/**
 * FR-035: mirrors edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto.AdminReportResponse
 * field-for-field so Gson can deserialize GET /api/admin/reports directly.
 */
data class AdminReportResponse(
    val totalPatients: Long,
    val totalDoctors: Long,
    val totalAdmins: Long,
    val totalAppointments: Long,
    val appointmentsToday: Long,
    val appointmentsThisWeek: Long,
    val appointmentsByStatus: Map<String, Long>,
    val topDoctorsByAppointments: List<DoctorLoad>,
    val notificationsByStatus: Map<String, Long>,
    val totalNotifications: Long
) {
    data class DoctorLoad(
        val doctorId: Long,
        val doctorName: String,
        val specialization: String?,
        val appointmentCount: Long
    )
}
