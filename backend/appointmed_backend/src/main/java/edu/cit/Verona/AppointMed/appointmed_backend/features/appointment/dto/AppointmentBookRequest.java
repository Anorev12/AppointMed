package edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.dto;

public class AppointmentBookRequest {
    private Long doctorId;
    private String date; // "yyyy-MM-dd"
    private String time; // "HH:mm"

    public AppointmentBookRequest() {}

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
}
