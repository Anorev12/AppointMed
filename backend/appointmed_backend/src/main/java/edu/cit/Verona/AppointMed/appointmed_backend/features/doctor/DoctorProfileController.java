package edu.cit.Verona.AppointMed.appointmed_backend.features.doctor;

import edu.cit.Verona.AppointMed.appointmed_backend.Security.JwtUtil;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.dto.PasswordChangeRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Doctor's own account actions — kept separate from DoctorController (which
 * is the patient-facing browse/booking surface) the same way
 * PatientProfileController is kept separate from PatientController.
 * Same manual bearer-token pattern as the rest of this codebase.
 */
@RestController
@RequestMapping("/api/doctor/profile")
@CrossOrigin(origins = "http://localhost:3000")
public class DoctorProfileController {

    private final DoctorService doctorService;
    private final JwtUtil jwtUtil;

    public DoctorProfileController(DoctorService doctorService, JwtUtil jwtUtil) {
        this.doctorService = doctorService;
        this.jwtUtil = jwtUtil;
    }

    /** Doctor's own full account details — id, name, email, specialization, status. */
    @GetMapping
    public ResponseEntity<?> getProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long doctorId = requireDoctor(authHeader);
            return ResponseEntity.ok(doctorService.getProfile(doctorId));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Doctor changes their own password — requires the current password and a confirmed new one. */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody PasswordChangeRequest request
    ) {
        try {
            Long doctorId = requireDoctor(authHeader);
            doctorService.changePassword(doctorId, request);
            return ResponseEntity.ok("Password updated successfully.");
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
 