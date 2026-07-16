package edu.cit.Verona.AppointMed.appointmed_backend.features.admin;

import edu.cit.Verona.AppointMed.appointmed_backend.Security.JwtUtil;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.NotificationTemplateService;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.ReminderSettingsService;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.dto.NotificationTemplateUpdateRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.dto.ReminderSettingsUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * FR-024: "The system shall allow administrators to configure notification
 * templates and reminder schedules."
 *
 * Split out from AdminController (already large) since this is a distinct
 * settings concern rather than user/appointment management. Same
 * requireAdmin() auth pattern as AdminController — only an authenticated
 * Admin can view or change these.
 */
@RestController
@RequestMapping("/api/admin/settings")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminSettingsController {

    private final NotificationTemplateService templateService;
    private final ReminderSettingsService reminderSettingsService;
    private final JwtUtil jwtUtil;

    public AdminSettingsController(NotificationTemplateService templateService,
                                    ReminderSettingsService reminderSettingsService,
                                    JwtUtil jwtUtil) {
        this.templateService = templateService;
        this.reminderSettingsService = reminderSettingsService;
        this.jwtUtil = jwtUtil;
    }

    // ---------- Notification templates ----------

    @GetMapping("/templates")
    public ResponseEntity<?> listTemplates(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        return ResponseEntity.ok(templateService.listAll());
    }

    @GetMapping("/templates/{type}")
    public ResponseEntity<?> getTemplate(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String type
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        try {
            return ResponseEntity.ok(templateService.get(type));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/templates/{type}")
    public ResponseEntity<?> updateTemplate(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String type,
            @RequestBody NotificationTemplateUpdateRequest request
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        try {
            return ResponseEntity.ok(templateService.update(type, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/templates/{type}/reset")
    public ResponseEntity<?> resetTemplate(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String type
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        try {
            return ResponseEntity.ok(templateService.resetToDefault(type));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- Reminder schedule ----------

    @GetMapping("/reminders")
    public ResponseEntity<?> getReminderSettings(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        return ResponseEntity.ok(reminderSettingsService.get());
    }

    @PutMapping("/reminders")
    public ResponseEntity<?> updateReminderSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ReminderSettingsUpdateRequest request
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        try {
            return ResponseEntity.ok(reminderSettingsService.update(request.getOffsetHours()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Returns null if the caller is a valid admin, or an error response to return immediately. */
    private ResponseEntity<?> requireAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or malformed Authorization header.");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(401).body("Session expired or invalid. Please log in again.");
        }
        if (!"ADMIN".equals(jwtUtil.extractRole(token))) {
            return ResponseEntity.status(403).body("Only an administrator can perform this action.");
        }
        return null;
    }
}
