package edu.cit.Verona.AppointMed.appointmed_backend.features.notification.dto;

import java.util.List;

public class ReminderSettingsResponse {

    private List<Integer> offsetHours; // sorted descending, e.g. [24, 1]

    public ReminderSettingsResponse() {}

    public ReminderSettingsResponse(List<Integer> offsetHours) {
        this.offsetHours = offsetHours;
    }

    public List<Integer> getOffsetHours() { return offsetHours; }
    public void setOffsetHours(List<Integer> offsetHours) { this.offsetHours = offsetHours; }
}
