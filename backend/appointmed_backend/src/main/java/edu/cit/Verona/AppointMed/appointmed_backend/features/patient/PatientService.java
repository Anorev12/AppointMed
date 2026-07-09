package edu.cit.Verona.AppointMed.appointmed_backend.features.patient;

import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.entity.Patient;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.repository.PatientRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto.PatientRegisterRequest;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
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
        patient.setPassword(request.getPassword());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setContactNumber(request.getContactNumber());
        patient.setMedicalHistory(request.getMedicalHistory());

        return patientRepository.save(patient);
    }

    /** Called by the unified login endpoint for any email not on a staff domain. */
    public Patient login(String email, String rawPassword) {
        Patient patient = patientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Incorrect email or password."));

        if (!patient.getPassword().equals(rawPassword)) {
            throw new IllegalArgumentException("Incorrect email or password.");
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

    /** Used by PatientProfileController to update name, contact number, and medical history. */
    public edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto.PatientProfileResponse updateProfile(
            Long id, edu.cit.Verona.AppointMed.appointmed_backend.features.patient.dto.PatientProfileUpdateRequest request
    ) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found."));

        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new IllegalArgumentException("Full name can't be empty.");
        }

        patient.setFullName(request.getFullName());
        patient.setContactNumber(request.getContactNumber());
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
}