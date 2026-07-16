package edu.cit.Verona.AppointMed.appointmed_backend.features.notification.repository;

import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByType(String type);
    boolean existsByType(String type);
}
