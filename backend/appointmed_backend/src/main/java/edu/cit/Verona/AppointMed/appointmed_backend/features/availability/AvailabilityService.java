package edu.cit.Verona.AppointMed.appointmed_backend.features.availability;

import edu.cit.Verona.AppointMed.appointmed_backend.features.availability.dto.AvailabilityResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.availability.dto.AvailabilityUpdateRequest;
import edu.cit.Verona.AppointMed.appointmed_backend.features.availability.entity.DoctorAvailability;
import edu.cit.Verona.AppointMed.appointmed_backend.features.availability.entity.UnavailableDate;
import edu.cit.Verona.AppointMed.appointmed_backend.features.availability.repository.DoctorAvailabilityRepository;
import edu.cit.Verona.AppointMed.appointmed_backend.features.availability.repository.UnavailableDateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    private static final Set<String> VALID_DAYS = Set.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final DoctorAvailabilityRepository availabilityRepository;
    private final UnavailableDateRepository unavailableDateRepository;

    public AvailabilityService(DoctorAvailabilityRepository availabilityRepository,
                                UnavailableDateRepository unavailableDateRepository) {
        this.availabilityRepository = availabilityRepository;
        this.unavailableDateRepository = unavailableDateRepository;
    }

    /** Fetches the doctor's schedule, creating sane defaults on first use. */
    public AvailabilityResponse getAvailability(Long doctorId) {
        DoctorAvailability availability = getOrCreate(doctorId);
        List<String> unavailableDates = unavailableDateRepository
                .findByDoctorIdOrderByDateAsc(doctorId)
                .stream()
                .map(d -> d.getDate().toString())
                .collect(Collectors.toList());

        return new AvailabilityResponse(
                availability.getWorkingDays(),
                availability.getStartTime().format(TIME_FMT),
                availability.getEndTime().format(TIME_FMT),
                unavailableDates
        );
    }

    @Transactional
    public AvailabilityResponse updateSchedule(Long doctorId, AvailabilityUpdateRequest request) {
        if (request.getWorkingDays() == null || request.getWorkingDays().isEmpty()) {
            throw new IllegalArgumentException("Select at least one working day.");
        }
        if (!VALID_DAYS.containsAll(request.getWorkingDays())) {
            throw new IllegalArgumentException("Working days must be one of Mon-Sun.");
        }

        LocalTime start = parseTime(request.getStartTime(), "Start time");
        LocalTime end = parseTime(request.getEndTime(), "End time");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }

        DoctorAvailability availability = getOrCreate(doctorId);
        availability.setWorkingDays(request.getWorkingDays());
        availability.setStartTime(start);
        availability.setEndTime(end);
        availabilityRepository.save(availability);

        return getAvailability(doctorId);
    }

    @Transactional
    public AvailabilityResponse addUnavailableDate(Long doctorId, String dateStr) {
        LocalDate date = parseDate(dateStr);

        if (unavailableDateRepository.findByDoctorIdAndDate(doctorId, date).isPresent()) {
            throw new IllegalArgumentException("That date is already marked unavailable.");
        }

        unavailableDateRepository.save(new UnavailableDate(doctorId, date));
        return getAvailability(doctorId);
    }

    @Transactional
    public AvailabilityResponse removeUnavailableDate(Long doctorId, String dateStr) {
        LocalDate date = parseDate(dateStr);
        unavailableDateRepository.deleteByDoctorIdAndDate(doctorId, date);
        return getAvailability(doctorId);
    }

    private DoctorAvailability getOrCreate(Long doctorId) {
        return availabilityRepository.findByDoctorId(doctorId)
                .orElseGet(() -> availabilityRepository.save(new DoctorAvailability(doctorId)));
    }

    private LocalTime parseTime(String value, String fieldName) {
        try {
            return LocalTime.parse(value, TIME_FMT);
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " must be in HH:mm format.");
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Date must be in yyyy-MM-dd format.");
        }
    }
}