package com.example.appointmed.features.doctor.models


/** Converts a raw API doctor-appointment into the shape ScheduleAdapter expects. */
fun DoctorAppointmentApiResponse.toUi(): DoctorAppointment = DoctorAppointment(
    dbId = id,
    id = reference,
    patient = patientName,
    date = date,
    time = time,
    status = status.lowercase() // "CONFIRMED" -> "confirmed", matches the adapter's checks
)
