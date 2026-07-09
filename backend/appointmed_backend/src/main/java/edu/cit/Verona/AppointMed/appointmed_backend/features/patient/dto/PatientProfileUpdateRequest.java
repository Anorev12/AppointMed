package edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto;

public class PatientProfileUpdateRequest {
    private String fullName;
    private String contactNumber;
    private String medicalHistory;

    public PatientProfileUpdateRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }
}
