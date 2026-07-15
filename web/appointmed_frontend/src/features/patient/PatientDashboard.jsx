import { useState, useEffect, useCallback } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "../../shared/styles/Appointmed.css";
import { DoctorsAPI, AppointmentsAPI, PatientProfileAPI } from "./api/patientApi";
import { formatTime12h } from "../../shared/utils/format";

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
  const location = useLocation();
const navigate = useNavigate();

const PATH_TO_VIEW = {
  "/patient": "home",
  "/patient/book": "book",
  "/patient/appointments": "history",
  "/patient/profile": "profile",
  "/patient/password": "password",
};
const VIEW_TO_PATH = {
  home: "/patient",
  book: "/patient/book",
  history: "/patient/appointments",
  profile: "/patient/profile",
  password: "/patient/password",
};

const view = PATH_TO_VIEW[location.pathname] || "home";

function setView(next) {
  navigate(VIEW_TO_PATH[next] || "/patient");
}

  // ---- Doctors ----
  const [doctors, setDoctors] = useState([]);
  const [doctorsLoading, setDoctorsLoading] = useState(true);
  const [doctorsError, setDoctorsError] = useState("");

  // ---- Appointments ----
  const [appointments, setAppointments] = useState([]);
  const [apptsLoading, setApptsLoading] = useState(true);
  const [apptsError, setApptsError] = useState("");
  const [cancellingId, setCancellingId] = useState(null);

  // ---- Appointment history search/filter (FR-012) ----
  const [historyKeyword, setHistoryKeyword] = useState("");
  const [historyStatus, setHistoryStatus] = useState(""); // "" = All

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

  // ---- Reschedule flow (FR-011) ----
  const [reschedulingAppt, setReschedulingAppt] = useState(null); // the appointment being moved, or null
  const [rescheduleDate, setRescheduleDate] = useState("");
  const [rescheduleSlots, setRescheduleSlots] = useState([]);
  const [rescheduleSlotsLoading, setRescheduleSlotsLoading] = useState(false);
  const [rescheduleSlotsError, setRescheduleSlotsError] = useState("");
  const [rescheduleSlot, setRescheduleSlot] = useState(null);
  const [rescheduleSaving, setRescheduleSaving] = useState(false);
  const [rescheduleError, setRescheduleError] = useState("");

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

  // ---- Change own password ----
  const [passwordForm, setPasswordForm] = useState({ oldPassword: "", newPassword: "", confirmPassword: "" });
  const [passwordError, setPasswordError] = useState("");
  const [passwordSuccess, setPasswordSuccess] = useState("");
  const [changingPassword, setChangingPassword] = useState(false);

  useEffect(() => {
    loadDoctors();
    loadAppointments();
    loadProfile();
    // eslint-disable-next-line react-hooks/exhaustive-deps
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

  /** FR-012: re-issues the list call with whatever search/filter state is current. */
  async function loadAppointments(overrides = {}) {
    setApptsLoading(true);
    setApptsError("");
    try {
      const data = await AppointmentsAPI.list({
        status: overrides.status !== undefined ? overrides.status : historyStatus,
        keyword: overrides.keyword !== undefined ? overrides.keyword : historyKeyword,
      });
      setAppointments(data || []);
    } catch (err) {
      setApptsError(err.message || "Couldn't load your appointments.");
    } finally {
      setApptsLoading(false);
    }
  }

  function applyStatusFilter(status) {
    setHistoryStatus(status);
    loadAppointments({ status });
  }

  function submitSearch(e) {
    e.preventDefault();
    loadAppointments({ keyword: historyKeyword });
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

  /** FR-011: opens the reschedule modal for a given appointment, preloaded to its current date. */
  function openReschedule(appt) {
    setReschedulingAppt(appt);
    setRescheduleDate(appt.date);
    setRescheduleSlot(null);
    setRescheduleError("");
    loadRescheduleSlots(appt.doctorId, appt.date);
  }

  function closeReschedule() {
    setReschedulingAppt(null);
    setRescheduleSlots([]);
    setRescheduleSlot(null);
    setRescheduleError("");
  }

  async function loadRescheduleSlots(doctorId, date) {
    setRescheduleSlotsLoading(true);
    setRescheduleSlotsError("");
    setRescheduleSlots([]);
    setRescheduleSlot(null);
    try {
      const data = await DoctorsAPI.getSlots(doctorId, date);
      setRescheduleSlots(data || []);
    } catch (err) {
      setRescheduleSlotsError(err.message || "Couldn't load open slots for that day.");
    } finally {
      setRescheduleSlotsLoading(false);
    }
  }

  function changeRescheduleDate(date) {
    setRescheduleDate(date);
    if (reschedulingAppt) loadRescheduleSlots(reschedulingAppt.doctorId, date);
  }

  async function confirmReschedule() {
    if (!reschedulingAppt || !rescheduleSlot) return;
    setRescheduleSaving(true);
    setRescheduleError("");
    try {
      const updated = await AppointmentsAPI.reschedule(reschedulingAppt.id, rescheduleDate, rescheduleSlot);
      setAppointments((prev) => prev.map((a) => (a.id === updated.id ? updated : a)));
      closeReschedule();
    } catch (err) {
      setRescheduleError(err.message || "Couldn't reschedule that appointment.");
    } finally {
      setRescheduleSaving(false);
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
        profile.dateOfBirth,
        profile.medicalHistory
      );
      setProfile((p) => ({
        ...p,
        fullName: updated.fullName,
        contact: updated.contactNumber,
        dateOfBirth: updated.dateOfBirth || "",
      }));
      setSaveMessage("Saved.");
    } catch (err) {
      setProfileError(err.message || "Couldn't save your profile.");
    } finally {
      setSaving(false);
    }
  }

  async function submitPasswordChange(ev) {
    ev.preventDefault();
    setPasswordError("");
    setPasswordSuccess("");

    if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
      setPasswordError("Fill in all fields.");
      return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError("New password and confirmation don't match.");
      return;
    }

    setChangingPassword(true);
    try {
      await PatientProfileAPI.changePassword(passwordForm);
      setPasswordSuccess("Password updated successfully.");
      setPasswordForm({ oldPassword: "", newPassword: "", confirmPassword: "" });
    } catch (err) {
      setPasswordError(err.message || "Couldn't update your password.");
    } finally {
      setChangingPassword(false);
    }
  }

  // Feature 1: the "Next appointment(s)" panel shows every upcoming CONFIRMED
  // appointment (not just the nearest one), ordered by schedule date/time —
  // never by booking/creation order or appointment id. Cancelled and
  // completed appointments are excluded, and so is anything whose date has
  // already passed.
  const todayIso = todayStr();
  const upcomingSorted = appointments
    .filter((a) => a.status === "CONFIRMED" && a.date >= todayIso)
    .slice()
    .sort((a, b) => (a.date === b.date ? a.time.localeCompare(b.time) : a.date.localeCompare(b.date)));
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
          <button
            className={`db-nav-item${view === "password" ? " is-active" : ""}`}
            onClick={() => setView("password")}
          >
            Change Password
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
              {view === "password" && "Change password"}
            </div>
            <div className="db-topbar-sub">
              {view === "home" && "Here's what's coming up."}
              {view === "book" && "Pick a doctor, then a date and time."}
              {view === "history" && "Everything you've booked, past and upcoming."}
              {view === "profile" && "Keep your details up to date."}
              {view === "password" && "Update the password for your own account."}
            </div>
          </div>
        </div>

        <div className="db-content">
          {/* ---------- HOME ---------- */}
          {view === "home" && (
            <>
              <div className="db-stats-grid">
                <div className="db-stat-card">
                  <div className="db-stat-value">{apptsLoading ? "…" : upcomingSorted.length}</div>
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
                  <div className="db-panel-title">Next appointment{upcomingSorted.length > 1 ? "s" : ""}</div>
                  <button className="db-btn outline sm" onClick={() => setView("book")}>
                    Book another
                  </button>
                </div>
                <div className="db-panel-body">
                  {apptsError && <div className="db-error">{apptsError}</div>}
                  {apptsLoading ? (
                    <div className="db-empty">Loading…</div>
                  ) : upcomingSorted.length === 0 ? (
                    <div className="db-empty">You have no upcoming appointments.</div>
                  ) : (
                    upcomingSorted.map((appt) => (
                      <div className="db-row" key={appt.id}>
                        <div className="db-row-time">{formatTime12h(appt.time)}</div>
                        <div className="db-row-avatar">
                          {appt.doctorName?.trim().split(" ").pop()?.[0] || "D"}
                        </div>
                        <div className="db-row-main">
                          <div className="db-row-title">{appt.doctorName} · {appt.specialization}</div>
                          <div className="db-row-sub">{appt.date} · Ref {appt.reference}</div>
                        </div>
                        {appt.needsReschedule ? (
                          <span
                            className="db-badge"
                            style={{ background: "rgba(214,161,59,.16)", color: "var(--amber)" }}
                            title="Your doctor became unavailable on this date — please reschedule."
                          >
                            Needs reschedule
                          </span>
                        ) : (
                          <span className="db-badge confirmed">Confirmed</span>
                        )}
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
                              {formatTime12h(slot.time)}
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
                        {confirmedAppt.doctorName} · {confirmedAppt.date} at {formatTime12h(confirmedAppt.time)}
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
                              selectedSlot ? ` · ${selectedDate} · ${formatTime12h(selectedSlot)}` : " · select a date and time"
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

              {/* FR-012: searchable history — free-text keyword + status filter, both sent to the backend */}
              <form className="db-search-row" onSubmit={submitSearch}>
                <input
                  type="text"
                  className="db-input"
                  placeholder="Search by doctor, specialization, or reference no."
                  value={historyKeyword}
                  onChange={(e) => setHistoryKeyword(e.target.value)}
                />
              </form>
              <div className="db-filter-row">
                {[
                  { label: "All", value: "" },
                  { label: "Confirmed", value: "CONFIRMED" },
                  { label: "Cancelled", value: "CANCELLED" },
                  { label: "Completed", value: "COMPLETED" },
                ].map((f) => (
                  <button
                    key={f.value}
                    type="button"
                    className={`db-filter-btn${historyStatus === f.value ? " is-active" : ""}`}
                    onClick={() => applyStatusFilter(f.value)}
                  >
                    {f.label}
                  </button>
                ))}
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
                    {historyStatus || historyKeyword ? "No appointments match your search." : "No appointments yet."}
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
                        <tr key={a.id} style={a.needsReschedule ? { backgroundColor: "rgba(214,161,59,.08)" } : undefined}>
                          <td style={{ fontFamily: "var(--font-mono)", fontSize: 12.5 }}>{a.reference}</td>
                          <td>{a.doctorName} <span style={{ color: "var(--ink-soft)" }}>· {a.specialization}</span></td>
                          <td>{a.date}</td>
                          <td>{formatTime12h(a.time)}</td>
                          <td>
                            <span className={`db-badge ${a.status.toLowerCase()}`}>{a.status.toLowerCase()}</span>
                            {a.needsReschedule && (
                              <span
                                className="db-badge"
                                style={{ marginLeft: 6, background: "rgba(214,161,59,.16)", color: "var(--amber)" }}
                                title="Your doctor became unavailable on this date — please reschedule."
                              >
                                needs reschedule
                              </span>
                            )}
                          </td>
                          <td style={{ whiteSpace: "nowrap" }}>
                            {a.status === "CONFIRMED" && (
                              <>
                                <button
                                  className={a.needsReschedule ? "db-btn primary sm" : "db-btn outline sm"}
                                  style={{ marginRight: 6 }}
                                  onClick={() => openReschedule(a)}
                                >
                                  Reschedule
                                </button>
                                <button
                                  className="db-btn danger sm"
                                  disabled={cancellingId === a.id}
                                  onClick={() => cancelAppointment(a.id)}
                                >
                                  {cancellingId === a.id ? "Cancelling…" : "Cancel"}
                                </button>
                              </>
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
                      <label className="db-label">Date of birth</label>
                      <input
                        type="date"
                        className="db-input"
                        style={{ maxWidth: 220 }}
                        value={profile.dateOfBirth || ""}
                        max={todayStr()}
                        onChange={(e) => setProfile({ ...profile, dateOfBirth: e.target.value })}
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

          {/* ---------- CHANGE PASSWORD ---------- */}
          {view === "password" && (
            <div className="db-panel" style={{ maxWidth: 480 }}>
              <div className="db-panel-head">
                <div className="db-panel-title">Change your password</div>
              </div>
              <div className="db-panel-body">
                <form onSubmit={submitPasswordChange}>
                  <div className="db-field">
                    <label className="db-label">Current password</label>
                    <input
                      className="db-input"
                      type="password"
                      value={passwordForm.oldPassword}
                      onChange={(e) => setPasswordForm({ ...passwordForm, oldPassword: e.target.value })}
                      required
                    />
                  </div>
                  <div className="db-field">
                    <label className="db-label">New password</label>
                    <input
                      className="db-input"
                      type="password"
                      value={passwordForm.newPassword}
                      onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                      required
                    />
                  </div>
                  <div className="db-field">
                    <label className="db-label">Confirm new password</label>
                    <input
                      className="db-input"
                      type="password"
                      value={passwordForm.confirmPassword}
                      onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
                      required
                    />
                  </div>

                  {passwordError && (
                    <div style={{ color: "var(--alert)", fontSize: 13, marginBottom: 12 }}>{passwordError}</div>
                  )}
                  {passwordSuccess && (
                    <div style={{ color: "var(--green)", fontSize: 13, marginBottom: 12 }}>{passwordSuccess}</div>
                  )}

                  <button className="db-btn primary" type="submit" disabled={changingPassword}>
                    {changingPassword ? "Updating…" : "Update password"}
                  </button>
                </form>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ---------- RESCHEDULE MODAL (FR-011) ---------- */}
      {reschedulingAppt && (
        <div className="db-modal-overlay" onClick={closeReschedule}>
          <div className="db-modal" onClick={(e) => e.stopPropagation()}>
            <div className="db-modal-title">Reschedule appointment</div>
            <div className="db-modal-sub">
              Currently: {reschedulingAppt.doctorName} · {reschedulingAppt.date} · {formatTime12h(reschedulingAppt.time)}
            </div>

            <div className="db-field">
              <label className="db-label">New date</label>
              <input
                type="date"
                className="db-input"
                value={rescheduleDate}
                min={todayStr()}
                onChange={(e) => changeRescheduleDate(e.target.value)}
              />
            </div>

            <div className="db-label" style={{ margin: "14px 0 10px" }}>
              New time
            </div>
            {rescheduleSlotsError && <div className="db-error">{rescheduleSlotsError}</div>}
            {rescheduleSlotsLoading ? (
              <div className="db-empty">Loading slots…</div>
            ) : rescheduleSlots.length === 0 ? (
              !rescheduleSlotsError && (
                <div className="db-empty">
                  This doctor has no open hours on that day. Try another date.
                </div>
              )
            ) : (
              <div className="db-slot-grid">
                {rescheduleSlots.map((slot) => (
                  <button
                    key={slot.time}
                    disabled={slot.reserved}
                    className={`db-slot-btn${slot.reserved ? " is-reserved" : ""}${
                      rescheduleSlot === slot.time ? " is-selected" : ""
                    }`}
                    onClick={() => setRescheduleSlot(slot.time)}
                  >
                    {formatTime12h(slot.time)}
                  </button>
                ))}
              </div>
            )}

            {rescheduleError && (
              <div className="db-error" style={{ marginTop: 14 }}>
                {rescheduleError}
              </div>
            )}

            <div className="db-modal-actions">
              <button className="db-btn outline" onClick={closeReschedule}>
                Close
              </button>
              <button
                className="db-btn primary"
                disabled={!rescheduleSlot || rescheduleSaving}
                onClick={confirmReschedule}
              >
                {rescheduleSaving ? "Moving…" : "Confirm move"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}