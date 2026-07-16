package edu.cit.Verona.AppointMed.appointmed_backend.features.notification.repository;

import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.entity.ReminderSentLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderSentLogRepository extends JpaRepository<ReminderSentLog, Long> {
    boolean existsByAppointmentIdAndOffsetHours(Long appointmentId, int offsetHours);
}
