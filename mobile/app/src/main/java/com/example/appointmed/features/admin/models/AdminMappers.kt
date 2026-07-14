package com.example.appointmed.features.admin.models
/** Converts a raw API admin-appointment into the shape AdminAppointmentAdapter expects. */
fun AdminAppointmentApiResponse.toUi(): AdminAppointment = AdminAppointment(
    dbId = id,
    id = reference,
    patient = patientName,
    doctor = doctorName,
    date = date,
    time = time,
    status = status.lowercase() // "CONFIRMED" -> "confirmed", matches the adapter's checks
)

/** Converts a raw API doctor into the shape AdminDoctorAdapter expects. */
fun DoctorResponse.toUi(): AdminDoctor = AdminDoctor(
    id = id,
    name = fullName,
    specialization = specialization,
    status = if (status == "ACTIVE") "active" else "on leave"
)

/** Converts a raw API patient into the shape AdminPatientAdapter expects. */
fun AdminPatientApiResponse.toUi(): AdminPatient = AdminPatient(
    id = id,
    name = fullName,
    email = email,
    contact = contactNumber ?: "—",
    dateOfBirth = dateOfBirth
)

