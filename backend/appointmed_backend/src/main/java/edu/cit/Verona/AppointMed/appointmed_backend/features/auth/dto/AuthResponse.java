package edu.cit.Verona.AppointMed.appointmed_backend.features.auth.dto;

/**
 * Returned by /api/auth/login and /api/auth/me.
 * `role` is what the frontend uses to pick which dashboard to show —
 * but the token itself is what actually proves the role server-side,
 * since the frontend can't be trusted to police its own routing.
 */
public class AuthResponse {
    private Long id;
    private String fullName;
    private String email;
    private String role; // "PATIENT" | "DOCTOR" | "ADMIN"
    private String token;

    public AuthResponse() {}

    public AuthResponse(Long id, String fullName, String email, String role, String token) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.token = token;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}