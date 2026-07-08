package edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.dto;

public class DoctorCreateRequest {
    private String fullName;
    private String email;
    private String password;
    private String specialization;

    public DoctorCreateRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
}