package edu.cit.Verona.AppointMed.appointmed_backend.features.notification.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * FR-024: "The system shall allow administrators to configure notification
 * templates ... ".
 *
 * One row per notification type (BOOKING_CONFIRMATION, CANCELLATION, etc).
 * Deliberately does NOT let an admin edit raw HTML — the branded email
 * layout (templates/email/*.html) stays fixed so every notification keeps a
 * consistent, tested design and nobody can accidentally break the markup
 * or inject something into it. What's actually configurable, which is what
 * a clinic admin realistically wants control over, is:
 *
 *  - subjectTemplate: the email subject line, with {{placeholders}} like
 *    {{reference}}, {{doctorName}}, {{date}}, {{time}} filled in per-send
 *    by TemplateRenderer.
 *  - customMessage: an optional short note inserted into a highlighted box
 *    in the email body (e.g. clinic hours, a COVID notice, a holiday
 *    reminder). Empty by default, meaning no extra box is shown.
 *
 * Seeded with sensible defaults on first startup by
 * NotificationTemplateService — see DEFAULT_SUBJECTS there.
 */
@Entity
@Table(name = "notification_templates")
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * "BOOKING_CONFIRMATION" | "NEW_BOOKING" | "CANCELLATION" | "RESCHEDULE"
     * | "SCHEDULE_CHANGE" | "REMINDER_24H" | "REMINDER_1H"
     */
    @Column(nullable = false, unique = true)
    private String type;

    @Column(nullable = false, length = 300)
    private String subjectTemplate;

    @Column(length = 1000)
    private String customMessage = "";

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public NotificationTemplate() {}

    public NotificationTemplate(String type, String subjectTemplate) {
        this.type = type;
        this.subjectTemplate = subjectTemplate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSubjectTemplate() { return subjectTemplate; }
    public void setSubjectTemplate(String subjectTemplate) { this.subjectTemplate = subjectTemplate; }

    public String getCustomMessage() { return customMessage; }
    public void setCustomMessage(String customMessage) { this.customMessage = customMessage; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
