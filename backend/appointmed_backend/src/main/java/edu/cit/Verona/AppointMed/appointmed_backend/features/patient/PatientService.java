package edu.cit.Verona.AppointMed.appointmed_backend.features.patient;

import edu.cit.Verona.AppointMed.appointmed_backend.Security.PasswordVerifier;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.entity.Patient;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.repository.PatientRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto.PatientRegisterRequest;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PasswordVerifier passwordVerifier;

    public PatientService(PatientRepository patientRepository, PasswordVerifier passwordVerifier) {
        this.patientRepository = patientRepository;
        this.passwordVerifier = passwordVerifier;
    }

    public Patient registerPatient(PatientRegisterRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();

        // Prevent someone from registering a "patient" account on a
        // doctor/admin domain to try to sneak into those dashboards.
        if (email.endsWith("@appointmeddoctor.com") || email.endsWith("@appointmedadmin.com")) {
            throw new IllegalArgumentException("This email domain is reserved and can't be used for patient registration.");
        }

        if (patientRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        Patient patient = new Patient();
        patient.setFullName(request.getFullName());
        patient.setEmail(email);
        patient.setPassword(passwordVerifier.hash(request.getPassword()));
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setContactNumber(request.getContactNumber());
        patient.setMedicalHistory(request.getMedicalHistory());

        return patientRepository.save(patient);
    }

    /** Called by the unified login endpoint for any email not on a staff domain. */
    public Patient login(String email, String rawPassword) {
        Patient patient = patientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Incorrect email or password."));

        if (!passwordVerifier.matches(rawPassword, patient.getPassword())) {
            throw new IllegalArgumentException("Incorrect email or password.");
        }

        // Lazy migration: upgrade any pre-BCrypt (plaintext) row the moment
        // its owner successfully logs in, so no manual data migration is needed.
        if (!passwordVerifier.isBcryptHash(patient.getPassword())) {
            patient.setPassword(passwordVerifier.hash(rawPassword));
            patientRepository.save(patient);
        }

        return patient;
    }

    /** Used by AuthController's /me endpoint to refresh the display name. */
    public String findFullNameByEmail(String email) {
        return patientRepository.findByEmail(email)
                .map(Patient::getFullName)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found."));
    }

    /** Used by PatientProfileController to show the patient's own details. */
    public edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto.PatientProfileResponse getProfile(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found."));
        return toProfileResponse(patient);
    }

    /** Used by PatientProfileController to update name, contact number, date of birth, and medical history. */
    public edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto.PatientProfileResponse updateProfile(
            Long id, edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto.PatientProfileUpdateRequest request
    ) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found."));

        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new IllegalArgumentException("Full name can't be empty.");
        }

        java.time.LocalDate dob = patient.getDateOfBirth();
        if (request.getDateOfBirth() != null) {
            String raw = request.getDateOfBirth().trim();
            if (raw.isEmpty()) {
                dob = null;
            } else {
                try {
                    java.time.LocalDate parsed = java.time.LocalDate.parse(raw);
                    if (parsed.isAfter(java.time.LocalDate.now())) {
                        throw new IllegalArgumentException("Date of birth can't be in the future.");
                    }
                    dob = parsed;
                } catch (java.time.format.DateTimeParseException e) {
                    throw new IllegalArgumentException("Date of birth must be a valid date (yyyy-MM-dd).");
                }
            }
        }

        patient.setFullName(request.getFullName());
        patient.setContactNumber(request.getContactNumber());
        patient.setDateOfBirth(dob);
        patient.setMedicalHistory(request.getMedicalHistory());
        patient = patientRepository.save(patient);

        return toProfileResponse(patient);
    }

    private edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto.PatientProfileResponse toProfileResponse(Patient patient) {
        return new edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto.PatientProfileResponse(
                patient.getId(),
                patient.getFullName(),
                patient.getEmail(),
                patient.getContactNumber(),
                patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : null,
                patient.getMedicalHistory()
        );
    }

    /**
     * Patient-initiated password change — requires the current password to
     * be supplied and correct, and the new password to be confirmed, before
     * anything is written. Matches the same validation shape used by
     * DoctorService and AdminService's changePassword.
     */
    public void changePassword(Long id, edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto.PasswordChangeRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found."));

        if (!passwordVerifier.matches(request.getOldPassword(), patient.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("New password can't be empty.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation don't match.");
        }

        patient.setPassword(passwordVerifier.hash(request.getNewPassword()));
        patientRepository.save(patient);
    }
}