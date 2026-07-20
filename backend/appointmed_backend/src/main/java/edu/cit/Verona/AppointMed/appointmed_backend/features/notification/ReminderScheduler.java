package edu.cit.Verona.AppointMed.appointmed_backend.features.notification;

import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.entity.Appointment;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.repository.AppointmentRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.entity.ReminderSentLog;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.repository.ReminderSentLogRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FR-022: "The system shall send automated reminder notifications ...
 * before scheduled appointments." FR-024 makes the offsets (originally a
 * hardcoded "24 hours and 1 hour") admin-configurable via
 * ReminderSettingsService — this class just reads whatever list is
 * currently configured on every run.
 *
 * Runs every 5 minutes (see the fixedRate below) and checks every CONFIRMED
 * appointment against each configured offset. A 15-minute window per check,
 * checked every 5 minutes, guarantees each appointment is caught at least
 * once without needing minute-perfect scheduling.
 *
 * Which offsets have already fired for a given appointment is tracked in
 * ReminderSentLog (one row per appointment+offset) rather than the old
 * fixed reminder24hSent/reminder1hSent booleans on Appointment, since the
 * offset list is no longer fixed at exactly two values.
 *
 * Requires @EnableScheduling on the main application class.
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private static final long CHECK_INTERVAL_MS = 5 * 60 * 1000; // every 5 minutes
    private static final long WINDOW_MINUTES = 15; // catch window per check
    // Render's servers run in UTC, but appointment.date/time are clinic-local (Philippine time).
    // Comparing against LocalDateTime.now() in UTC would fire reminders hours early or late.
    private static final java.time.ZoneId CLINIC_ZONE = java.time.ZoneId.of("Asia/Manila");

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;
    private final ReminderSettingsService reminderSettingsService;
    private final ReminderSentLogRepository reminderSentLogRepository;

    public ReminderScheduler(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              NotificationService notificationService,
                              ReminderSettingsService reminderSettingsService,
                              ReminderSentLogRepository reminderSentLogRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.notificationService = notificationService;
        this.reminderSettingsService = reminderSettingsService;
        this.reminderSentLogRepository = reminderSentLogRepository;
    }

    @Scheduled(fixedRate = CHECK_INTERVAL_MS)
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now(CLINIC_ZONE);
        List<Integer> offsetHours = reminderSettingsService.getOffsetHours(); // FR-024, admin-configurable
        var confirmed = appointmentRepository.findByStatus("CONFIRMED");

        for (Appointment a : confirmed) {
            LocalDateTime start = LocalDateTime.of(a.getDate(), a.getTime());
            long minutesUntil = Duration.between(now, start).toMinutes();

            for (int hours : offsetHours) {
                long targetMinutes = hours * 60L;
                if (isDue(minutesUntil, targetMinutes)
                        && !reminderSentLogRepository.existsByAppointmentIdAndOffsetHours(a.getId(), hours)) {
                    sendAndMark(a, hours);
                }
            }
        }
    }

    private boolean isDue(long minutesUntil, long targetMinutes) {
        return minutesUntil <= targetMinutes && minutesUntil > targetMinutes - WINDOW_MINUTES;
    }

    private void sendAndMark(Appointment a, int offsetHours) {
        patientRepository.findById(a.getPatientId()).ifPresentOrElse(patient -> {
            String windowLabel = offsetHours == 1 ? "in 1 hour" : "in " + offsetHours + " hours";
            notificationService.notifyReminder(a, patient.getEmail(), windowLabel, offsetHours);
            reminderSentLogRepository.save(new ReminderSentLog(a.getId(), offsetHours));
        }, () -> log.warn("Skipping reminder for appointment {} — patient {} no longer exists", a.getReference(), a.getPatientId()));
    }
}
 