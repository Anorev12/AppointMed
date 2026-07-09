import { useState } from "react";
import "../../shared/styles/Appointmed.css";

/**
 * AppointMed — Patient Dashboard
 * Covers: FR-006 to FR-013 (browse doctors, book, reschedule/cancel, history)
 *
 * NOTE: this slice is still on local mock data end-to-end (booking,
 * cancellation, profile edits). When the backend endpoints for these land,
 * add a features/patient/api/patientApi.js alongside this file, following
 * the same pattern as the doctor and admin slices.
 */

const DOCTORS = [
  { id: 1, name: "Dr. Reyes", specialization: "Pediatrics" },
  { id: 2, name: "Dr. Tan", specialization: "Internal Medicine" },
  { id: 3, name: "Dr. Cruz", specialization: "Dermatology" },
];

const SLOTS_BY_DOCTOR = {
  1: [
    { time: "09:00", reserved: false },
    { time: "09:30", reserved: true },
    { time: "10:00", reserved: false },
    { time: "10:30", reserved: false },
  ],
  2: [
    { time: "10:00", reserved: false },
    { time: "10:30", reserved: false },
    { time: "11:00", reserved: true },
    { time: "11:30", reserved: false },
  ],
  3: [
    { time: "13:00", reserved: false },
    { time: "13:30", reserved: false },
    { time: "14:00", reserved: true },
    { time: "14:30", reserved: false },
  ],
};

const INITIAL_APPOINTMENTS = [
  { id: "APT-102938", doctor: "Dr. Reyes", specialization: "Pediatrics", date: "2026-07-10", time: "09:00", status: "confirmed" },
  { id: "APT-100221", doctor: "Dr. Cruz", specialization: "Dermatology", date: "2026-06-28", time: "14:00", status: "confirmed" },
  { id: "APT-099120", doctor: "Dr. Tan", specialization: "Internal Medicine", date: "2026-06-14", time: "11:00", status: "cancelled" },
];

function makeReference() {
  return `APT-${Math.floor(100000 + Math.random() * 900000)}`;
}

export default function PatientDashboard({ patientName = "Juan Dela Cruz", onLogout }) {
  const [view, setView] = useState("home"); // home | book | history | profile
  const [appointments, setAppointments] = useState(INITIAL_APPOINTMENTS);
  const [selectedDoctor, setSelectedDoctor] = useState(null);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [confirmedRef, setConfirmedRef] = useState(null);
  const [profile, setProfile] = useState({
    fullName: patientName,
    email: "juan.delacruz@email.com",
    contact: "0917 123 4567",
    medicalHistory: "No known allergies.",
  });

  const upcoming = appointments.filter((a) => a.status === "confirmed");

  function handleBook() {
    if (!selectedDoctor || !selectedSlot) return;
    const ref = makeReference();
    const doctor = DOCTORS.find((d) => d.id === selectedDoctor);
    setAppointments((prev) => [
      {
        id: ref,
        doctor: doctor.name,
        specialization: doctor.specialization,
        date: "2026-07-14",
        time: selectedSlot,
        status: "confirmed",
      },
      ...prev,
    ]);
    setConfirmedRef(ref);
  }

  function resetBooking() {
    setSelectedDoctor(null);
    setSelectedSlot(null);
    setConfirmedRef(null);
    setView("history");
  }

  function cancelAppointment(id) {
    setAppointments((prev) =>
      prev.map((a) => (a.id === id ? { ...a, status: "cancelled" } : a))
    );
  }

  return (
    <div className="db-shell">
      {/* ---------- Sidebar ---------- */}
      <div className="db-sidebar">
        <div className="db-brand">
          Appoint<span>Med</span>
        </div>
        <div className="db-role-tag">Patient portal</div>

        <div className="db-nav">
          <button
            className={`db-nav-item${view === "home" ? " is-active" : ""}`}
            onClick={() => setView("home")}
          >
            Dashboard
          </button>
          <button
            className={`db-nav-item${view === "book" ? " is-active" : ""}`}
            onClick={() => setView("book")}
          >
            Book Appointment
          </button>
          <button
            className={`db-nav-item${view === "history" ? " is-active" : ""}`}
            onClick={() => setView("history")}
          >
            My Appointments
          </button>
          <button
            className={`db-nav-item${view === "profile" ? " is-active" : ""}`}
            onClick={() => setView("profile")}
          >
            Profile
          </button>
        </div>

        <div className="db-sidebar-foot">
          <div className="db-avatar">
            {profile.fullName.split(" ").map((w) => w[0]).slice(0, 2).join("")}
          </div>
          <div>
            <div className="db-foot-name">{profile.fullName}</div>
            <div className="db-foot-role">Patient</div>
          </div>
          <button className="db-logout" onClick={onLogout} title="Log out">
            Exit
          </button>
        </div>
      </div>

      {/* ---------- Main ---------- */}
      <div className="db-main">
        <div className="db-topbar">
          <div>
            <div className="db-topbar-title">
              {view === "home" && "Welcome back"}
              {view === "book" && "Book an appointment"}
              {view === "history" && "My appointments"}
              {view === "profile" && "My profile"}
            </div>
            <div className="db-topbar-sub">
              {view === "home" && "Here's what's coming up."}
              {view === "book" && "Pick a doctor, then a time slot."}
              {view === "history" && "Everything you've booked, past and upcoming."}
              {view === "profile" && "Keep your details up to date."}
            </div>
          </div>
        </div>

        <div className="db-content">
          {/* ---------- HOME ---------- */}
          {view === "home" && (
            <>
              <div className="db-stats-grid">
                <div className="db-stat-card">
                  <div className="db-stat-value">{upcoming.length}</div>
                  <div className="db-stat-label">Upcoming appointments</div>
                </div>
                <div className="db-stat-card">
                  <div className="db-stat-value">{appointments.length}</div>
                  <div className="db-stat-label">Total visits on record</div>
                </div>
                <div className="db-stat-card">
                  <div className="db-stat-value">{DOCTORS.length}</div>
                  <div className="db-stat-label">Doctors available</div>
                </div>
              </div>

              <div className="db-panel">
                <div className="db-panel-head">
                  <div className="db-panel-title">Next appointment</div>
                  <button className="db-btn outline sm" onClick={() => setView("book")}>
                    Book another
                  </button>
                </div>
                <div className="db-panel-body">
                  {upcoming.length === 0 ? (
                    <div className="db-empty">No upcoming appointments yet.</div>
                  ) : (
                    upcoming.slice(0, 1).map((a) => (
                      <div className="db-row" key={a.id}>
                        <div className="db-row-time">{a.time}</div>
                        <div className="db-row-avatar">
                          {a.doctor.split(" ")[1]?.[0] || "D"}
                        </div>
                        <div className="db-row-main">
                          <div className="db-row-title">{a.doctor} · {a.specialization}</div>
                          <div className="db-row-sub">{a.date} · Ref {a.id}</div>
                        </div>
                        <span className="db-badge confirmed">Confirmed</span>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </>
          )}

          {/* ---------- BOOK ---------- */}
          {view === "book" && (
            <div className="db-two-col">
              <div className="db-panel">
                <div className="db-panel-head">
                  <div className="db-panel-title">Choose a doctor</div>
                </div>
                <div className="db-panel-body">
                  <div className="db-doctor-grid">
                    {DOCTORS.map((doc) => (
                      <div
                        key={doc.id}
                        className={`db-doctor-card${selectedDoctor === doc.id ? " is-selected" : ""}`}
                        onClick={() => {
                          setSelectedDoctor(doc.id);
                          setSelectedSlot(null);
                          setConfirmedRef(null);
                        }}
                      >
                        <div className="db-doctor-name">{doc.name}</div>
                        <div className="db-doctor-spec">{doc.specialization}</div>
                      </div>
                    ))}
                  </div>

                  {selectedDoctor && !confirmedRef && (
                    <div style={{ marginTop: 22 }}>
                      <div className="db-label" style={{ marginBottom: 10 }}>
                        Available time slots
                      </div>
                      <div className="db-slot-grid">
                        {SLOTS_BY_DOCTOR[selectedDoctor].map((slot) => (
                          <button
                            key={slot.time}
                            disabled={slot.reserved}
                            className={`db-slot-btn${slot.reserved ? " is-reserved" : ""}${
                              selectedSlot === slot.time ? " is-selected" : ""
                            }`}
                            onClick={() => setSelectedSlot(slot.time)}
                          >
                            {slot.time}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </div>

              <div className="db-panel">
                <div className="db-panel-head">
                  <div className="db-panel-title">Confirm booking</div>
                </div>
                <div className="db-panel-body">
                  {confirmedRef ? (
                    <div className="db-confirm-ticket">
                      <div className="db-confirm-code">{confirmedRef}</div>
                      <div style={{ fontWeight: 600, marginBottom: 6 }}>Booking confirmed</div>
                      <div style={{ fontSize: 13, color: "var(--ink-soft)", marginBottom: 14 }}>
                        A confirmation has been sent to your email. Reminders go out
                        24 hours and 1 hour before your visit.
                      </div>
                      <button className="db-btn primary sm" onClick={resetBooking}>
                        View my appointments
                      </button>
                    </div>
                  ) : (
                    <>
                      <div style={{ fontSize: 13.5, marginBottom: 14 }}>
                        {selectedDoctor
                          ? `${DOCTORS.find((d) => d.id === selectedDoctor).name}${
                              selectedSlot ? ` · ${selectedSlot}` : " · select a time"
                            }`
                          : "Select a doctor to continue."}
                      </div>
                      <button
                        className="db-btn primary"
                        disabled={!selectedDoctor || !selectedSlot}
                        onClick={handleBook}
                      >
                        Confirm appointment
                      </button>
                    </>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* ---------- HISTORY ---------- */}
          {view === "history" && (
            <div className="db-panel">
              <div className="db-panel-head">
                <div className="db-panel-title">Appointment history</div>
              </div>
              <div className="db-panel-body no-pad">
                <table className="db-table">
                  <thead>
                    <tr>
                      <th>Reference</th>
                      <th>Doctor</th>
                      <th>Date</th>
                      <th>Time</th>
                      <th>Status</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {appointments.map((a) => (
                      <tr key={a.id}>
                        <td style={{ fontFamily: "var(--font-mono)", fontSize: 12.5 }}>{a.id}</td>
                        <td>{a.doctor} <span style={{ color: "var(--ink-soft)" }}>· {a.specialization}</span></td>
                        <td>{a.date}</td>
                        <td>{a.time}</td>
                        <td>
                          <span className={`db-badge ${a.status}`}>{a.status}</span>
                        </td>
                        <td>
                          {a.status === "confirmed" && (
                            <button className="db-btn danger sm" onClick={() => cancelAppointment(a.id)}>
                              Cancel
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* ---------- PROFILE ---------- */}
          {view === "profile" && (
            <div className="db-panel" style={{ maxWidth: 480 }}>
              <div className="db-panel-head">
                <div className="db-panel-title">Profile information</div>
              </div>
              <div className="db-panel-body">
                <div className="db-field">
                  <label className="db-label">Full name</label>
                  <input
                    className="db-input"
                    value={profile.fullName}
                    onChange={(e) => setProfile({ ...profile, fullName: e.target.value })}
                  />
                </div>
                <div className="db-field">
                  <label className="db-label">Email</label>
                  <input
                    className="db-input"
                    value={profile.email}
                    onChange={(e) => setProfile({ ...profile, email: e.target.value })}
                  />
                </div>
                <div className="db-field">
                  <label className="db-label">Contact number</label>
                  <input
                    className="db-input"
                    value={profile.contact}
                    onChange={(e) => setProfile({ ...profile, contact: e.target.value })}
                  />
                </div>
                <div className="db-field">
                  <label className="db-label">Medical history</label>
                  <textarea
                    className="db-textarea"
                    rows={3}
                    value={profile.medicalHistory}
                    onChange={(e) => setProfile({ ...profile, medicalHistory: e.target.value })}
                  />
                </div>
                <button className="db-btn primary">Save changes</button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
