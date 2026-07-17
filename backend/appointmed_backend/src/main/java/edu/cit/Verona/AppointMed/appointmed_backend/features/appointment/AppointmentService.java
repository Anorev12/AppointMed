package edu.cit.Verona.AppointMed.appointmed_backend.features.appointment;

import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.dto.AppointmentBookRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.dto.AppointmentRescheduleRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.dto.AppointmentResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.dto.SlotResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.entity.Appointment;
import edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.repository.AppointmentRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.availability.AvailabilityService;
import edu.cit.Verona.AppointMed.appointmed_backend.features.availability.dto.AvailabilityResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.entity.Doctor;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.repository.DoctorRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.doctor.DoctorNameFormatter;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.NotificationService;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.entity.Patient;
import edu.cit.Verona.AppointMed.appointmed_backend.features.patient.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;

    /**
     * Business rule: "Patients may reschedule or cancel an appointment only
     * before the cutoff period configured by the clinic administrator."
     * There's no admin UI for this yet, so it's a single application-wide
     * value read from application.properties (defaults to 2 hours), rather
     * than a per-doctor/per-clinic DB setting.
     */
    @Value("${appointment.cutoff-hours:2}")
    private int cutoffHours;

    public AppointmentService(AppointmentRepository appointmentRepository,
                               AvailabilityService availabilityService,
                               DoctorRepository doctorRepository,
                               PatientRepository patientRepository,
                               NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityService = availabilityService;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.notificationService = notificationService;
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

        validateSlot(doctor.getId(), date, time, patientId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found."));

        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setPatientName(patient.getFullName());
        appointment.setDoctorId(doctor.getId());
        appointment.setDoctorName(doctor.getFullName());
        appointment.setSpecialization(doctor.getSpecialization());
        appointment.setDate(date);
        appointment.setTime(time);
        appointment.setStatus("CONFIRMED");
        appointment.setReference("PENDING-" + java.util.UUID.randomUUID()); // unique placeholder until we have an id — must never collide across concurrent bookings

        appointment = appointmentRepository.save(appointment);
        appointment.setReference(String.format("APT-%06d", appointment.getId()));
        appointment = appointmentRepository.save(appointment);

        notificationService.notifyBookingConfirmation(appointment, patient.getEmail()); // FR-013/FR-021
        notificationService.notifyNewBookingToDoctor(appointment, doctor.getEmail());   // FR-027

        return toResponse(appointment);
    }

    /**
     * FR-012: "maintain and display a complete, searchable appointment
     * history". status/keyword/from/to are all optional — when every
     * parameter is null this behaves exactly like the old unfiltered list.
     *
     * @param status  optional exact status match ("CONFIRMED" | "CANCELLED" | "COMPLETED"), case-insensitive
     * @param keyword optional free-text match against doctor name, specialization, or reference number
     * @param from    optional inclusive lower bound on appointment date (yyyy-MM-dd)
     * @param to      optional inclusive upper bound on appointment date (yyyy-MM-dd)
     */
    public List<AppointmentResponse> listForPatient(Long patientId, String status, String keyword, String from, String to) {
        List<Appointment> appointments = appointmentRepository.findByPatientIdOrderByDateAscTimeAsc(patientId);
        return applyFilters(appointments, status, keyword, from, to).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponse> listForPatient(Long patientId) {
        return listForPatient(patientId, null, null, null, null);
    }

    /**
     * FR-035: admin-wide view across every patient/doctor, with the same
     * optional status/keyword/from/to filters as the patient history search.
     * keyword additionally matches the patient's name, since an admin (unlike
     * a patient browsing their own history) needs to search across patients.
     */
    public List<AppointmentResponse> listAll(String status, String keyword, String from, String to) {
        List<Appointment> appointments = appointmentRepository.findAllByOrderByDateAscTimeAsc();

        LocalDate fromDate = (from == null || from.isBlank()) ? null : parseDate(from);
        LocalDate toDate = (to == null || to.isBlank()) ? null : parseDate(to);
        String needle = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase(Locale.ROOT);

        return appointments.stream()
                .filter(a -> status == null || status.isBlank() || a.getStatus().equalsIgnoreCase(status.trim()))
                .filter(a -> fromDate == null || !a.getDate().isBefore(fromDate))
                .filter(a -> toDate == null || !a.getDate().isAfter(toDate))
                .filter(a -> needle == null
                        || a.getDoctorName().toLowerCase(Locale.ROOT).contains(needle)
                        || a.getPatientName().toLowerCase(Locale.ROOT).contains(needle)
                        || (a.getSpecialization() != null && a.getSpecialization().toLowerCase(Locale.ROOT).contains(needle))
                        || a.getReference().toLowerCase(Locale.ROOT).contains(needle))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Shared status/date-range/keyword filter used by both the patient and admin history views. */
    private List<Appointment> applyFilters(List<Appointment> appointments, String status, String keyword, String from, String to) {
        LocalDate fromDate = (from == null || from.isBlank()) ? null : parseDate(from);
        LocalDate toDate = (to == null || to.isBlank()) ? null : parseDate(to);
        String needle = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase(Locale.ROOT);

        return appointments.stream()
                .filter(a -> status == null || status.isBlank() || a.getStatus().equalsIgnoreCase(status.trim()))
                .filter(a -> fromDate == null || !a.getDate().isBefore(fromDate))
                .filter(a -> toDate == null || !a.getDate().isAfter(toDate))
                .filter(a -> needle == null
                        || a.getDoctorName().toLowerCase(Locale.ROOT).contains(needle)
                        || (a.getSpecialization() != null && a.getSpecialization().toLowerCase(Locale.ROOT).contains(needle))
                        || a.getReference().toLowerCase(Locale.ROOT).contains(needle))
                .collect(Collectors.toList());
    }

    public List<AppointmentResponse> listForDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorIdOrderByDateAscTimeAsc(doctorId)
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
        if ("COMPLETED".equals(appointment.getStatus())) {
            throw new IllegalArgumentException("A completed appointment can't be cancelled.");
        }
        // FR-020: the cutoff protects against last-minute *patient* changes — it doesn't
        // apply when the clinic is the one who caused the change (doctor went unavailable).
        if (!appointment.isNeedsReschedule()) {
            requireBeforeCutoff(appointment);
        }

        appointment.setStatus("CANCELLED");
        appointment.setNeedsReschedule(false);
        appointment = appointmentRepository.save(appointment);

        Appointment finalAppointment = appointment;
        patientRepository.findById(patientId).ifPresent(p ->
                notificationService.notifyCancellation(finalAppointment, p.getEmail(), "you")); // FR-023

        return toResponse(appointment);
    }

    /**
     * FR-011: lets a patient move a confirmed appointment to a new date/time
     * with the same doctor. Re-runs the same slot validation booking uses
     * (working hours, unavailable dates, double-booking) against the new
     * slot, and enforces the same cancellation-cutoff business rule against
     * the *current* slot before allowing the move.
     */
    @Transactional
    public AppointmentResponse reschedule(Long patientId, Long appointmentId, AppointmentRescheduleRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));

        if (!appointment.getPatientId().equals(patientId)) {
            throw new SecurityException("This appointment doesn't belong to you.");
        }
        if (!"CONFIRMED".equals(appointment.getStatus())) {
            throw new IllegalArgumentException("Only a confirmed appointment can be rescheduled.");
        }
        // FR-020: same bypass as cancel() — a clinic-forced schedule change
        // shouldn't be blocked by the patient-initiated-change cutoff.
        if (!appointment.isNeedsReschedule()) {
            requireBeforeCutoff(appointment);
        }

        LocalDate newDate = parseDate(request.getDate());
        LocalTime newTime = parseTime(request.getTime());

        if (newDate.equals(appointment.getDate()) && newTime.equals(appointment.getTime())) {
            throw new IllegalArgumentException("That's already your current appointment time.");
        }

        validateSlot(appointment.getDoctorId(), newDate, newTime, patientId);

        String oldDateStr = appointment.getDate().toString();
        String oldTimeStr = edu.cit.Verona.AppointMed.appointmed_backend.features.notification.NotificationTimeFormatter.format(appointment.getTime());

        appointment.setDate(newDate);
        appointment.setTime(newTime);
        appointment.setNeedsReschedule(false);
        // Reference number and status are preserved — this is the same booking, just moved.
        appointment = appointmentRepository.save(appointment);

        Appointment finalAppointment = appointment;
        patientRepository.findById(patientId).ifPresent(p ->
                notificationService.notifyReschedule(finalAppointment, p.getEmail(), oldDateStr, oldTimeStr)); // FR-023

        return toResponse(appointment);
    }

    /** Business rule: no cancel/reschedule once we're inside the configured cutoff window. */
    private void requireBeforeCutoff(Appointment appointment) {
        LocalDateTime appointmentStart = LocalDateTime.of(appointment.getDate(), appointment.getTime());
        LocalDateTime cutoff = appointmentStart.minusHours(cutoffHours);
        if (LocalDateTime.now().isAfter(cutoff)) {
            throw new IllegalArgumentException(
                    "This appointment can no longer be changed — it's within the " + cutoffHours
                            + "-hour cutoff period. Please contact the clinic directly.");
        }
    }

    /**
     * FR-016/FR-035: admin override cancel — unlike the patient-initiated
     * cancel(), this ignores the reschedule cutoff window and doesn't check
     * appointment ownership, since an admin can act on any booking.
     */
    @Transactional
    public AppointmentResponse cancelByAdmin(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));

        if ("CANCELLED".equals(appointment.getStatus())) {
            throw new IllegalArgumentException("This appointment is already cancelled.");
        }

        appointment.setStatus("CANCELLED");
        appointment = appointmentRepository.save(appointment);

        Appointment finalAppointment = appointment;
        patientRepository.findById(finalAppointment.getPatientId()).ifPresent(p ->
                notificationService.notifyCancellation(finalAppointment, p.getEmail(), "the clinic administrator")); // FR-023

        return toResponse(appointment);
    }

    /** Doctor-initiated cancellation — e.g. the doctor can't make that slot after all. */
    @Transactional
    public AppointmentResponse cancelByDoctor(Long doctorId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));

        if (!appointment.getDoctorId().equals(doctorId)) {
            throw new SecurityException("This appointment doesn't belong to you.");
        }
        if ("CANCELLED".equals(appointment.getStatus())) {
            throw new IllegalArgumentException("This appointment is already cancelled.");
        }

        appointment.setStatus("CANCELLED");
        appointment = appointmentRepository.save(appointment);

        Appointment finalAppointment = appointment;
        patientRepository.findById(finalAppointment.getPatientId()).ifPresent(p ->
                notificationService.notifyCancellation(finalAppointment, p.getEmail(), "your doctor")); // FR-023

        return toResponse(appointment);
    }

    /** Lets a doctor close out a visit that actually happened, for record-keeping. */
    @Transactional
    public AppointmentResponse completeByDoctor(Long doctorId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));

        if (!appointment.getDoctorId().equals(doctorId)) {
            throw new SecurityException("This appointment doesn't belong to you.");
        }
        if (!"CONFIRMED".equals(appointment.getStatus())) {
            throw new IllegalArgumentException("Only confirmed appointments can be marked complete.");
        }

        appointment.setStatus("COMPLETED");
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
    private void validateSlot(Long doctorId, LocalDate date, LocalTime time, Long patientId) {
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

        // Prevent a patient from holding two CONFIRMED appointments — even with
        // different doctors — at the same date and time.
        if (appointmentRepository.existsByPatientIdAndDateAndTimeAndStatus(patientId, date, time, "CONFIRMED")) {
            throw new IllegalArgumentException("You already have a confirmed appointment at this date and time with another doctor.");
        }
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getReference(),
                a.getDoctorId(),
                DoctorNameFormatter.format(a.getDoctorName()),
                a.getSpecialization(),
                a.getPatientName(),
                a.getDate().toString(),
                a.getTime().format(TIME_FMT),
                a.getStatus(),
                a.isNeedsReschedule()
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