package edu.cit.Verona.AppointMed.appointmed_backend.features.admin;

import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto.DoctorStatusUpdateRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.AppointmentService;
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
 * This controller intentionally depends on features.doctor.DoctorService and
 * features.appointment.AppointmentService — admin actions on doctor/appointment
 * data are admin actions, not duplicates of those domains' own logic, so the
 * cross-slice calls stay here rather than re-implementing that logic inside
 * the admin slice.
 *
 * Covers: FR-028, FR-030, FR-032, FR-034, FR-035 (overview, user management,
 * reports) and FR-016 (override doctor availability / appointments).
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminController {

    private final DoctorService doctorService;
    private final AdminService adminService;
    private final AppointmentService appointmentService;
    private final JwtUtil jwtUtil;

    public AdminController(DoctorService doctorService, AdminService adminService,
                            AppointmentService appointmentService, JwtUtil jwtUtil) {
        this.doctorService = doctorService;
        this.adminService = adminService;
        this.appointmentService = appointmentService;
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
                    doctor.getId(), doctor.getFullName(), doctor.getEmail(), doctor.getSpecialization(), doctor.getStatus()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** FR-032/FR-034: full doctor roster with active/on-leave status. */
    @GetMapping("/doctors")
    public ResponseEntity<?> listDoctors(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        return ResponseEntity.ok(adminService.listDoctors());
    }

    /** FR-034: mark a doctor active or on leave. */
    @PutMapping("/doctors/{id}/status")
    public ResponseEntity<?> updateDoctorStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody DoctorStatusUpdateRequest request
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        try {
            return ResponseEntity.ok(adminService.updateDoctorStatus(id, request.getStatus()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** FR-028/FR-030: full patient roster. */
    @GetMapping("/patients")
    public ResponseEntity<?> listPatients(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        return ResponseEntity.ok(adminService.listPatients());
    }

    /** FR-030: a single patient's full appointment history, for the "View history" action. */
    @GetMapping("/patients/{id}/appointments")
    public ResponseEntity<?> patientHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        try {
            adminService.requirePatient(id);
            return ResponseEntity.ok(appointmentService.listForPatient(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** FR-035: clinic-wide appointment list, with the same optional search filters as patient history. */
    @GetMapping("/appointments")
    public ResponseEntity<?> listAppointments(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        return ResponseEntity.ok(appointmentService.listAll(status, keyword, from, to));
    }

    /** FR-016: admin override-cancels any appointment, bypassing the patient reschedule cutoff. */
    @PutMapping("/appointments/{id}/cancel")
    public ResponseEntity<?> overrideCancel(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        try {
            return ResponseEntity.ok(appointmentService.cancelByAdmin(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** FR-025: notification log — every confirmation, cancellation, reschedule, and reminder ever attempted. */
    @GetMapping("/notifications")
    public ResponseEntity<?> listNotifications(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        return ResponseEntity.ok(adminService.listNotifications());
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

