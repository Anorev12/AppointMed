package edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * One row per booking. doctorName/specialization are denormalized at booking
 * time (snapshot of the doctor as they were when booked) so a doctor later
 * changing their specialization doesn't rewrite history on old bookings.
 *
 * status is one of "CONFIRMED" / "CANCELLED" / "COMPLETED" — kept as a plain
 * String to match the rest of this codebase's style (see DoctorAvailability,
 * etc.) rather than introducing a new enum pattern.
 */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private String patientName;

    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false)
    private String doctorName;

    private String specialization;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    @Column(nullable = false)
    private String status = "CONFIRMED";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Appointment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
