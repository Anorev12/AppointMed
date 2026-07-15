package edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto;

/** Admin's view of a patient account — used by the Patients tab of the admin dashboard. */
public class AdminPatientResponse {
    private Long id;
    private String fullName;
    private String email;
    private String contactNumber;
    private String dateOfBirth; // "yyyy-MM-dd", may be null

    public AdminPatientResponse() {}

    public AdminPatientResponse(Long id, String fullName, String email, String contactNumber, String dateOfBirth) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.contactNumber = contactNumber;
        this.dateOfBirth = dateOfBirth;
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
}
