package edu.cit.Verona.AppointMed.appointmed_backend.features.availability.dto;

import java.util.Set;

public class AvailabilityUpdateRequest {
    private Set<String> workingDays;
    private String startTime; // "HH:mm"
    private String endTime;   // "HH:mm"

    public AvailabilityUpdateRequest() {}

    public Set<String> getWorkingDays() { return workingDays; }
    public void setWorkingDays(Set<String> workingDays) { this.workingDays = workingDays; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}