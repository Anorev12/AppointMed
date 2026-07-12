package edu.cit.Verona.AppointMed.appointmed_backend.features.notification.repository;

import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByOrderByCreatedAtDesc();
}
