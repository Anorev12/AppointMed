package edu.cit.Verona.AppointMed.appointmed_backend.features.availability.dto;

public class UnavailableDateRequest {
    private String date; // "yyyy-MM-dd"

    public UnavailableDateRequest() {}

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}