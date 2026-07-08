package edu.cit.Verona.AppointMed.appointmed_backend.features.auth.dto;

/**
 * Returned by /api/auth/login, /api/auth/register, and /api/auth/me.
 * `role` is what the frontend uses to pick which dashboard to show —
 * but the token itself is what actually proves the role server-side,
 * since the frontend can't be trusted to police its own routing.
 *
 * contactNumber and dateOfBirth are only populated for PATIENT accounts
 * (doctors/admins don't have these fields) — they'll be null otherwise.
 */
public class AuthResponse {
    private Long id;
    private String fullName;
    private String email;
    private String contactNumber;
    private String dateOfBirth;
    private String role; // "PATIENT" | "DOCTOR" | "ADMIN"
    private String token;

    public AuthResponse() {}

    // Full constructor — used for PATIENT, which has contact/dob
    public AuthResponse(Long id, String fullName, String email, String contactNumber,
                         String dateOfBirth, String role, String token) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.contactNumber = contactNumber;
        this.dateOfBirth = dateOfBirth;
        this.role = role;
        this.token = token;
    }

    // Convenience constructor — used for DOCTOR/ADMIN, which don't have contact/dob
    public AuthResponse(Long id, String fullName, String email, String role, String token) {
        this(id, fullName, email, null, null, role, token);
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

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}