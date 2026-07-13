package edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto;

/** What the admin roster (GET /api/admin/admins) returns — deliberately has no delete action anywhere. */
public class AdminResponse {
    private Long id;
    private String fullName;
    private String email;

    public AdminResponse() {}

    public AdminResponse(Long id, String fullName, String email) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
