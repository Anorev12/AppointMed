package edu.cit.Verona.AppointMed.appointmed_backend.features.auth;

import edu.cit.Verona.AppointMed.appointmed_backend.features.auth.dto.AuthResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.auth.dto.LoginRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.entity.Admin;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.entity.Doctor;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.entity.Patient;
import edu.cit.Verona.AppointMed.appointmed_backend.Security.JwtUtil;
import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.AdminService;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.DoctorService;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Single login endpoint for all three roles. It looks at the email
 * domain FIRST, then only checks the matching table — so a doctor address
 * can never accidentally match a patient row, and vice versa.
 *
 * @appointmeddoctor.com -> Doctor table, DOCTOR role
 * @appointmedadmin.com  -> Admin table, ADMIN role
 * anything else         -> Patient table, PATIENT role
 *
 * This controller deliberately depends on all three domain slices
 * (doctor, patient, admin) — login is inherently a cross-cutting concern
 * that needs to know about every role, so it lives in its own auth slice
 * rather than being duplicated three times or bolted onto one role's slice.
 *
 * The /me endpoint is what the frontend calls on every dashboard load to
 * confirm (server-side, via the token signature) that the logged-in user
 * really does hold the role that dashboard requires.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private static final String DOCTOR_DOMAIN = "@appointmeddoctor.com";
    private static final String ADMIN_DOMAIN = "@appointmedadmin.com";

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AdminService adminService;
    private final JwtUtil jwtUtil;

    public AuthController(
            PatientService patientService,
            DoctorService doctorService,
            AdminService adminService,
            JwtUtil jwtUtil
    ) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.adminService = adminService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();

        try {
            if (email.endsWith(DOCTOR_DOMAIN)) {
                Doctor doctor = doctorService.login(email, request.getPassword());
                String token = jwtUtil.generateToken(doctor.getId(), doctor.getEmail(), "DOCTOR");
                return ResponseEntity.ok(new AuthResponse(
                        doctor.getId(), doctor.getFullName(), doctor.getEmail(), "DOCTOR", token
                ));
            }

            if (email.endsWith(ADMIN_DOMAIN)) {
                Admin admin = adminService.login(email, request.getPassword());
                String token = jwtUtil.generateToken(admin.getId(), admin.getEmail(), "ADMIN");
                return ResponseEntity.ok(new AuthResponse(
                        admin.getId(), admin.getFullName(), admin.getEmail(), "ADMIN", token
                ));
            }

            Patient patient = patientService.login(email, request.getPassword());
            String token = jwtUtil.generateToken(patient.getId(), patient.getEmail(), "PATIENT");
            return ResponseEntity.ok(new AuthResponse(
                    patient.getId(),
                    patient.getFullName(),
                    patient.getEmail(),
                    patient.getContactNumber(),
                    patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : null,
                    "PATIENT",
                    token
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    /**
     * Called by the frontend's route guard on every dashboard mount.
     * Header must be: Authorization: Bearer <token>
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or malformed Authorization header.");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(401).body("Session expired or invalid. Please log in again.");
        }

        String role = jwtUtil.extractRole(token);
        String email = jwtUtil.extractEmail(token);
        Long id = jwtUtil.extractId(token);

        // Look up the current name fresh from the DB rather than trusting
        // a name embedded in the token, in case it changed since login.
        try {
            switch (role) {
                case "DOCTOR" -> {
                    var doctor = doctorLookup(email);
                    return ResponseEntity.ok(new AuthResponse(id, doctor, email, role, token));
                }
                case "ADMIN" -> {
                    var admin = adminLookup(email);
                    return ResponseEntity.ok(new AuthResponse(id, admin, email, role, token));
                }
                default -> {
                    var patient = patientLookup(email);
                    return ResponseEntity.ok(new AuthResponse(id, patient, email, role, token));
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Account no longer exists.");
        }
    }

    private String doctorLookup(String email) {
        return doctorService.findFullNameByEmail(email);
    }

    private String adminLookup(String email) {
        return adminService.findFullNameByEmail(email);
    }

    private String patientLookup(String email) {
        return patientService.findFullNameByEmail(email);
    }
}