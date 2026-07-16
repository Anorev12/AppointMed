package edu.cit.Verona.AppointMed.appointmed_backend.features.notification.entity;

import jakarta.persistence.*;

/**
 * FR-024: "... and reminder schedules."
 *
 * Singleton row (id is always 1) holding how many hours before an
 * appointment reminders go out. Stored as a comma-separated list of
 * distinct positive integers, e.g. "24,1" (the SRS default) or "48,24,2".
 * ReminderScheduler reads this instead of the old hardcoded 24h/1h
 * constants, and ReminderSentLog (keyed by appointment + offset) tracks
 * which offsets have already fired per appointment so changing the list
 * later never causes duplicate sends for in-flight appointments.
 */
@Entity
@Table(name = "reminder_settings")
public class ReminderSettings {

    @Id
    private Long id = 1L;

    @Column(nullable = false, length = 100)
    private String offsetHoursCsv = "24,1";

    public ReminderSettings() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOffsetHoursCsv() { return offsetHoursCsv; }
    public void setOffsetHoursCsv(String offsetHoursCsv) { this.offsetHoursCsv = offsetHoursCsv; }
}
