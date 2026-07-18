package com.example.appointmed.features.patient.models

/** Converts a raw API appointment into the shape AppointmentAdapter expects. */
fun AppointmentApiResponse.toUi(): Appointment = Appointment(
    dbId = id,
    id = reference,
    doctorId = doctorId,
    doctor = doctorName,
    specialization = specialization,
    date = date,
    time = time,
    status = status.lowercase(), // "CONFIRMED" -> "confirmed", matches the adapter's checks
    needsReschedule = needsReschedule
)

/** Converts a raw API doctor into the shape DoctorAdapter expects. */
fun DoctorApiResponse.toUi(): Doctor = Doctor(
    id = id,
    name = fullName,
    specialization = specialization,
    status = status
)