package edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.repository;

import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    // Ascending chronological order (nearest date/time first) everywhere —
    // every appointment list in the app (admin, doctor, patient, history,
    // dashboard) should read in this order per the app-wide sorting rule.
    List<Appointment> findByPatientIdOrderByDateAscTimeAsc(Long patientId);

    List<Appointment> findByDoctorIdOrderByDateAscTimeAsc(Long doctorId);

    List<Appointment> findAllByOrderByDateAscTimeAsc();

    List<Appointment> findByStatus(String status);

    List<Appointment> findByDoctorIdAndDateAndStatus(Long doctorId, LocalDate date, String status);

    boolean existsByDoctorIdAndDateAndTimeAndStatus(Long doctorId, LocalDate date, LocalTime time, String status);

    // Used to block a patient from holding two CONFIRMED appointments (even with
    // different doctors) at the same date/time — prevents scheduling conflicts.
    boolean existsByPatientIdAndDateAndTimeAndStatus(Long patientId, LocalDate date, LocalTime time, String status);
}
