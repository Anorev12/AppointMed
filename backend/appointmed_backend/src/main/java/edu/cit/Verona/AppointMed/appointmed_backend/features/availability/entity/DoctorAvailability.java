package edu.cit.Verona.AppointMed.appointmed_backend.features.availability.entity;

import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One row per doctor. Created lazily with sensible defaults (Mon-Fri, 09:00-17:00)
 * the first time a doctor's availability is requested, then updated in place.
 *
 * Deliberately stores only doctorId (Long), not a Doctor entity reference —
 * this feature slice doesn't need to know anything about how doctors are
 * modeled elsewhere in the app.
 */
@Entity
@Table(name = "doctor_availability")
public class DoctorAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long doctorId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "doctor_working_days",
            joinColumns = @JoinColumn(name = "availability_id")
    )
    @Column(name = "day")
    private Set<String> workingDays = new LinkedHashSet<>();

    @Column(nullable = false)
    private LocalTime startTime = LocalTime.of(9, 0);

    @Column(nullable = false)
    private LocalTime endTime = LocalTime.of(17, 0);

    public DoctorAvailability() {}

    public DoctorAvailability(Long doctorId) {
        this.doctorId = doctorId;
        this.workingDays = new LinkedHashSet<>(Set.of("Mon", "Tue", "Wed", "Thu", "Fri"));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public Set<String> getWorkingDays() { return workingDays; }
    public void setWorkingDays(Set<String> workingDays) { this.workingDays = workingDays; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}