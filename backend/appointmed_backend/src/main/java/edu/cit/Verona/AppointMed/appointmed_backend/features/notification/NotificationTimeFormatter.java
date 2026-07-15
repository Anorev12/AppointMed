package edu.cit.Verona.AppointMed.appointmed_backend.features.notification;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 12-hour AM/PM formatting for times shown inside notification emails
 * (plain text and HTML). This mirrors the same rule already applied on
 * web and mobile — appointment times are stored/transmitted as 24-hour
 * (see AppointmentResponse.time), and only ever formatted for display,
 * here at the point where a human actually reads them.
 */
public final class NotificationTimeFormatter {

    private static final DateTimeFormatter TIME_12H = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private NotificationTimeFormatter() {}

    public static String format(LocalTime time) {
        if (time == null) return "";
        return time.format(TIME_12H);
    }
}

