package edu.cit.Verona.AppointMed.appointmed_backend.features.availability.repository;

import edu.cit.Verona.AppointMed.appointmed_backend.features.availability.entity.UnavailableDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UnavailableDateRepository extends JpaRepository<UnavailableDate, Long> {
    List<UnavailableDate> findByDoctorIdOrderByDateAsc(Long doctorId);
    Optional<UnavailableDate> findByDoctorIdAndDate(Long doctorId, LocalDate date);
    void deleteByDoctorIdAndDate(Long doctorId, LocalDate date);
}