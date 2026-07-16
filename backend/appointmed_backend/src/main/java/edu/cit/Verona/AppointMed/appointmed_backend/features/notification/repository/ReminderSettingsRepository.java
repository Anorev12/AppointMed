package edu.cit.Verona.AppointMed.appointmed_backend.features.notification.repository;

import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.entity.ReminderSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderSettingsRepository extends JpaRepository<ReminderSettings, Long> {
}
