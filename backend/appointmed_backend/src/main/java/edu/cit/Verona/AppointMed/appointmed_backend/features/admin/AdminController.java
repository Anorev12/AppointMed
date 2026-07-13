package edu.cit.Verona.AppointMed.appointmed_backend.features.admin;

import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto.AdminCreateRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto.DoctorStatusUpdateRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto.PasswordChangeRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.AppointmentService;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.DoctorNameFormatter;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.dto.DoctorCreateRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.dto.DoctorResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.entity.Doctor;
import edu.cit.Verona.AppointMed.appointmed_backend.Security.JwtUtil;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.DoctorService;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto.PatientCreateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Only an authenticated Admin can hit these endpoints — enforced by
 * requireAdmin() below, which checks the token's role claim the same way
 * /api/auth/me does. Doctors cannot self-register (business rule), so
 * there is no public /api/doctors/register anywhere in this codebase.
 * Admins likewise cannot self-register — accounts are created by another
 * admin via POST /admins — and there is deliberately no DELETE endpoint
 * for admin accounts anywhere in this controller.
 *
 * This controller intentionally depends on features.doctor.DoctorService,
 * features.appointment.AppointmentService, and features.patient DTOs —
 * admin actions on doctor/appointment/patient data are admin actions, not
 * duplicates of those domains' own logic, so the cross-slice calls stay
 * here rather than re-implementing that logic inside the admin slice.
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

    // ---------- Doctor management ----------

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
                    doctor.getId(), DoctorNameFormatter.format(doctor.getFullName()), doctor.getEmail(), doctor.getSpecialization(), doctor.getStatus()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** FR-032/FR-034: doctor roster, optionally filtered by name/email via ?search=. */
    @GetMapping("/doctors")
    public ResponseEntity<?> listDoctors(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String search
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        return ResponseEntity.ok(adminService.listDoctors(search));
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

    /** Admin removes a doctor account. */
    @DeleteMapping("/doctors/{id}")
    public ResponseEntity<?> deleteDoctor(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        try {
            adminService.deleteDoctor(id);
            return ResponseEntity.ok("Doctor account deleted.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- Patient management ----------

    /** FR-028/FR-030: patient roster, optionally filtered by name/email via ?search=. */
    @GetMapping("/patients")
    public ResponseEntity<?> listPatients(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String search
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        return ResponseEntity.ok(adminService.listPatients(search));
    }

    /** Admin creates a patient account directly. */
    @PostMapping("/patients")
    public ResponseEntity<?> createPatient(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody PatientCreateRequest request
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        try {
            return ResponseEntity.ok(adminService.createPatient(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Admin removes a patient account. */
    @DeleteMapping("/patients/{id}")
    public ResponseEntity<?> deletePatient(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        try {
            adminService.deletePatient(id);
            return ResponseEntity.ok("Patient account deleted.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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

    // ---------- Admin management ----------

    /** Full admin roster. No delete action exists anywhere for admin accounts — see class-level note. */
    @GetMapping("/admins")
    public ResponseEntity<?> listAdmins(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        return ResponseEntity.ok(adminService.listAdmins());
    }

    /** An admin creates another admin account. */
    @PostMapping("/admins")
    public ResponseEntity<?> createAdmin(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody AdminCreateRequest request
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        try {
            return ResponseEntity.ok(adminService.createAdmin(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * An admin changes their OWN password only — the target id comes from
     * the caller's own JWT, never from the request body or a path
     * variable, so there is no way to reach this endpoint and change
     * someone else's password.
     */
    @PutMapping("/password")
    public ResponseEntity<?> changeOwnPassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody PasswordChangeRequest request
    ) {
        ResponseEntity<?> authError = requireAdmin(authHeader);
        if (authError != null) return authError;

        try {
            Long adminId = jwtUtil.extractId(authHeader.substring(7));
            adminService.changeOwnPassword(adminId, request);
            return ResponseEntity.ok("Password updated successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------- Appointments (clinic-wide) ----------

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
