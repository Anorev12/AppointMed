package edu.cit.Verona.AppointMed.appointmed_backend.features.admin;

import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.dto.DoctorCreateRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.dto.DoctorResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.entity.Doctor;
import edu.cit.Verona.AppointMed.appointmed_backend.Security.JwtUtil;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.DoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Only an authenticated Admin can hit these endpoints — enforced by
 * requireAdmin() below, which checks the token's role claim the same way
 * /api/auth/me does. Doctors cannot self-register (business rule), so
 * there is no public /api/doctors/register anywhere in this codebase.
 *
 * This controller intentionally depends on features.doctor.DoctorService —
 * admin-creates-doctor is an admin action on doctor data, not a duplicate
 * of doctor domain logic, so the cross-slice call stays here rather than
 * re-implementing doctor creation inside the admin slice.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminController {

    private final DoctorService doctorService;
    private final JwtUtil jwtUtil;

    public AdminController(DoctorService doctorService, JwtUtil jwtUtil) {
        this.doctorService = doctorService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/doctors")
    public ResponseEntity<?> createDoctor(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody DoctorCreateRequest request
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        try {
            Doctor doctor = doctorService.createDoctor(request);
            return ResponseEntity.ok(new DoctorResponse(
                    doctor.getId(), doctor.getFullName(), doctor.getEmail(), doctor.getSpecialization()
            ));
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