package edu.cit.Verona.AppointMed.appointmed_backend.features.patient;

import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.entity.Patient;
import edu.cit.Verona.AppointMed.appointmed_backend.Security.JwtUtil;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto.PatientRegisterRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.auth.dto.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Registration only lives here — login for ALL roles (patient, doctor,
 * admin) goes through the unified AuthController in features.auth, since
 * the login endpoint needs to look at the email domain before it knows
 * which repository to check.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class PatientController {

    private final PatientService patientService;
    private final JwtUtil jwtUtil;

    public PatientController(PatientService patientService, JwtUtil jwtUtil) {
        this.patientService = patientService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody PatientRegisterRequest request) {
        try {
            Patient patient = patientService.registerPatient(request);
            // Log the patient straight in after registering, matching the
            // success screen Register.jsx already shows.
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
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}