package edu.cit.Verona.AppointMed.appointmed_backend.features.notification.dto;

import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.entity.Notification;

/** FR-025: what the admin's Notifications log view reads. */
public class NotificationResponse {
    private Long id;
    private String recipientType;
    private String recipientEmail;
    private String type;
    private String subject;
    private String status;
    private int attempts;
    private String createdAt;
    private String sentAt;

    public NotificationResponse() {}

    public NotificationResponse(Notification n) {
        this.id = n.getId();
        this.recipientType = n.getRecipientType();
        this.recipientEmail = n.getRecipientEmail();
        this.type = n.getType();
        this.subject = n.getSubject();
        this.status = n.getStatus();
        this.attempts = n.getAttempts();
        this.createdAt = n.getCreatedAt().toString();
        this.sentAt = n.getSentAt() != null ? n.getSentAt().toString() : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }
}
