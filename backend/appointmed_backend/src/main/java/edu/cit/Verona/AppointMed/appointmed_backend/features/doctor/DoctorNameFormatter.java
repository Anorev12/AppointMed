package edu.cit.Verona.AppointMed.appointmed_backend.features.doctor;

/**
 * Single source of truth for the "Dr." prefix requirement — applied here,
 * at the backend response layer, so every client (web, mobile, admin
 * dashboard, and notification emails) gets a consistently formatted name
 * without each frontend needing its own copy of this logic.
 *
 * Idempotent: a name that already starts with "Dr." (any case) is returned
 * unchanged, so this is always safe to call even on a name that might
 * already be formatted.
 */
public final class DoctorNameFormatter {

    private DoctorNameFormatter() {}

    public static String format(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return fullName;
        }
        String trimmed = fullName.trim();
        if (trimmed.toLowerCase().startsWith("dr.") || trimmed.toLowerCase().startsWith("dr ")) {
            return trimmed;
        }
        return "Dr. " + trimmed;
    }
}
