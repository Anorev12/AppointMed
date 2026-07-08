package edu.cit.Verona.AppointMed.appointmed_backend.features.patient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.entity.Patient;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByEmail(String email);
    boolean existsByEmail(String email);
}