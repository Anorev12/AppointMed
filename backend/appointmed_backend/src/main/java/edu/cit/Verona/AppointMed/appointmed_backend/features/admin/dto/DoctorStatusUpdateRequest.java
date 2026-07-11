package edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto;

/** Body for PUT /api/admin/doctors/{id}/status. */
public class DoctorStatusUpdateRequest {
    private String status; // "ACTIVE" | "ON_LEAVE"

    public DoctorStatusUpdateRequest() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}