package edu.cit.Verona.AppointMed.appointmed_backend.features.notification;

import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.entity.Appointment;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.DoctorNameFormatter;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.entity.Notification;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.repository.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * FR-013, FR-020, FR-021, FR-022, FR-023, FR-025, FR-026, FR-027.
 *
 * Every notification — sent or not — gets logged as a Notification row
 * (FR-025). Real email delivery is gated behind notification.email.enabled
 * (application.properties), off by default: with it off, this still writes
 * a "LOGGED" row for every event so the feature is fully demonstrable
 * without needing real SMTP credentials configured. Flip it on and it
 * sends through whatever spring.mail.* JavaMailSender is configured.
 *
 * Every notification type is now sent as a styled HTML email, rendered
 * via the TemplateEngine bean spring-boot-starter-thymeleaf already
 * auto-configures (same dependency used for the web view layer, just
 * pointed at templates under templates/email/). Each has a matching
 * renderXxxHtml() method below that builds the Context and calls
 * templateEngine.process(...) — follow that same pattern for any new
 * notification type added later.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final NotificationRepository notificationRepository;
    private final TemplateEngine templateEngine;
    private final NotificationTemplateService templateService;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.from-email:no-reply@appointmed.local}")
    private String fromEmail;

    private static final int MAX_ATTEMPTS = 3; // FR-026

    public NotificationService(NotificationRepository notificationRepository, TemplateEngine templateEngine,
                                NotificationTemplateService templateService) {
        this.notificationRepository = notificationRepository;
        this.templateEngine = templateEngine;
        this.templateService = templateService;
    }

    /** FR-013 / FR-021: sent to the patient the moment a booking is confirmed — the one HTML-styled email so far. */
    public void notifyBookingConfirmation(Appointment a, String patientEmail) {
        String doctorName = DoctorNameFormatter.format(a.getDoctorName());
        String date = a.getDate().format(DATE_FMT);
        String time = NotificationTimeFormatter.format(a.getTime());

        Map<String, String> vars = new HashMap<>();
        vars.put("patientName", a.getPatientName());
        vars.put("doctorName", doctorName);
        vars.put("date", date);
        vars.put("time", time);
        vars.put("reference", a.getReference());
        String subject = templateService.renderSubject("BOOKING_CONFIRMATION", vars); // FR-024

        // Plain-text fallback for the notification log and for mail clients that can't render HTML.
        String plainBody = "Hi " + a.getPatientName() + ",\n\n"
                + "Your appointment with " + doctorName + " is confirmed for " + date + " at " + time + ".\n"
                + "Reference number: " + a.getReference() + "\n\n"
                + "— AppointMed";

        String htmlBody = renderBookingConfirmationHtml(a, doctorName, date, time);

        send("PATIENT", a.getPatientId(), patientEmail, "BOOKING_CONFIRMATION", subject, plainBody, htmlBody);
    }

    private String renderBookingConfirmationHtml(Appointment a, String doctorName, String date, String time) {
        Context context = new Context();
        context.setVariable("patientName", a.getPatientName());
        context.setVariable("doctorName", doctorName);
        context.setVariable("date", date);
        context.setVariable("time", time);
        context.setVariable("reference", a.getReference());
        context.setVariable("customMessage", templateService.getCustomMessage("BOOKING_CONFIRMATION")); // FR-024
        return templateEngine.process("email/booking-confirmation", context);
    }

    /** FR-027: sent to the doctor when a patient books with them. */
    /** FR-027: sent to the doctor when a patient books with them. */
    public void notifyNewBookingToDoctor(Appointment a, String doctorEmail) {
        String date = a.getDate().format(DATE_FMT);
        String time = NotificationTimeFormatter.format(a.getTime());

        Map<String, String> vars = new HashMap<>();
        vars.put("patientName", a.getPatientName());
        vars.put("date", date);
        vars.put("time", time);
        vars.put("reference", a.getReference());
        String subject = templateService.renderSubject("NEW_BOOKING", vars); // FR-024

        String plainBody = "You have a new appointment with " + a.getPatientName() + " on "
                + date + " at " + time + ".\n"
                + "Reference number: " + a.getReference() + "\n\n"
                + "— AppointMed";

        String htmlBody = renderNewBookingDoctorHtml(a, date, time);

        send("DOCTOR", a.getDoctorId(), doctorEmail, "NEW_BOOKING", subject, plainBody, htmlBody);
    }

    private String renderNewBookingDoctorHtml(Appointment a, String date, String time) {
        Context context = new Context();
        context.setVariable("patientName", a.getPatientName());
        context.setVariable("date", date);
        context.setVariable("time", time);
        context.setVariable("reference", a.getReference());
        context.setVariable("customMessage", templateService.getCustomMessage("NEW_BOOKING")); // FR-024
        return templateEngine.process("email/new-booking-doctor", context);
    }

    /** FR-023: sent to the patient when their appointment is cancelled, by whoever cancelled it. */
    public void notifyCancellation(Appointment a, String patientEmail, String cancelledBy) {
        String doctorName = DoctorNameFormatter.format(a.getDoctorName());
        String date = a.getDate().format(DATE_FMT);
        String time = NotificationTimeFormatter.format(a.getTime());

        Map<String, String> vars = new HashMap<>();
        vars.put("patientName", a.getPatientName());
        vars.put("doctorName", doctorName);
        vars.put("date", date);
        vars.put("time", time);
        vars.put("reference", a.getReference());
        vars.put("cancelledBy", cancelledBy);
        String subject = templateService.renderSubject("CANCELLATION", vars); // FR-024

        String plainBody = "Hi " + a.getPatientName() + ",\n\n"
                + "Your appointment with " + doctorName + " on " + date
                + " at " + time + " has been cancelled by " + cancelledBy + ".\n"
                + "Reference number: " + a.getReference() + "\n\n"
                + "If this wasn't expected, please contact the clinic directly.\n\n"
                + "— AppointMed";

        String htmlBody = renderCancellationHtml(a, doctorName, date, time, cancelledBy);

        send("PATIENT", a.getPatientId(), patientEmail, "CANCELLATION", subject, plainBody, htmlBody);
    }

    private String renderCancellationHtml(Appointment a, String doctorName, String date, String time, String cancelledBy) {
        Context context = new Context();
        context.setVariable("patientName", a.getPatientName());
        context.setVariable("doctorName", doctorName);
        context.setVariable("date", date);
        context.setVariable("time", time);
        context.setVariable("reference", a.getReference());
        context.setVariable("cancelledBy", cancelledBy);
        context.setVariable("customMessage", templateService.getCustomMessage("CANCELLATION")); // FR-024
        return templateEngine.process("email/appointment-cancelled", context);
    }

    /** FR-023: sent to the patient when their appointment is moved to a new date/time. */
    public void notifyReschedule(Appointment a, String patientEmail, String oldDate, String oldTime) {
        String doctorName = DoctorNameFormatter.format(a.getDoctorName());
        String newDate = a.getDate().format(DATE_FMT);
        String newTime = NotificationTimeFormatter.format(a.getTime());

        Map<String, String> vars = new HashMap<>();
        vars.put("patientName", a.getPatientName());
        vars.put("doctorName", doctorName);
        vars.put("oldDate", oldDate);
        vars.put("oldTime", oldTime);
        vars.put("newDate", newDate);
        vars.put("newTime", newTime);
        vars.put("reference", a.getReference());
        String subject = templateService.renderSubject("RESCHEDULE", vars); // FR-024

        String plainBody = "Hi " + a.getPatientName() + ",\n\n"
                + "Your appointment with " + doctorName + " has been moved from "
                + oldDate + " " + oldTime + " to " + newDate + " at " + newTime + ".\n"
                + "Reference number: " + a.getReference() + "\n\n"
                + "— AppointMed";

        String htmlBody = renderRescheduleHtml(a, doctorName, oldDate, oldTime, newDate, newTime);

        send("PATIENT", a.getPatientId(), patientEmail, "RESCHEDULE", subject, plainBody, htmlBody);
    }

    private String renderRescheduleHtml(Appointment a, String doctorName, String oldDate, String oldTime, String newDate, String newTime) {
        Context context = new Context();
        context.setVariable("patientName", a.getPatientName());
        context.setVariable("doctorName", doctorName);
        context.setVariable("oldDate", oldDate);
        context.setVariable("oldTime", oldTime);
        context.setVariable("newDate", newDate);
        context.setVariable("newTime", newTime);
        context.setVariable("reference", a.getReference());
        context.setVariable("customMessage", templateService.getCustomMessage("RESCHEDULE")); // FR-024
        return templateEngine.process("email/appointment-rescheduled", context);
    }

    /**
     * FR-020: sent to a patient whose confirmed appointment falls on a date
     * a doctor just marked unavailable. Informational only — this does not
     * auto-cancel the appointment; the clinic still needs to follow up.
     */
    public void notifyScheduleChange(Appointment a, String patientEmail) {
        String doctorName = DoctorNameFormatter.format(a.getDoctorName());
        String date = a.getDate().format(DATE_FMT);
        String time = NotificationTimeFormatter.format(a.getTime());

        Map<String, String> vars = new HashMap<>();
        vars.put("patientName", a.getPatientName());
        vars.put("doctorName", doctorName);
        vars.put("date", date);
        vars.put("time", time);
        vars.put("reference", a.getReference());
        String subject = templateService.renderSubject("SCHEDULE_CHANGE", vars); // FR-024

        String plainBody = "Hi " + a.getPatientName() + ",\n\n"
                + doctorName + " has become unavailable on " + date
                + ", which is the date of your appointment at " + time + " (Reference: " + a.getReference() + ").\n"
                + "Please open the AppointMed app to pick a new date and time that works for you — "
                + "you can reschedule or cancel this appointment any time, with no cutoff restriction, "
                + "since this change wasn't something you asked for. We apologize for the inconvenience.\n\n"
                + "— AppointMed";

        String htmlBody = renderScheduleChangeHtml(a, doctorName, date, time);

        send("PATIENT", a.getPatientId(), patientEmail, "SCHEDULE_CHANGE", subject, plainBody, htmlBody);
    }

    private String renderScheduleChangeHtml(Appointment a, String doctorName, String date, String time) {
        Context context = new Context();
        context.setVariable("patientName", a.getPatientName());
        context.setVariable("doctorName", doctorName);
        context.setVariable("date", date);
        context.setVariable("time", time);
        context.setVariable("reference", a.getReference());
        context.setVariable("customMessage", templateService.getCustomMessage("SCHEDULE_CHANGE")); // FR-024
        return templateEngine.process("email/schedule-change", context);
    }

    /** FR-022: 24-hour or 1-hour reminder, called by ReminderScheduler. Both now have HTML templates — 1h is styled more urgently than 24h. */
    public void notifyReminder(Appointment a, String patientEmail, String windowLabel, int offsetHours) {
        String doctorName = DoctorNameFormatter.format(a.getDoctorName());
        String date = a.getDate().format(DATE_FMT);
        String time = NotificationTimeFormatter.format(a.getTime());

        // Only the 1-hour offset gets the "urgent" styled template — any other
        // admin-configured offset (24h default, or something custom like 48h)
        // uses the calmer advance-notice design. FR-024 lets the offsets be
        // arbitrary, but authoring a bespoke branded HTML template per possible
        // value isn't practical, so offsets are grouped into these two designs.
        boolean isOneHour = offsetHours == 1;
        String type = isOneHour ? "REMINDER_1H" : "REMINDER_24H";

        Map<String, String> vars = new HashMap<>();
        vars.put("patientName", a.getPatientName());
        vars.put("doctorName", doctorName);
        vars.put("date", date);
        vars.put("time", time);
        vars.put("reference", a.getReference());
        String subject = templateService.renderSubject(type, vars); // FR-024

        String plainBody = "Hi " + a.getPatientName() + ",\n\n"
                + "This is a reminder that you have an appointment with " + doctorName + " "
                + windowLabel + ", on " + date + " at " + time + ".\n"
                + "Reference number: " + a.getReference() + "\n\n"
                + "— AppointMed";

        String templateName = isOneHour ? "email/appointment-reminder-1h" : "email/appointment-reminder-24h";
        String htmlBody = renderReminderHtml(templateName, type, a, doctorName, date, time);

        send("PATIENT", a.getPatientId(), patientEmail, type, subject, plainBody, htmlBody);
    }

    private String renderReminderHtml(String templateName, String type, Appointment a, String doctorName, String date, String time) {
        Context context = new Context();
        context.setVariable("patientName", a.getPatientName());
        context.setVariable("doctorName", doctorName);
        context.setVariable("date", date);
        context.setVariable("time", time);
        context.setVariable("reference", a.getReference());
        context.setVariable("customMessage", templateService.getCustomMessage(type)); // FR-024
        return templateEngine.process(templateName, context);
    }

    // ---------- Core send + log + retry ----------

    /** Plain-text-only overload — used by every notification type that doesn't have an HTML template yet. */
    private void send(String recipientType, Long recipientId, String recipientEmail,
                       String type, String subject, String body) {
        send(recipientType, recipientId, recipientEmail, type, subject, body, null);
    }

    /**
     * @param htmlBody optional — when present, the email is sent as HTML with
     *                 plainBody as the fallback for clients that can't render
     *                 HTML (multipart/alternative). When null, plain text only.
     */
    private void send(String recipientType, Long recipientId, String recipientEmail,
                       String type, String subject, String plainBody, String htmlBody) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Skipping notification type={} — recipient {} #{} has no email on file",
                    type, recipientType, recipientId);
            return;
        }

        Notification n = new Notification();
        n.setRecipientType(recipientType);
        n.setRecipientId(recipientId);
        n.setRecipientEmail(recipientEmail);
        n.setType(type);
        n.setSubject(subject);
        n.setBody(plainBody); // the log always stores the plain-text version — short and readable in the admin table

        if (!emailEnabled || mailSender == null) {
            n.setStatus("LOGGED");
            notificationRepository.save(n);
            log.info("[notification-logged] to={} type={} subject=\"{}\"", recipientEmail, type, subject);
            return;
        }

        int attempts = 0;
        boolean sent = false;
        Exception lastError = null;

        while (attempts < MAX_ATTEMPTS && !sent) {
            attempts++;
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, htmlBody != null, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(recipientEmail);
                helper.setSubject(subject);

                if (htmlBody != null) {
                    helper.setText(plainBody, htmlBody); // multipart/alternative: HTML + plain-text fallback
                } else {
                    helper.setText(plainBody);
                }

                mailSender.send(message);
                sent = true;
            } catch (Exception e) {
                lastError = e;
                log.warn("Notification send attempt {}/{} failed for {}: {}", attempts, MAX_ATTEMPTS, recipientEmail, e.getMessage());
            }
        }

        n.setAttempts(attempts);
        if (sent) {
            n.setStatus("SENT");
            n.setSentAt(java.time.LocalDateTime.now());
        } else {
            n.setStatus("UNDELIVERED");
            log.error("Notification permanently undelivered to {} after {} attempts: {}",
                    recipientEmail, attempts, lastError != null ? lastError.getMessage() : "unknown error");
        }
        notificationRepository.save(n);
    }
}
