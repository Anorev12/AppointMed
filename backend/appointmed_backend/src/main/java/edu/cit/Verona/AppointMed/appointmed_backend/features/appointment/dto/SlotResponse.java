package edu.cit.Verona.AppointMed.appointmed_backend.features.appointment.dto;

public class SlotResponse {
    private String time; // "HH:mm"
    private boolean reserved;

    public SlotResponse() {}

    public SlotResponse(String time, boolean reserved) {
        this.time = time;
        this.reserved = reserved;
    }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public boolean isReserved() { return reserved; }
    public void setReserved(boolean reserved) { this.reserved = reserved; }
}
