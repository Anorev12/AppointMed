package edu.cit.Verona.AppointMed.appointmed_backend.features.availability.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "doctor_unavailable_dates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"doctorId", "date"})
)
public class UnavailableDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false)
    private LocalDate date;

    public UnavailableDate() {}

    public UnavailableDate(Long doctorId, LocalDate date) {
        this.doctorId = doctorId;
        this.date = date;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}