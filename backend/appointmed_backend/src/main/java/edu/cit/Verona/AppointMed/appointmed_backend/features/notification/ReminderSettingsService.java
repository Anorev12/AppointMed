package edu.cit.Verona.AppointMed.appointmed_backend.features.notification;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.dto.ReminderSettingsResponse;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.entity.ReminderSettings;
import edu.cit.Verona.AppointMed.appointmed_backend.features.notification.repository.ReminderSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FR-024 (reminder schedule half): lets an admin configure how many hours
 * before an appointment reminders fire — replacing the old hardcoded
 * "24 hours and 1 hour" from ReminderScheduler. Read by ReminderScheduler
 * on every run, so a change here takes effect within one scheduler tick
 * (every 5 minutes), no restart needed.
 */
@Service
public class ReminderSettingsService {

    private static final int MAX_OFFSETS = 5;
    private static final int MIN_HOURS = 1;
    private static final int MAX_HOURS = 168; // 1 week

    private final ReminderSettingsRepository repository;

    public ReminderSettingsService(ReminderSettingsRepository repository) {
        this.repository = repository;
    }

    public ReminderSettingsResponse get() {
        return new ReminderSettingsResponse(getOffsetHours());
    }

    /** Sorted descending (furthest-out reminder first), e.g. [24, 1]. Used directly by ReminderScheduler. */
    public List<Integer> getOffsetHours() {
        ReminderSettings settings = getOrCreate();
        return Arrays.stream(settings.getOffsetHoursCsv().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    @Transactional
    public ReminderSettingsResponse update(List<Integer> offsetHours) {
        if (offsetHours == null || offsetHours.isEmpty()) {
            throw new IllegalArgumentException("Select at least one reminder offset.");
        }
        if (offsetHours.size() > MAX_OFFSETS) {
            throw new IllegalArgumentException("At most " + MAX_OFFSETS + " reminder offsets are allowed.");
        }

        Set<Integer> distinct = new LinkedHashSet<>();
        for (Integer hours : offsetHours) {
            if (hours == null || hours < MIN_HOURS || hours > MAX_HOURS) {
                throw new IllegalArgumentException(
                        "Each reminder offset must be between " + MIN_HOURS + " and " + MAX_HOURS + " hours.");
            }
            distinct.add(hours);
        }

        String csv = distinct.stream().sorted(Comparator.reverseOrder())
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        ReminderSettings settings = getOrCreate();
        settings.setOffsetHoursCsv(csv);
        repository.save(settings);

        return new ReminderSettingsResponse(getOffsetHours());
    }

    private ReminderSettings getOrCreate() {
        return repository.findById(1L).orElseGet(() -> repository.save(new ReminderSettings()));
    }
}
