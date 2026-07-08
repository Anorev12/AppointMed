package edu.cit.Verona.AppointMed.appointmed_backend.features.availability.dto;

import java.util.List;
import java.util.Set;

public class AvailabilityResponse {
    private Set<String> workingDays;
    private String startTime;   // "HH:mm"
    private String endTime;     // "HH:mm"
    private List<String> unavailableDates; // "yyyy-MM-dd"

    public AvailabilityResponse() {}

    public AvailabilityResponse(Set<String> workingDays, String startTime, String endTime,
                                 List<String> unavailableDates) {
        this.workingDays = workingDays;
        this.startTime = startTime;
        this.endTime = endTime;
        this.unavailableDates = unavailableDates;
    }

    public Set<String> getWorkingDays() { return workingDays; }
    public void setWorkingDays(Set<String> workingDays) { this.workingDays = workingDays; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public List<String> getUnavailableDates() { return unavailableDates; }
    public void setUnavailableDates(List<String> unavailableDates) { this.unavailableDates = unavailableDates; }
}