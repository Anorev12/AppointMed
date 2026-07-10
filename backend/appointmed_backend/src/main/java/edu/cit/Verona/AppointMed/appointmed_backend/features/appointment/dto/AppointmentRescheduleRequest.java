package edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.dto;

/**
 * Payload for FR-011 (reschedule). A patient can only move an existing
 * booking to a new date/time slot with the *same* doctor — changing doctors
 * is treated as cancel-and-rebook, not a reschedule.
 */
public class AppointmentRescheduleRequest {
    private String date; // "yyyy-MM-dd"
    private String time; // "HH:mm"

    public AppointmentRescheduleRequest() {}

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
}
