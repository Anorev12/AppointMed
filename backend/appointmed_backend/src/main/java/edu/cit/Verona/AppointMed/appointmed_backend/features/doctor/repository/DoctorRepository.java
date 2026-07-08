package edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.repository;

import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByEmail(String email);
    boolean existsByEmail(String email);
}