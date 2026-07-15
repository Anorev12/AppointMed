package edu.cit.Verona.AppointMed.appointmed_backend.features.doctor;

import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.dto.DoctorCreateRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.entity.Doctor;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.repository.DoctorRepository;
import org.springframework.stereotype.Service;

@Service
public class DoctorService {

    private static final String REQUIRED_DOMAIN = "@appointmeddoctor.com";

    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    /** Only ever called from AdminController — doctors cannot self-register. */
    public Doctor createDoctor(DoctorCreateRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();

        if (!email.endsWith(REQUIRED_DOMAIN)) {
            throw new IllegalArgumentException(
                "Doctor accounts must use an " + REQUIRED_DOMAIN + " email address."
            );
        }
        if (doctorRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        Doctor doctor = new Doctor();
        doctor.setFullName(request.getFullName());
        doctor.setEmail(email);
        doctor.setPassword(request.getPassword());
        doctor.setSpecialization(request.getSpecialization());

        return doctorRepository.save(doctor);
    }

    /**
     * Called by the unified login endpoint once it's already checked the
     * email ends in the doctor domain.
     */
    public Doctor login(String email, String rawPassword) {
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Incorrect email or password."));

        if (!doctor.getPassword().equals(rawPassword)) {
            throw new IllegalArgumentException("Incorrect email or password.");
        }

        return doctor;
    }

    /** Used by AuthController's /me endpoint to refresh the display name. */
    public String findFullNameByEmail(String email) {
        return doctorRepository.findByEmail(email)
                .map(Doctor::getFullName)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found."));
    }

    /** Used by DoctorController so patients can browse the full roster. */
    public java.util.List<Doctor> listAll() {
        return doctorRepository.findAll();
    }

    /** Used by admin endpoints (e.g. availability management) to validate a doctor id before acting on it. */
    public boolean existsById(Long id) {
        return doctorRepository.existsById(id);
    }

    /**
     * Doctor-initiated password change — requires the current password to
     * be supplied and correct, and the new password to be confirmed, before
     * anything is written. Matches PatientService/AdminService's changePassword shape.
     */
    public void changePassword(Long id, edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.dto.PasswordChangeRequest request) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found."));

        if (request.getOldPassword() == null || !doctor.getPassword().equals(request.getOldPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("New password can't be empty.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation don't match.");
        }

        doctor.setPassword(request.getNewPassword());
        doctorRepository.save(doctor);
    }
}