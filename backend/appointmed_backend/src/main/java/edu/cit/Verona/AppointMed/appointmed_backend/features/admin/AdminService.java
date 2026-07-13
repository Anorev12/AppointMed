package edu.cit.Verona.AppointMed.appointmed_backend.features.admin;

import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto.AdminCreateRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto.AdminPatientResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto.AdminResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto.PasswordChangeRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.entity.Admin;
import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.repository.AdminRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.DoctorNameFormatter;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.dto.DoctorResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.entity.Doctor;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.repository.DoctorRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.dto.NotificationResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.repository.NotificationRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.entity.Patient;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto.PatientCreateRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final Set<String> VALID_DOCTOR_STATUSES = Set.of("ACTIVE", "ON_LEAVE");
    private static final String ADMIN_DOMAIN = "@appointmedadmin.com";
    private static final String DOCTOR_DOMAIN = "@appointmeddoctor.com";

    private final AdminRepository adminRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final NotificationRepository notificationRepository;

    public AdminService(AdminRepository adminRepository, PatientRepository patientRepository,
                         DoctorRepository doctorRepository, NotificationRepository notificationRepository) {
        this.adminRepository = adminRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.notificationRepository = notificationRepository;
    }

    /** Called by the unified login endpoint once it's already checked the email ends in the admin domain. */
    public Admin login(String email, String rawPassword) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Incorrect email or password."));

        if (!admin.getPassword().equals(rawPassword)) {
            throw new IllegalArgumentException("Incorrect email or password.");
        }

        return admin;
    }

    /** Used by AuthController's /me endpoint to refresh the display name. */
    public String findFullNameByEmail(String email) {
        return adminRepository.findByEmail(email)
                .map(Admin::getFullName)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found."));
    }

    // ---------- Patient management ----------

    /** FR-028/FR-030: patient roster for the admin's Patients tab, with an optional name/email search filter. */
    public List<AdminPatientResponse> listPatients(String search) {
        String needle = (search == null || search.isBlank()) ? null : search.trim().toLowerCase(Locale.ROOT);

        return patientRepository.findAll().stream()
                .filter(p -> needle == null
                        || p.getFullName().toLowerCase(Locale.ROOT).contains(needle)
                        || p.getEmail().toLowerCase(Locale.ROOT).contains(needle))
                .map(p -> new AdminPatientResponse(p.getId(), p.getFullName(), p.getEmail(), p.getContactNumber()))
                .collect(Collectors.toList());
    }

    /** Admin creates a patient account directly — separate from the patient's own self-register flow. */
    public AdminPatientResponse createPatient(PatientCreateRequest request) {
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (email.endsWith(DOCTOR_DOMAIN) || email.endsWith(ADMIN_DOMAIN)) {
            throw new IllegalArgumentException("This email domain is reserved and can't be used for a patient account.");
        }
        if (patientRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        Patient patient = new Patient();
        patient.setFullName(request.getFullName());
        patient.setEmail(email);
        patient.setPassword(request.getPassword());
        patient.setContactNumber(request.getContactNumber());

        patient = patientRepository.save(patient);
        return new AdminPatientResponse(patient.getId(), patient.getFullName(), patient.getEmail(), patient.getContactNumber());
    }

    /** Admin removes a patient account. Appointment history rows are left as-is (no cascade), same as everywhere else in this codebase. */
    public void deletePatient(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new IllegalArgumentException("Patient not found.");
        }
        patientRepository.deleteById(patientId);
    }

    // ---------- Doctor management ----------

    /** FR-032/FR-034: doctor roster, including active/on-leave status, with an optional name/email search filter. */
    public List<DoctorResponse> listDoctors(String search) {
        String needle = (search == null || search.isBlank()) ? null : search.trim().toLowerCase(Locale.ROOT);

        return doctorRepository.findAll().stream()
                .filter(d -> needle == null
                        || d.getFullName().toLowerCase(Locale.ROOT).contains(needle)
                        || d.getEmail().toLowerCase(Locale.ROOT).contains(needle))
                .map(d -> new DoctorResponse(d.getId(), DoctorNameFormatter.format(d.getFullName()), d.getEmail(), d.getSpecialization(), d.getStatus()))
                .collect(Collectors.toList());
    }

    /** FR-034: admin marks a doctor active or on leave. */
    public DoctorResponse updateDoctorStatus(Long doctorId, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!VALID_DOCTOR_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Status must be either ACTIVE or ON_LEAVE.");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found."));

        doctor.setStatus(normalized);
        doctor = doctorRepository.save(doctor);

        return new DoctorResponse(doctor.getId(), DoctorNameFormatter.format(doctor.getFullName()), doctor.getEmail(), doctor.getSpecialization(), doctor.getStatus());
    }

    /** Admin removes a doctor account. Appointment history rows are left as-is (no cascade), same as everywhere else in this codebase. */
    public void deleteDoctor(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new IllegalArgumentException("Doctor not found.");
        }
        doctorRepository.deleteById(doctorId);
    }

    // ---------- Admin management ----------

    /** FR-032: full admin roster. There is deliberately no delete method for admins — see the class-level business rule. */
    public List<AdminResponse> listAdmins() {
        return adminRepository.findAll().stream()
                .map(a -> new AdminResponse(a.getId(), a.getFullName(), a.getEmail()))
                .collect(Collectors.toList());
    }

    /**
     * An admin creating another admin account. Requires the admin domain,
     * same enforcement style as DoctorService.createDoctor requiring the
     * doctor domain. There is intentionally no self-register path for admins.
     */
    public AdminResponse createAdmin(AdminCreateRequest request) {
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!email.endsWith(ADMIN_DOMAIN)) {
            throw new IllegalArgumentException("Admin accounts must use an " + ADMIN_DOMAIN + " email address.");
        }
        if (adminRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        Admin admin = new Admin();
        admin.setFullName(request.getFullName());
        admin.setEmail(email);
        admin.setPassword(request.getPassword());

        admin = adminRepository.save(admin);
        return new AdminResponse(admin.getId(), admin.getFullName(), admin.getEmail());
    }

    /**
     * Self-service only — the caller's own id comes from their JWT
     * (see AdminController), so there is no path here that lets one admin
     * change another admin's password.
     */
    public void changeOwnPassword(Long adminId, PasswordChangeRequest request) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found."));

        if (request.getOldPassword() == null || !admin.getPassword().equals(request.getOldPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("New password can't be empty.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation don't match.");
        }

        admin.setPassword(request.getNewPassword());
        adminRepository.save(admin);
    }

    /** Confirms a patient id exists — used before returning that patient's appointment history to an admin. */
    public Patient requirePatient(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found."));
    }

    /** FR-025: the notification log — every confirmation, cancellation, reschedule, and reminder ever attempted. */
    public List<NotificationResponse> listNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(NotificationResponse::new)
                .collect(Collectors.toList());
    }
}
