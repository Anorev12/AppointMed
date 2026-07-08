package edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.dto;

public class DoctorResponse {
    private Long id;
    private String fullName;
    private String email;
    private String specialization;

    public DoctorResponse() {}

    public DoctorResponse(Long id, String fullName, String email, String specialization) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.specialization = specialization;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
}