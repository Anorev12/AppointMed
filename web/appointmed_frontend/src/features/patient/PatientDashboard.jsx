import { useState, useEffect, useCallback } from "react";
import "../../shared/styles/Appointmed.css";
import { DoctorsAPI, AppointmentsAPI, PatientProfileAPI } from "./api/patientApi";

/**
 * AppointMed — Patient Dashboard
 * Covers: FR-006 to FR-013 (browse doctors, book, cancel, history, profile)
 *
 * Fully wired to the backend:
 *  - GET  /api/doctors                         (browse doctors)
 *  - GET  /api/doctors/{id}/slots?date=...      (open slots for a day)
 *  - GET  /api/patient/appointments             (my appointments)
 *  - POST /api/patient/appointments             (book)
 *  - PUT  /api/patient/appointments/{id}/cancel (cancel)
 *  - GET  /api/patient/profile                  (my profile)
 *  - PUT  /api/patient/profile                  (update profile)
 */

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

export default function PatientDashboard({ patientName = "Patient", onLogout }) {
  const [view, setView] = useState("home"); // home | book | history | profile

  // ---- Doctors ----
  const [doctors, setDoctors] = useState([]);
  const [doctorsLoading, setDoctorsLoading] = useState(true);
  const [doctorsError, setDoctorsError] = useState("");

  // ---- Appointments ----
  const [appointments, setAppointments] = useState([]);
  const [apptsLoading, setApptsLoading] = useState(true);
  const [apptsError, setApptsError] = useState("");
  const [cancellingId, setCancellingId] = useState(null);

  // ---- Booking flow ----
  const [selectedDoctor, setSelectedDoctor] = useState(null);
  const [selectedDate, setSelectedDate] = useState(todayStr());
  const [slots, setSlots] = useState([]);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [slotsError, setSlotsError] = useState("");
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [booking, setBooking] = useState(false);
  const [bookError, setBookError] = useState("");
  const [confirmedAppt, setConfirmedAppt] = useState(null);

  // ---- Profile ----
  const [profile, setProfile] = useState({
    fullName: patientName,
    email: "",
    contact: "",
    dateOfBirth: "",
    medicalHistory: "",
  });
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileError, setProfileError] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState("");

  useEffect(() => {
    loadDoctors();
    loadAppointments();
    loadProfile();
  }, []);

  async function loadDoctors() {
    setDoctorsLoading(true);
    setDoctorsError("");
    try {
      const data = await DoctorsAPI.list();
      setDoctors(data || []);
    } catch (err) {
      setDoctorsError(err.message || "Couldn't load the list of doctors.");
    } finally {
      setDoctorsLoading(false);
    }
  }

  async function loadAppointments() {
    setApptsLoading(true);
    setApptsError("");
    try {
      const data = await AppointmentsAPI.list();
      setAppointments(data || []);
    } catch (err) {
      setApptsError(err.message || "Couldn't load your appointments.");
    } finally {
      setApptsLoading(false);
    }
  }

  async function loadProfile() {
    setProfileLoading(true);
    setProfileError("");
    try {
      const data = await PatientProfileAPI.get();
      setProfile({
        fullName: data.fullName || "",
        email: data.email || "",
        contact: data.contactNumber || "",
        dateOfBirth: data.dateOfBirth || "",
        medicalHistory: data.medicalHistory || "",
      });
    } catch (err) {
      setProfileError(err.message || "Couldn't load your profile.");
    } finally {
      setProfileLoading(false);
    }
  }

  const loadSlots = useCallback(async (doctorId, date) => {
    if (!doctorId || !date) return;
    setSlotsLoading(true);
    setSlotsError("");
    setSlots([]);
    try {
      const data = await DoctorsAPI.getSlots(doctorId, date);
      setSlots(data || []);
    } catch (err) {
      setSlotsError(err.message || "Couldn't load open slots for that day.");
    } finally {
      setSlotsLoading(false);
    }
  }, []);

  function pickDoctor(doctorId) {
    setSelectedDoctor(doctorId);
    setSelectedSlot(null);
    setConfirmedAppt(null);
    setBookError("");
    loadSlots(doctorId, selectedDate);
  }

  function changeDate(date) {
    setSelectedDate(date);
    setSelectedSlot(null);
    setConfirmedAppt(null);
    setBookError("");
    if (selectedDoctor) loadSlots(selectedDoctor, date);
  }

  async function handleBook() {
    if (!selectedDoctor || !selectedSlot) return;
    setBooking(true);
    setBookError("");
    try {
      const appt = await AppointmentsAPI.book(selectedDoctor, selectedDate, selectedSlot);
      setConfirmedAppt(appt);
      setAppointments((prev) => [appt, ...prev]);
      loadSlots(selectedDoctor, selectedDate); // that slot is now reserved
    } catch (err) {
      setBookError(err.message || "Couldn't book that appointment.");
    } finally {
      setBooking(false);
    }
  }

  function resetBooking() {
    setSelectedDoctor(null);
    setSelectedSlot(null);
    setConfirmedAppt(null);
    setView("history");
  }

  async function cancelAppointment(id) {
    setCancellingId(id);
    setApptsError("");
    try {
      const updated = await AppointmentsAPI.cancel(id);
      setAppointments((prev) => prev.map((a) => (a.id === id ? updated : a)));
    } catch (err) {
      setApptsError(err.message || "Couldn't cancel that appointment.");
    } finally {
      setCancellingId(null);
    }
  }

  async function saveProfile() {
    setSaving(true);
    setSaveMessage("");
    setProfileError("");
    try {
      const updated = await PatientProfileAPI.update(
        profile.fullName,
        profile.contact,
        profile.medicalHistory
      );
      setProfile((p) => ({ ...p, fullName: updated.fullName, contact: updated.contactNumber }));
      setSaveMessage("Saved.");
    } catch (err) {
      setProfileError(err.message || "Couldn't save your profile.");
    } finally {
      setSaving(false);
    }
  }

  const upcoming = appointments.filter((a) => a.status === "CONFIRMED");
  const selectedDoctorObj = doctors.find((d) => d.id === selectedDoctor);

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
            {(profile.fullName || patientName).split(" ").map((w) => w[0]).slice(0, 2).join("")}
          </div>
          <div>
            <div className="db-foot-name">{profile.fullName || patientName}</div>
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
              {view === "book" && "Pick a doctor, then a date and time."}
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
                  <div className="db-stat-value">{apptsLoading ? "…" : upcoming.length}</div>
                  <div className="db-stat-label">Upcoming appointments</div>
                </div>
                <div className="db-stat-card">
                  <div className="db-stat-value">{apptsLoading ? "…" : appointments.length}</div>
                  <div className="db-stat-label">Total visits on record</div>
                </div>
                <div className="db-stat-card">
                  <div className="db-stat-value">{doctorsLoading ? "…" : doctors.length}</div>
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
                  {apptsError && <div className="db-error">{apptsError}</div>}
                  {apptsLoading ? (
                    <div className="db-empty">Loading…</div>
                  ) : upcoming.length === 0 ? (
                    <div className="db-empty">No upcoming appointments yet.</div>
                  ) : (
                    upcoming.slice(0, 1).map((a) => (
                      <div className="db-row" key={a.id}>
                        <div className="db-row-time">{a.time}</div>
                        <div className="db-row-avatar">
                          {a.doctorName?.split(" ")[1]?.[0] || "D"}
                        </div>
                        <div className="db-row-main">
                          <div className="db-row-title">{a.doctorName} · {a.specialization}</div>
                          <div className="db-row-sub">{a.date} · Ref {a.reference}</div>
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
                  {doctorsError && <div className="db-error">{doctorsError}</div>}
                  {doctorsLoading ? (
                    <div className="db-empty">Loading doctors…</div>
                  ) : doctors.length === 0 ? (
                    <div className="db-empty">No doctors available yet.</div>
                  ) : (
                    <div className="db-doctor-grid">
                      {doctors.map((doc) => (
                        <div
                          key={doc.id}
                          className={`db-doctor-card${selectedDoctor === doc.id ? " is-selected" : ""}`}
                          onClick={() => pickDoctor(doc.id)}
                        >
                          <div className="db-doctor-name">{doc.fullName}</div>
                          <div className="db-doctor-spec">{doc.specialization}</div>
                        </div>
                      ))}
                    </div>
                  )}

                  {selectedDoctor && !confirmedAppt && (
                    <div style={{ marginTop: 22 }}>
                      <div className="db-label" style={{ marginBottom: 10 }}>
                        Date
                      </div>
                      <input
                        type="date"
                        className="db-input"
                        style={{ marginBottom: 20, maxWidth: 220 }}
                        value={selectedDate}
                        min={todayStr()}
                        onChange={(e) => changeDate(e.target.value)}
                      />

                      <div className="db-label" style={{ marginBottom: 10 }}>
                        Available time slots
                      </div>
                      {slotsError && <div className="db-error">{slotsError}</div>}
                      {slotsLoading ? (
                        <div className="db-empty">Loading slots…</div>
                      ) : slots.length === 0 ? (
                        <div className="db-empty">
                          This doctor has no open hours on that day. Try another date.
                        </div>
                      ) : (
                        <div className="db-slot-grid">
                          {slots.map((slot) => (
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
                      )}
                    </div>
                  )}
                </div>
              </div>

              <div className="db-panel">
                <div className="db-panel-head">
                  <div className="db-panel-title">Confirm booking</div>
                </div>
                <div className="db-panel-body">
                  {confirmedAppt ? (
                    <div className="db-confirm-ticket">
                      <div className="db-confirm-code">{confirmedAppt.reference}</div>
                      <div style={{ fontWeight: 600, marginBottom: 6 }}>Booking confirmed</div>
                      <div style={{ fontSize: 13, color: "var(--ink-soft)", marginBottom: 14 }}>
                        {confirmedAppt.doctorName} · {confirmedAppt.date} at {confirmedAppt.time}
                      </div>
                      <button className="db-btn primary sm" onClick={resetBooking}>
                        View my appointments
                      </button>
                    </div>
                  ) : (
                    <>
                      {bookError && <div className="db-error">{bookError}</div>}
                      <div style={{ fontSize: 13.5, marginBottom: 14 }}>
                        {selectedDoctorObj
                          ? `${selectedDoctorObj.fullName}${
                              selectedSlot ? ` · ${selectedDate} · ${selectedSlot}` : " · select a date and time"
                            }`
                          : "Select a doctor to continue."}
                      </div>
                      <button
                        className="db-btn primary"
                        disabled={!selectedDoctor || !selectedSlot || booking}
                        onClick={handleBook}
                      >
                        {booking ? "Booking…" : "Confirm appointment"}
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
                {apptsError && (
                  <div className="db-error" style={{ padding: "12px 20px" }}>
                    {apptsError}
                  </div>
                )}
                {apptsLoading ? (
                  <div className="db-empty" style={{ padding: 20 }}>
                    Loading…
                  </div>
                ) : appointments.length === 0 ? (
                  <div className="db-empty" style={{ padding: 20 }}>
                    No appointments yet.
                  </div>
                ) : (
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
                          <td style={{ fontFamily: "var(--font-mono)", fontSize: 12.5 }}>{a.reference}</td>
                          <td>{a.doctorName} <span style={{ color: "var(--ink-soft)" }}>· {a.specialization}</span></td>
                          <td>{a.date}</td>
                          <td>{a.time}</td>
                          <td>
                            <span className={`db-badge ${a.status.toLowerCase()}`}>{a.status.toLowerCase()}</span>
                          </td>
                          <td>
                            {a.status === "CONFIRMED" && (
                              <button
                                className="db-btn danger sm"
                                disabled={cancellingId === a.id}
                                onClick={() => cancelAppointment(a.id)}
                              >
                                {cancellingId === a.id ? "Cancelling…" : "Cancel"}
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
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
                {profileError && <div className="db-error">{profileError}</div>}
                {profileLoading ? (
                  <div className="db-empty">Loading…</div>
                ) : (
                  <>
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
                      <input className="db-input" value={profile.email} disabled />
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
                    {saveMessage && (
                      <div style={{ fontSize: 13, color: "var(--ink-soft)", marginBottom: 10 }}>
                        {saveMessage}
                      </div>
                    )}
                    <button className="db-btn primary" disabled={saving} onClick={saveProfile}>
                      {saving ? "Saving…" : "Save changes"}
                    </button>
                  </>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
