package edu.cit.Verona.AppointMed.appointmed_backend.features.notification;

import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.entity.Appointment;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.repository.AppointmentRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * FR-022: "The system shall send automated reminder notifications 24 hours
 * and 1 hour before scheduled appointments."
 *
 * Runs every 5 minutes (see the fixedRate below) and checks every CONFIRMED
 * appointment against two windows. A 15-minute window per check, checked
 * every 5 minutes, guarantees each appointment is caught at least once
 * without needing minute-perfect scheduling. reminder24hSent/reminder1hSent
 * on Appointment prevent duplicate sends across scheduler runs.
 *
 * Requires @EnableScheduling on the main application class.
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private static final long CHECK_INTERVAL_MS = 5 * 60 * 1000; // every 5 minutes
    private static final long WINDOW_MINUTES = 15; // catch window per check

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;

    public ReminderScheduler(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRate = CHECK_INTERVAL_MS)
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        var confirmed = appointmentRepository.findByStatus("CONFIRMED");

        for (Appointment a : confirmed) {
            LocalDateTime start = LocalDateTime.of(a.getDate(), a.getTime());
            long minutesUntil = Duration.between(now, start).toMinutes();

            if (!a.isReminder24hSent() && isDue(minutesUntil, 24 * 60)) {
                sendAndMark(a, "in 24 hours", () -> a.setReminder24hSent(true));
            }
            if (!a.isReminder1hSent() && isDue(minutesUntil, 60)) {
                sendAndMark(a, "in 1 hour", () -> a.setReminder1hSent(true));
            }
        }
    }

    private boolean isDue(long minutesUntil, long targetMinutes) {
        return minutesUntil <= targetMinutes && minutesUntil > targetMinutes - WINDOW_MINUTES;
    }

    private void sendAndMark(Appointment a, String windowLabel, Runnable markSent) {
        patientRepository.findById(a.getPatientId()).ifPresentOrElse(patient -> {
            notificationService.notifyReminder(a, patient.getEmail(), windowLabel);
            markSent.run();
            appointmentRepository.save(a);
        }, () -> log.warn("Skipping reminder for appointment {} — patient {} no longer exists", a.getReference(), a.getPatientId()));
    }
}

