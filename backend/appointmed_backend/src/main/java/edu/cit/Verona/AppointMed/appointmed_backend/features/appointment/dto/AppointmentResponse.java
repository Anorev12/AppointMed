package edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.dto;

public class AppointmentResponse {
    private Long id;
    private String reference;
    private Long doctorId;
    private String doctorName;
    private String specialization;
    private String date; // "yyyy-MM-dd"
    private String time; // "HH:mm"
    private String status; // "CONFIRMED" | "CANCELLED"

    public AppointmentResponse() {}

    public AppointmentResponse(Long id, String reference, Long doctorId, String doctorName,
                                String specialization, String date, String time, String status) {
        this.id = id;
        this.reference = reference;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialization = specialization;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
