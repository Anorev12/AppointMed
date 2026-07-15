package edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto;

/** Body for POST /api/admin/patients — an admin creating a patient account directly (not self-register). */
public class PatientCreateRequest {
    private String fullName;
    private String email;
    private String password;
    private String contactNumber;
    private String dateOfBirth; // "yyyy-MM-dd", optional

    public PatientCreateRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
}