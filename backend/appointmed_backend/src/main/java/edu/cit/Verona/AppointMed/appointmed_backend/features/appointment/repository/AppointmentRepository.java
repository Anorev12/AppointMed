package edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.repository;

import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByDateDescTimeDesc(Long patientId);

    List<Appointment> findByDoctorIdOrderByDateDescTimeDesc(Long doctorId);

    List<Appointment> findAllByOrderByDateDescTimeDesc();

    List<Appointment> findByStatus(String status);

    List<Appointment> findByDoctorIdAndDateAndStatus(Long doctorId, LocalDate date, String status);

    boolean existsByDoctorIdAndDateAndTimeAndStatus(Long doctorId, LocalDate date, LocalTime time, String status);
}
