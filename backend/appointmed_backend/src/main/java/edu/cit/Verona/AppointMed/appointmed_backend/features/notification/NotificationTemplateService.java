package edu.cit.Verona.AppointMed.appointmed_backend.features.notification;

import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.dto.NotificationTemplateResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.dto.NotificationTemplateUpdateRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.entity.NotificationTemplate;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.repository.NotificationTemplateRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FR-024 (templates half). Every notification type NotificationService
 * sends has exactly one row here, seeded with the same subject lines that
 * used to be hardcoded (see DEFINITIONS below) so behavior is unchanged
 * until an admin actually edits something.
 */
@Service
public class NotificationTemplateService {

    /** type -> { display label, default subject, placeholder hint shown in the admin UI } */
    private static final Map<String, String[]> DEFINITIONS = new LinkedHashMap<>();
    static {
        DEFINITIONS.put("BOOKING_CONFIRMATION", new String[]{
                "Booking confirmation (to patient)",
                "Appointment confirmed — {{reference}}",
                "{{patientName}}, {{doctorName}}, {{date}}, {{time}}, {{reference}}"
        });
        DEFINITIONS.put("NEW_BOOKING", new String[]{
                "New booking alert (to doctor)",
                "New appointment booked — {{reference}}",
                "{{patientName}}, {{date}}, {{time}}, {{reference}}"
        });
        DEFINITIONS.put("CANCELLATION", new String[]{
                "Cancellation (to patient)",
                "Appointment cancelled — {{reference}}",
                "{{patientName}}, {{doctorName}}, {{date}}, {{time}}, {{reference}}, {{cancelledBy}}"
        });
        DEFINITIONS.put("RESCHEDULE", new String[]{
                "Reschedule confirmation (to patient)",
                "Appointment rescheduled — {{reference}}",
                "{{patientName}}, {{doctorName}}, {{reference}}, {{oldDate}}, {{oldTime}}, {{newDate}}, {{newTime}}"
        });
        DEFINITIONS.put("SCHEDULE_CHANGE", new String[]{
                "Doctor became unavailable (to patient)",
                "Action needed: reschedule your appointment — {{reference}}",
                "{{patientName}}, {{doctorName}}, {{date}}, {{time}}, {{reference}}"
        });
        DEFINITIONS.put("REMINDER_24H", new String[]{
                "24-hour reminder",
                "Reminder: appointment in 24 hours — {{reference}}",
                "{{patientName}}, {{doctorName}}, {{date}}, {{time}}, {{reference}}"
        });
        DEFINITIONS.put("REMINDER_1H", new String[]{
                "1-hour reminder",
                "Reminder: appointment in 1 hour — {{reference}}",
                "{{patientName}}, {{doctorName}}, {{date}}, {{time}}, {{reference}}"
        });
    }

    private final NotificationTemplateRepository repository;
    private final TemplateRenderer renderer;

    public NotificationTemplateService(NotificationTemplateRepository repository, TemplateRenderer renderer) {
        this.repository = repository;
        this.renderer = renderer;
    }

    /** Seeds one row per known notification type on startup — safe to run every boot, only inserts what's missing. */
    @PostConstruct
    public void seedDefaults() {
        DEFINITIONS.forEach((type, meta) -> {
            if (!repository.existsByType(type)) {
                repository.save(new NotificationTemplate(type, meta[1]));
            }
        });
    }

    public List<NotificationTemplateResponse> listAll() {
        return DEFINITIONS.keySet().stream().map(this::toResponse).toList();
    }

    public NotificationTemplateResponse get(String type) {
        requireKnownType(type);
        return toResponse(type);
    }

    @Transactional
    public NotificationTemplateResponse update(String type, NotificationTemplateUpdateRequest request) {
        requireKnownType(type);

        String subject = request.getSubjectTemplate() == null ? "" : request.getSubjectTemplate().trim();
        if (subject.isBlank()) {
            throw new IllegalArgumentException("Subject line can't be empty.");
        }
        if (subject.length() > 300) {
            throw new IllegalArgumentException("Subject line is too long (300 characters max).");
        }

        String customMessage = request.getCustomMessage() == null ? "" : request.getCustomMessage().trim();
        if (customMessage.length() > 1000) {
            throw new IllegalArgumentException("Custom message is too long (1000 characters max).");
        }

        NotificationTemplate template = repository.findByType(type)
                .orElseGet(() -> new NotificationTemplate(type, DEFINITIONS.get(type)[1]));
        template.setSubjectTemplate(subject);
        template.setCustomMessage(customMessage);
        template.setUpdatedAt(LocalDateTime.now());
        repository.save(template);

        return toResponse(type);
    }

    @Transactional
    public NotificationTemplateResponse resetToDefault(String type) {
        requireKnownType(type);
        NotificationTemplate template = repository.findByType(type)
                .orElseGet(() -> new NotificationTemplate(type, DEFINITIONS.get(type)[1]));
        template.setSubjectTemplate(DEFINITIONS.get(type)[1]);
        template.setCustomMessage("");
        template.setUpdatedAt(LocalDateTime.now());
        repository.save(template);
        return toResponse(type);
    }

    // ---------- Used by NotificationService when actually sending ----------

    /** Renders the configured subject line for this type, merging in the given values. */
    public String renderSubject(String type, Map<String, String> vars) {
        String template = repository.findByType(type)
                .map(NotificationTemplate::getSubjectTemplate)
                .orElse(DEFINITIONS.getOrDefault(type, new String[]{null, ""})[1]);
        return renderer.render(template, vars);
    }

    /** The admin's optional custom note for this type, or "" if none is set — passed into the email template as ${customMessage}. */
    public String getCustomMessage(String type) {
        return repository.findByType(type).map(NotificationTemplate::getCustomMessage).orElse("");
    }

    // ---------- Helpers ----------

    private void requireKnownType(String type) {
        if (!DEFINITIONS.containsKey(type)) {
            throw new IllegalArgumentException("Unknown notification template type: " + type);
        }
    }

    private NotificationTemplateResponse toResponse(String type) {
        String[] meta = DEFINITIONS.get(type);
        NotificationTemplate saved = repository.findByType(type).orElse(null);
        String subject = saved != null ? saved.getSubjectTemplate() : meta[1];
        String customMessage = saved != null ? saved.getCustomMessage() : "";
        return new NotificationTemplateResponse(type, meta[0], subject, customMessage, meta[2]);
    }
}
