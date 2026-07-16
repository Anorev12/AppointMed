package edu.cit.Verona.AppointMed.appointmed_backend.features.notification.dto;

import java.util.List;

public class ReminderSettingsUpdateRequest {

    private List<Integer> offsetHours;

    public List<Integer> getOffsetHours() { return offsetHours; }
    public void setOffsetHours(List<Integer> offsetHours) { this.offsetHours = offsetHours; }
}
