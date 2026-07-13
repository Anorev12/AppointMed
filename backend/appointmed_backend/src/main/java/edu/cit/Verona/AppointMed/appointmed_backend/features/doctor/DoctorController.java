package edu.cit.Verona.AppointMed.appointmed_backend.features.doctor;

import edu.cit.Verona.AppointMed.appointmed_backend.Security.JwtUtil;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.AppointmentService;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.dto.DoctorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only endpoints any logged-in user (patient, in practice) can hit to
 * browse doctors and see their open slots before booking. No role check
 * beyond "has a valid session" — unlike AvailabilityController, which is
 * doctor-only because it edits a specific doctor's own schedule.
 */
@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "http://localhost:3000")
public class DoctorController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final JwtUtil jwtUtil;

    public DoctorController(DoctorService doctorService, AppointmentService appointmentService, JwtUtil jwtUtil) {
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<?> listDoctors(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            requireAuth(authHeader);
            List<DoctorResponse> doctors = doctorService.listAll().stream()
                    .map(d -> new DoctorResponse(d.getId(), DoctorNameFormatter.format(d.getFullName()), d.getEmail(), d.getSpecialization(), d.getStatus()))
                    .toList();
            return ResponseEntity.ok(doctors);
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<?> getSlots(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestParam String date
    ) {
        try {
            requireAuth(authHeader);
            return ResponseEntity.ok(appointmentService.getAvailableSlots(id, date));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Just confirms there's a valid, unexpired session — any role may browse doctors. */
    private void requireAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Missing or malformed Authorization header.");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            throw new SecurityException("Session expired or invalid. Please log in again.");
        }
    }
}
