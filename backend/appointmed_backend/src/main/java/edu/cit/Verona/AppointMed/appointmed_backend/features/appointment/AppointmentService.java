package edu.cit.Verona.AppointMed.appointmed_backend.features.appointment;

import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.dto.AppointmentBookRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.dto.AppointmentResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.dto.SlotResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.entity.Appointment;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.repository.AppointmentRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.availability.AvailabilityService;
import edu.cit.Verona.AppointMed.appointmed_backend.features.availability.dto.AvailabilityResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.entity.Doctor;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Booking lives here rather than in the patient or doctor slices — it's a
 * cross-cutting concern that needs the doctor's availability rules
 * (features.availability), the doctor's identity (features.doctor), and its
 * own storage, so it gets its own feature slice like auth does.
 *
 * Slot length is fixed at 30 minutes, matching the mock data this replaces.
 */
@Service
public class AppointmentService {

    private static final int SLOT_MINUTES = 30;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final AvailabilityService availabilityService;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                               AvailabilityService availabilityService,
                               DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityService = availabilityService;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public AppointmentResponse book(Long patientId, AppointmentBookRequest request) {
        if (request.getDoctorId() == null) {
            throw new IllegalArgumentException("Select a doctor.");
        }

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found."));

        LocalDate date = parseDate(request.getDate());
        LocalTime time = parseTime(request.getTime());

        validateSlot(doctor.getId(), date, time);

        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(doctor.getId());
        appointment.setDoctorName(doctor.getFullName());
        appointment.setSpecialization(doctor.getSpecialization());
        appointment.setDate(date);
        appointment.setTime(time);
        appointment.setStatus("CONFIRMED");
        appointment.setReference("PENDING"); // placeholder until we have an id

        appointment = appointmentRepository.save(appointment);
        appointment.setReference(String.format("APT-%06d", appointment.getId()));
        appointment = appointmentRepository.save(appointment);

        return toResponse(appointment);
    }

    public List<AppointmentResponse> listForPatient(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByDateDescTimeDesc(patientId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponse cancel(Long patientId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));

        if (!appointment.getPatientId().equals(patientId)) {
            throw new SecurityException("This appointment doesn't belong to you.");
        }
        if ("CANCELLED".equals(appointment.getStatus())) {
            throw new IllegalArgumentException("This appointment is already cancelled.");
        }

        appointment.setStatus("CANCELLED");
        appointment = appointmentRepository.save(appointment);
        return toResponse(appointment);
    }

    /** Available 30-minute slots for a doctor on a given date, marking already-booked ones as reserved. */
    public List<SlotResponse> getAvailableSlots(Long doctorId, String dateStr) {
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found."));

        LocalDate date = parseDate(dateStr);
        AvailabilityResponse availability = availabilityService.getAvailability(doctorId);

        List<SlotResponse> slots = new ArrayList<>();

        String dayAbbrev = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        Set<String> workingDays = availability.getWorkingDays();
        boolean isWorkingDay = workingDays != null && workingDays.contains(dayAbbrev);
        boolean isMarkedUnavailable = availability.getUnavailableDates() != null
                && availability.getUnavailableDates().contains(date.toString());

        if (!isWorkingDay || isMarkedUnavailable) {
            return slots; // empty — doctor doesn't work that day
        }

        LocalTime start = LocalTime.parse(availability.getStartTime(), TIME_FMT);
        LocalTime end = LocalTime.parse(availability.getEndTime(), TIME_FMT);

        Set<LocalTime> bookedTimes = appointmentRepository
                .findByDoctorIdAndDateAndStatus(doctorId, date, "CONFIRMED")
                .stream()
                .map(Appointment::getTime)
                .collect(Collectors.toSet());

        boolean isToday = date.equals(LocalDate.now());
        LocalTime now = LocalTime.now();

        LocalTime cursor = start;
        while (cursor.isBefore(end)) {
            boolean isPast = isToday && !cursor.isAfter(now);
            boolean reserved = bookedTimes.contains(cursor) || isPast;
            slots.add(new SlotResponse(cursor.format(TIME_FMT), reserved));
            cursor = cursor.plusMinutes(SLOT_MINUTES);
        }

        return slots;
    }

    /** Re-validates a requested slot server-side — never trust that the frontend only offered valid times. */
    private void validateSlot(Long doctorId, LocalDate date, LocalTime time) {
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Can't book an appointment in the past.");
        }
        if (date.equals(LocalDate.now()) && !time.isAfter(LocalTime.now())) {
            throw new IllegalArgumentException("That time has already passed today.");
        }

        AvailabilityResponse availability = availabilityService.getAvailability(doctorId);
        String dayAbbrev = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

        if (availability.getWorkingDays() == null || !availability.getWorkingDays().contains(dayAbbrev)) {
            throw new IllegalArgumentException("This doctor isn't available on that day.");
        }
        if (availability.getUnavailableDates() != null && availability.getUnavailableDates().contains(date.toString())) {
            throw new IllegalArgumentException("This doctor is unavailable on that date.");
        }

        LocalTime start = LocalTime.parse(availability.getStartTime(), TIME_FMT);
        LocalTime end = LocalTime.parse(availability.getEndTime(), TIME_FMT);
        if (time.isBefore(start) || !time.isBefore(end)) {
            throw new IllegalArgumentException("That time is outside the doctor's working hours.");
        }

        if (appointmentRepository.existsByDoctorIdAndDateAndTimeAndStatus(doctorId, date, time, "CONFIRMED")) {
            throw new IllegalArgumentException("That slot was just booked by someone else. Please pick another.");
        }
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getReference(),
                a.getDoctorId(),
                a.getDoctorName(),
                a.getSpecialization(),
                a.getDate().toString(),
                a.getTime().format(TIME_FMT),
                a.getStatus()
        );
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Date must be in yyyy-MM-dd format.");
        }
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value, TIME_FMT);
        } catch (Exception e) {
            throw new IllegalArgumentException("Time must be in HH:mm format.");
        }
    }
}

