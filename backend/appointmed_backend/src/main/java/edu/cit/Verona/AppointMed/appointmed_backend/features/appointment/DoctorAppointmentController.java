package edu.cit.Verona.AppointMed.appointmed_backend.features.appointment;

import edu.cit.Verona.AppointMed.appointmed_backend.Security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Doctor-only view of the appointments booked with them, plus the ability
 * to cancel or close one out. Booking itself is patient-initiated (see
 * AppointmentController) — this controller only ever reads or transitions
 * appointments that already exist.
 */
@RestController
@RequestMapping("/api/doctor/appointments")
@CrossOrigin(origins = "http://localhost:3000")
public class DoctorAppointmentController {

    private final AppointmentService appointmentService;
    private final JwtUtil jwtUtil;

    public DoctorAppointmentController(AppointmentService appointmentService, JwtUtil jwtUtil) {
        this.appointmentService = appointmentService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long doctorId = requireDoctor(authHeader);
            return ResponseEntity.ok(appointmentService.listForDoctor(doctorId));
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
            Long doctorId = requireDoctor(authHeader);
            return ResponseEntity.ok(appointmentService.cancelByDoctor(doctorId, id));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> complete(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id
    ) {
        try {
            Long doctorId = requireDoctor(authHeader);
            return ResponseEntity.ok(appointmentService.completeByDoctor(doctorId, id));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Validates the bearer token and confirms the caller is a doctor. Returns their id. */
    private Long requireDoctor(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Missing or malformed Authorization header.");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            throw new SecurityException("Session expired or invalid. Please log in again.");
        }
        if (!"DOCTOR".equals(jwtUtil.extractRole(token))) {
            throw new SecurityException("Doctor access only.");
        }

        return jwtUtil.extractId(token);
    }
}
