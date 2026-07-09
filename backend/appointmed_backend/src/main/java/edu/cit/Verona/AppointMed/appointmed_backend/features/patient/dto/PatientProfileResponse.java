package edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto;

public class PatientProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String contactNumber;
    private String dateOfBirth; // "yyyy-MM-dd"
    private String medicalHistory;

    public PatientProfileResponse() {}

    public PatientProfileResponse(Long id, String fullName, String email, String contactNumber,
                                   String dateOfBirth, String medicalHistory) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.contactNumber = contactNumber;
        this.dateOfBirth = dateOfBirth;
        this.medicalHistory = medicalHistory;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }
}
