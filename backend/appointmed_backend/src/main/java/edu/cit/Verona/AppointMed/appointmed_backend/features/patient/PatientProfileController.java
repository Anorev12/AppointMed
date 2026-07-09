package edu.cit.Verona.AppointMed.appointmed_backend.features.patient;

import edu.cit.Verona.AppointMed.appointmed_backend.Security.JwtUtil;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto.PatientProfileUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Kept separate from PatientController (which only handles registration —
 * see the note there) so that file's single responsibility stays intact.
 * Same manual bearer-token pattern as AvailabilityController.
 */
@RestController
@RequestMapping("/api/patient/profile")
@CrossOrigin(origins = "http://localhost:3000")
public class PatientProfileController {

    private final PatientService patientService;
    private final JwtUtil jwtUtil;

    public PatientProfileController(PatientService patientService, JwtUtil jwtUtil) {
        this.patientService = patientService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long patientId = requirePatient(authHeader);
            return ResponseEntity.ok(patientService.getProfile(patientId));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody PatientProfileUpdateRequest request
    ) {
        try {
            Long patientId = requirePatient(authHeader);
            return ResponseEntity.ok(patientService.updateProfile(patientId, request));
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

