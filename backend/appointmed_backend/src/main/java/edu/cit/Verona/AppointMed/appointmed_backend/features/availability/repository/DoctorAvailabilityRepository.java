package edu.cit.Verona.AppointMed.appointmed_backend.features.availability.repository;

import edu.cit.Verona.AppointMed.appointmed_backend.features.availability.entity.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {
    Optional<DoctorAvailability> findByDoctorId(Long doctorId);
}