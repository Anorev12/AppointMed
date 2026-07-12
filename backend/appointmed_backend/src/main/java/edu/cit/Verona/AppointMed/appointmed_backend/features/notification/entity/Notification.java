package edu.cit.Verona.AppointMed.appointmed_backend.features.notification.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One row per notification attempt — satisfies FR-025 ("maintain a log of
 * all notifications, including recipient, delivery time, notification type,
 * and delivery status"). Written by NotificationService for every
 * confirmation/cancellation/reschedule/reminder, whether or not real email
 * sending is enabled (see NotificationService's emailEnabled flag).
 *
 * status is one of "SENT" | "UNDELIVERED" | "LOGGED":
 *  - SENT: actually delivered via JavaMailSender
 *  - UNDELIVERED: real sending was attempted 3 times (FR-026) and all failed
 *  - LOGGED: email sending is disabled (notification.email.enabled=false),
 *    so this row exists as a record of what *would* have been sent
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientType; // "PATIENT" | "DOCTOR"

    @Column(nullable = false)
    private Long recipientId;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String type; // "BOOKING_CONFIRMATION" | "CANCELLATION" | "RESCHEDULE" | "NEW_BOOKING" | "SCHEDULE_CHANGE" | "REMINDER_24H" | "REMINDER_1H"

    @Column(nullable = false)
    private String subject;

    @Column(length = 2000)
    private String body;

    @Column(nullable = false)
    private String status = "PENDING"; // "PENDING" | "SENT" | "UNDELIVERED" | "LOGGED"

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime sentAt;

    public Notification() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}

