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

    /** FR-022: tracked so the reminder scheduler never sends the same reminder twice. */
    @Column(nullable = false)
    private boolean reminder24hSent = false;

    @Column(nullable = false)
    private boolean reminder1hSent = false;

    /**
     * FR-020: set to true when a doctor marks a date unavailable while this
     * appointment is CONFIRMED on that date. Surfaced in the patient's
     * dashboard so it's obvious the appointment needs attention, and lets
     * them reschedule/cancel past the normal cutoff window — see
     * AppointmentService.requireBeforeCutoff(). Cleared automatically once
     * the patient reschedules or cancels.
     */
    @Column(nullable = false)
    private boolean needsReschedule = false;

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

    public boolean isReminder24hSent() { return reminder24hSent; }
    public void setReminder24hSent(boolean reminder24hSent) { this.reminder24hSent = reminder24hSent; }

    public boolean isReminder1hSent() { return reminder1hSent; }
    public void setReminder1hSent(boolean reminder1hSent) { this.reminder1hSent = reminder1hSent; }

    public boolean isNeedsReschedule() { return needsReschedule; }
    public void setNeedsReschedule(boolean needsReschedule) { this.needsReschedule = needsReschedule; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
