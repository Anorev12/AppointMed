package edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto;

/** Body for PUT /api/admin/password — an admin can only ever change their own. */
public class PasswordChangeRequest {
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;

    public PasswordChangeRequest() {}

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}

