package edu.cit.Verona.AppointMed.appointmed_backend.features.admin;

import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.dto.AdminPatientResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.entity.Admin;
import edu.cit.Verona.AppointMed.appointmed_backend.features.admin.repository.AdminRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.dto.DoctorResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.entity.Doctor;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.repository.DoctorRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.entity.Patient;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final Set<String> VALID_DOCTOR_STATUSES = Set.of("ACTIVE", "ON_LEAVE");

    private final AdminRepository adminRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AdminService(AdminRepository adminRepository, PatientRepository patientRepository, DoctorRepository doctorRepository) {
        this.adminRepository = adminRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
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

    /** FR-028/FR-030: full patient roster for the admin's Patients tab. */
    public List<AdminPatientResponse> listPatients() {
        return patientRepository.findAll().stream()
                .map(p -> new AdminPatientResponse(p.getId(), p.getFullName(), p.getEmail(), p.getContactNumber()))
                .collect(Collectors.toList());
    }

    /** FR-032/FR-034: full doctor roster, including active/on-leave status, for the admin's Doctors tab. */
    public List<DoctorResponse> listDoctors() {
        return doctorRepository.findAll().stream()
                .map(d -> new DoctorResponse(d.getId(), d.getFullName(), d.getEmail(), d.getSpecialization(), d.getStatus()))
                .collect(Collectors.toList());
    }

    /** FR-034: admin marks a doctor active or on leave. */
    public DoctorResponse updateDoctorStatus(Long doctorId, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!VALID_DOCTOR_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Status must be either ACTIVE or ON_LEAVE.");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found."));

        doctor.setStatus(normalized);
        doctor = doctorRepository.save(doctor);

        return new DoctorResponse(doctor.getId(), doctor.getFullName(), doctor.getEmail(), doctor.getSpecialization(), doctor.getStatus());
    }

    /** Confirms a patient id exists — used before returning that patient's appointment history to an admin. */
    public Patient requirePatient(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found."));
    }
}
