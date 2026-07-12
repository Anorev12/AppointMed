package edu.cit.Verona.AppointMed.appointmed_backend.features.notification;

import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.entity.Appointment;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.entity.Notification;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final NotificationRepository notificationRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.from-email:no-reply@appointmed.local}")
    private String fromEmail;

    private static final int MAX_ATTEMPTS = 3; // FR-026

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notifyBookingConfirmation(Appointment a, String patientEmail) {
        String subject = "Appointment confirmed — " + a.getReference();
        String body = "Hi " + a.getPatientName() + ",\n\n"
                + "Your appointment with " + a.getDoctorName() + " is confirmed for "
                + a.getDate().format(DATE_FMT) + " at " + a.getTime() + ".\n"
                + "Reference number: " + a.getReference() + "\n\n"
                + "— AppointMed";
        send("PATIENT", a.getPatientId(), patientEmail, "BOOKING_CONFIRMATION", subject, body);
    }

    public void notifyNewBookingToDoctor(Appointment a, String doctorEmail) {
        String subject = "New appointment booked — " + a.getReference();
        String body = "You have a new appointment with " + a.getPatientName() + " on "
                + a.getDate().format(DATE_FMT) + " at " + a.getTime() + ".\n"
                + "Reference number: " + a.getReference() + "\n\n"
                + "— AppointMed";
        send("DOCTOR", a.getDoctorId(), doctorEmail, "NEW_BOOKING", subject, body);
    }

    public void notifyCancellation(Appointment a, String patientEmail, String cancelledBy) {
        String subject = "Appointment cancelled — " + a.getReference();
        String body = "Hi " + a.getPatientName() + ",\n\n"
                + "Your appointment with " + a.getDoctorName() + " on " + a.getDate().format(DATE_FMT)
                + " at " + a.getTime() + " has been cancelled by " + cancelledBy + ".\n"
                + "Reference number: " + a.getReference() + "\n\n"
                + "If this wasn't expected, please contact the clinic directly.\n\n"
                + "— AppointMed";
        send("PATIENT", a.getPatientId(), patientEmail, "CANCELLATION", subject, body);
    }

    public void notifyReschedule(Appointment a, String patientEmail, String oldDate, String oldTime) {
        String subject = "Appointment rescheduled — " + a.getReference();
        String body = "Hi " + a.getPatientName() + ",\n\n"
                + "Your appointment with " + a.getDoctorName() + " has been moved from "
                + oldDate + " " + oldTime + " to " + a.getDate().format(DATE_FMT) + " at " + a.getTime() + ".\n"
                + "Reference number: " + a.getReference() + "\n\n"
                + "— AppointMed";
        send("PATIENT", a.getPatientId(), patientEmail, "RESCHEDULE", subject, body);
    }

    public void notifyScheduleChange(Appointment a, String patientEmail) {
        String subject = "Schedule change affecting your appointment — " + a.getReference();
        String body = "Hi " + a.getPatientName() + ",\n\n"
                + "Dr. " + a.getDoctorName() + " has become unavailable on " + a.getDate().format(DATE_FMT)
                + ", which is the date of your appointment at " + a.getTime() + " (Reference: " + a.getReference() + ").\n"
                + "The clinic will contact you shortly to reschedule. We apologize for the inconvenience.\n\n"
                + "— AppointMed";
        send("PATIENT", a.getPatientId(), patientEmail, "SCHEDULE_CHANGE", subject, body);
    }

    public void notifyReminder(Appointment a, String patientEmail, String windowLabel) {
        String subject = "Reminder: appointment " + windowLabel + " — " + a.getReference();
        String body = "Hi " + a.getPatientName() + ",\n\n"
                + "This is a reminder that you have an appointment with " + a.getDoctorName() + " "
                + windowLabel + ", on " + a.getDate().format(DATE_FMT) + " at " + a.getTime() + ".\n"
                + "Reference number: " + a.getReference() + "\n\n"
                + "— AppointMed";
        String type = "in 24 hours".equals(windowLabel) ? "REMINDER_24H" : "REMINDER_1H";
        send("PATIENT", a.getPatientId(), patientEmail, type, subject, body);
    }

    private void send(String recipientType, Long recipientId, String recipientEmail,
                       String type, String subject, String body) {
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
        n.setBody(body);

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
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(recipientEmail);
                message.setSubject(subject);
                message.setText(body);
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