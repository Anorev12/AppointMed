package edu.cit.Verona.AppointMed.appointmed_backend.features.notification.entity;

import jakarta.persistence.*;

/**
 * One row per (appointment, offset) reminder that has already fired.
 *
 * Appointment used to carry two fixed booleans (reminder24hSent /
 * reminder1hSent) for this, but those only work for exactly two hardcoded
 * offsets. Now that FR-024 lets an admin configure an arbitrary list of
 * offset hours, ReminderScheduler checks/writes rows here instead — so
 * adding or removing an offset in Settings never causes a re-send or a
 * missed send for appointments already in flight.
 */
@Entity
@Table(name = "reminder_sent_log", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"appointment_id", "offset_hours"})
})
public class ReminderSentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "offset_hours", nullable = false)
    private int offsetHours;

    public ReminderSentLog() {}

    public ReminderSentLog(Long appointmentId, int offsetHours) {
        this.appointmentId = appointmentId;
        this.offsetHours = offsetHours;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public int getOffsetHours() { return offsetHours; }
    public void setOffsetHours(int offsetHours) { this.offsetHours = offsetHours; }
}
