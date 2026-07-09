package edu.cit.Verona.AppointMed.appointmed_backend.features.appointment;

import edu.cit.Verona.AppointMed.appointmed_backend.Security.JwtUtil;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.dto.AppointmentBookRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Patient-only endpoints for booking, viewing, and cancelling their own
 * appointments. Follows the same manual bearer-token pattern as
 * AvailabilityController — every endpoint re-validates the token and role
 * itself since there's no Spring Security filter chain in this app.
 */
@RestController
@RequestMapping("/api/patient/appointments")
@CrossOrigin(origins = "http://localhost:3000")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final JwtUtil jwtUtil;

    public AppointmentController(AppointmentService appointmentService, JwtUtil jwtUtil) {
        this.appointmentService = appointmentService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<?> book(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody AppointmentBookRequest request
    ) {
        try {
            Long patientId = requirePatient(authHeader);
            return ResponseEntity.ok(appointmentService.book(patientId, request));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long patientId = requirePatient(authHeader);
            return ResponseEntity.ok(appointmentService.listForPatient(patientId));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id
    ) {
        try {
            Long patientId = requirePatient(authHeader);
            return ResponseEntity.ok(appointmentService.cancel(patientId, id));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Validates the bearer token and confirms the caller is a patient. Returns their id. */
    private Long requirePatient(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Missing or malformed Authorization header.");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            throw new SecurityException("Session expired or invalid. Please log in again.");
        }
        if (!"PATIENT".equals(jwtUtil.extractRole(token))) {
            throw new SecurityException("Patient access only.");
        }

        return jwtUtil.extractId(token);
    }
}

