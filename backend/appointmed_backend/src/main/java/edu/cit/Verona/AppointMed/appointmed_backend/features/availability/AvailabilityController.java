package edu.cit.Verona.AppointMed.appointmed_backend.features.availability;

import edu.cit.Verona.AppointMed.appointmed_backend.Security.JwtUtil;
import edu.cit.Verona.AppointMed.appointmed_backend.features.availability.dto.AvailabilityUpdateRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.availability.dto.UnavailableDateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Doctor-only endpoints for managing working days, hours, and leave dates.
 * Auth follows the same manual-header pattern as AuthController#me — there's
 * no Spring Security filter chain, so every endpoint here re-validates the
 * bearer token itself and confirms the role is DOCTOR before touching data.
 */
@RestController
@RequestMapping("/api/doctor/availability")
@CrossOrigin(origins = "http://localhost:3000")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final JwtUtil jwtUtil;

    public AvailabilityController(AvailabilityService availabilityService, JwtUtil jwtUtil) {
        this.availabilityService = availabilityService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<?> getAvailability(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long doctorId = requireDoctor(authHeader);
            return ResponseEntity.ok(availabilityService.getAvailability(doctorId));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<?> updateSchedule(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody AvailabilityUpdateRequest request
    ) {
        try {
            Long doctorId = requireDoctor(authHeader);
            return ResponseEntity.ok(availabilityService.updateSchedule(doctorId, request));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/unavailable-dates")
    public ResponseEntity<?> addUnavailableDate(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UnavailableDateRequest request
    ) {
        try {
            Long doctorId = requireDoctor(authHeader);
            return ResponseEntity.ok(availabilityService.addUnavailableDate(doctorId, request.getDate()));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/unavailable-dates/{date}")
    public ResponseEntity<?> removeUnavailableDate(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String date
    ) {
        try {
            Long doctorId = requireDoctor(authHeader);
            return ResponseEntity.ok(availabilityService.removeUnavailableDate(doctorId, date));
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