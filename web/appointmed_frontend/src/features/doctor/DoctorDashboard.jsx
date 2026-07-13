import { useState, useEffect } from "react";
import "../../shared/styles/Appointmed.css";
import { AvailabilityAPI } from "./api/availabilityApi";
import { DoctorAppointmentsAPI } from "./api/appointmentsApi";
import { DoctorProfileAPI } from "./api/doctorProfileApi";
import { formatTime12h } from "../../shared/utils/format";

/**
 * AppointMed — Doctor Dashboard
 * Covers: FR-014, FR-015, FR-017, FR-018, FR-019 (availability + schedule)
 * and the doctor side of FR-006..FR-013 (viewing/cancelling/completing
 * appointments patients booked with them).
 *
 * Both the availability panel and the appointments table are wired to the
 * backend (/api/doctor/availability, /api/doctor/appointments).
 */

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

const WEEKDAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

function daysArrayToMap(daysArray) {
  const map = {};
  WEEKDAYS.forEach((day) => {
    map[day] = daysArray.includes(day);
  });
  return map;
}

export default function DoctorDashboard({ doctorName = "Dr. Reyes", onLogout }) {
  const [view, setView] = useState("schedule"); // schedule | availability
  const [appointments, setAppointments] = useState([]);
  const [apptsLoading, setApptsLoading] = useState(true);
  const [apptsError, setApptsError] = useState("");
  const [actioningId, setActioningId] = useState(null);
  const [filter, setFilter] = useState("all"); // all | today

  // ---- Availability state (backed by the API) ----
  const [workingDays, setWorkingDays] = useState({
    Mon: true, Tue: true, Wed: true, Thu: true, Fri: true, Sat: false, Sun: false,
  });
  const [hours, setHours] = useState({ start: "09:00", end: "17:00" });
  const [unavailableDates, setUnavailableDates] = useState([]);
  const [newUnavailableDate, setNewUnavailableDate] = useState("");

  const [availLoading, setAvailLoading] = useState(true);
  const [availError, setAvailError] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState("");
  const [dateActionError, setDateActionError] = useState("");

  // ---- Change own password ----
  const [passwordForm, setPasswordForm] = useState({ oldPassword: "", newPassword: "", confirmPassword: "" });
  const [passwordError, setPasswordError] = useState("");
  const [passwordSuccess, setPasswordSuccess] = useState("");
  const [changingPassword, setChangingPassword] = useState(false);

  useEffect(() => {
    loadAvailability();
    loadAppointments();
  }, []);

  async function loadAvailability() {
    setAvailLoading(true);
    setAvailError("");
    try {
      const data = await AvailabilityAPI.get();
      setWorkingDays(daysArrayToMap(data.workingDays || []));
      setHours({ start: data.startTime, end: data.endTime });
      setUnavailableDates(data.unavailableDates || []);
    } catch (err) {
      setAvailError(err.message || "Couldn't load your availability.");
    } finally {
      setAvailLoading(false);
    }
  }

  async function loadAppointments() {
    setApptsLoading(true);
    setApptsError("");
    try {
      const data = await DoctorAppointmentsAPI.list();
      setAppointments(data || []);
    } catch (err) {
      setApptsError(err.message || "Couldn't load your appointments.");
    } finally {
      setApptsLoading(false);
    }
  }

  async function markCompleted(id) {
    setActioningId(id);
    setApptsError("");
    try {
      const updated = await DoctorAppointmentsAPI.complete(id);
      setAppointments((prev) => prev.map((a) => (a.id === id ? updated : a)));
    } catch (err) {
      setApptsError(err.message || "Couldn't mark that appointment complete.");
    } finally {
      setActioningId(null);
    }
  }

  async function cancelAppointment(id) {
    setActioningId(id);
    setApptsError("");
    try {
      const updated = await DoctorAppointmentsAPI.cancel(id);
      setAppointments((prev) => prev.map((a) => (a.id === id ? updated : a)));
    } catch (err) {
      setApptsError(err.message || "Couldn't cancel that appointment.");
    } finally {
      setActioningId(null);
    }
  }

  const today = todayStr();
  const todayCount = appointments.filter((a) => a.date === today && a.status === "CONFIRMED").length;
  const upcomingCount = appointments.filter((a) => a.date >= today && a.status === "CONFIRMED").length;

  const visibleAppointments =
    filter === "today" ? appointments.filter((a) => a.date === today) : appointments;

  function toggleDay(day) {
    setWorkingDays((prev) => ({ ...prev, [day]: !prev[day] }));
    setSaveMessage("");
  }

  async function saveSchedule() {
    const selectedDays = WEEKDAYS.filter((day) => workingDays[day]);

    if (selectedDays.length === 0) {
      setSaveMessage("Select at least one working day.");
      return;
    }
    if (hours.start >= hours.end) {
      setSaveMessage("Start time must be before end time.");
      return;
    }

    setSaving(true);
    setSaveMessage("");
    try {
      const data = await AvailabilityAPI.updateSchedule(selectedDays, hours.start, hours.end);
      setWorkingDays(daysArrayToMap(data.workingDays || []));
      setHours({ start: data.startTime, end: data.endTime });
      setSaveMessage("Schedule saved.");
    } catch (err) {
      setSaveMessage(err.message || "Couldn't save schedule.");
    } finally {
      setSaving(false);
    }
  }

  async function addUnavailableDate() {
    if (!newUnavailableDate) return;
    setDateActionError("");
    try {
      const data = await AvailabilityAPI.addUnavailableDate(newUnavailableDate);
      setUnavailableDates(data.unavailableDates || []);
      setNewUnavailableDate("");
    } catch (err) {
      setDateActionError(err.message || "Couldn't add that date.");
    }
  }

  async function removeUnavailableDate(date) {
    setDateActionError("");
    try {
      const data = await AvailabilityAPI.removeUnavailableDate(date);
      setUnavailableDates(data.unavailableDates || []);
    } catch (err) {
      setDateActionError(err.message || "Couldn't remove that date.");
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
      await DoctorProfileAPI.changePassword(passwordForm);
      setPasswordSuccess("Password updated successfully.");
      setPasswordForm({ oldPassword: "", newPassword: "", confirmPassword: "" });
    } catch (err) {
      setPasswordError(err.message || "Couldn't update your password.");
    } finally {
      setChangingPassword(false);
    }
  }

  return (
    <div className="db-shell">
      {/* ---------- Sidebar ---------- */}
      <div className="db-sidebar">
        <div className="db-brand">
          Appoint<span>Med</span>
        </div>
        <div className="db-role-tag">Doctor portal</div>

        <div className="db-nav">
          <button
            className={`db-nav-item${view === "schedule" ? " is-active" : ""}`}
            onClick={() => setView("schedule")}
          >
            My Schedule
          </button>
          <button
            className={`db-nav-item${view === "availability" ? " is-active" : ""}`}
            onClick={() => setView("availability")}
          >
            Availability
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
            {doctorName.split(" ").map((w) => w[0]).slice(0, 2).join("")}
          </div>
          <div>
            <div className="db-foot-name">{doctorName}</div>
            <div className="db-foot-role">Doctor</div>
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
              {view === "schedule" && "My schedule"}
              {view === "availability" && "Manage availability"}
              {view === "password" && "Change password"}
            </div>
            <div className="db-topbar-sub">
              {view === "schedule" && "Your upcoming and recent appointments."}
              {view === "availability" && "Set your working days, hours, and time off."}
              {view === "password" && "Update the password for your own account."}
            </div>
          </div>
        </div>

        <div className="db-content">
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
          {view === "schedule" && (
            <>
              <div className="db-stats-grid">
                <div className="db-stat-card">
                  <div className="db-stat-value">{apptsLoading ? "…" : todayCount}</div>
                  <div className="db-stat-label">Appointments today</div>
                </div>
                <div className="db-stat-card">
                  <div className="db-stat-value">{apptsLoading ? "…" : upcomingCount}</div>
                  <div className="db-stat-label">Upcoming confirmed</div>
                </div>
                <div className="db-stat-card">
                  <div className="db-stat-value">{apptsLoading ? "…" : appointments.length}</div>
                  <div className="db-stat-label">Total on record</div>
                </div>
              </div>

              <div className="db-panel">
                <div className="db-panel-head">
                  <div className="db-panel-title">Appointments</div>
                  <div style={{ display: "flex", gap: 6 }}>
                    <button
                      className={`db-btn sm ${filter === "all" ? "primary" : "outline"}`}
                      onClick={() => setFilter("all")}
                    >
                      All
                    </button>
                    <button
                      className={`db-btn sm ${filter === "today" ? "primary" : "outline"}`}
                      onClick={() => setFilter("today")}
                    >
                      Today
                    </button>
                  </div>
                </div>
                <div className="db-panel-body no-pad">
                  {apptsError && (
                    <div className="db-error" style={{ margin: "16px 20px 0" }}>
                      {apptsError}
                    </div>
                  )}
                  {apptsLoading ? (
                    <div className="db-empty" style={{ padding: 20 }}>
                      Loading…
                    </div>
                  ) : visibleAppointments.length === 0 ? (
                    <div className="db-empty" style={{ padding: 20 }}>
                      No appointments to show.
                    </div>
                  ) : (
                    <table className="db-table">
                      <thead>
                        <tr>
                          <th>Reference</th>
                          <th>Patient</th>
                          <th>Date</th>
                          <th>Time</th>
                          <th>Status</th>
                          <th></th>
                        </tr>
                      </thead>
                      <tbody>
                        {visibleAppointments.map((a) => (
                          <tr key={a.id}>
                            <td style={{ fontFamily: "var(--font-mono)", fontSize: 12.5 }}>{a.reference}</td>
                            <td>{a.patientName}</td>
                            <td>{a.date}</td>
                            <td>{formatTime12h(a.time)}</td>
                            <td>
                              <span className={`db-badge ${a.status.toLowerCase()}`}>{a.status.toLowerCase()}</span>
                            </td>
                            <td>
                              {a.status === "CONFIRMED" && (
                                <div style={{ display: "flex", gap: 6 }}>
                                  <button
                                    className="db-btn outline sm"
                                    disabled={actioningId === a.id}
                                    onClick={() => markCompleted(a.id)}
                                  >
                                    {actioningId === a.id ? "…" : "Mark completed"}
                                  </button>
                                  <button
                                    className="db-btn danger sm"
                                    disabled={actioningId === a.id}
                                    onClick={() => cancelAppointment(a.id)}
                                  >
                                    {actioningId === a.id ? "…" : "Cancel"}
                                  </button>
                                </div>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              </div>
            </>
          )}

          {/* ---------- AVAILABILITY ---------- */}
          {view === "availability" && (
            <div className="db-two-col">
              <div className="db-panel">
                <div className="db-panel-head">
                  <div className="db-panel-title">Working days &amp; hours</div>
                </div>
                <div className="db-panel-body">
                  {availLoading ? (
                    <div className="db-empty">Loading your schedule…</div>
                  ) : availError ? (
                    <div className="db-empty" style={{ color: "var(--danger, #c0392b)" }}>
                      {availError}
                    </div>
                  ) : (
                    <>
                      {WEEKDAYS.map((day) => (
                        <div className="db-checkbox-row" key={day}>
                          <input
                            type="checkbox"
                            checked={!!workingDays[day]}
                            onChange={() => toggleDay(day)}
                          />
                          <span style={{ width: 46 }}>{day}</span>
                          {workingDays[day] && (
                            <span style={{ fontSize: 12.5, color: "var(--ink-soft)" }}>
                              {formatTime12h(hours.start)} – {formatTime12h(hours.end)}
                            </span>
                          )}
                        </div>
                      ))}

                      <div style={{ display: "flex", gap: 12, marginTop: 16 }}>
                        <div className="db-field" style={{ marginBottom: 0 }}>
                          <label className="db-label">Start time</label>
                          <input
                            type="time"
                            className="db-input"
                            value={hours.start}
                            onChange={(e) => setHours({ ...hours, start: e.target.value })}
                          />
                        </div>
                        <div className="db-field" style={{ marginBottom: 0 }}>
                          <label className="db-label">End time</label>
                          <input
                            type="time"
                            className="db-input"
                            value={hours.end}
                            onChange={(e) => setHours({ ...hours, end: e.target.value })}
                          />
                        </div>
                      </div>

                      <button
                        className="db-btn primary"
                        style={{ marginTop: 18 }}
                        onClick={saveSchedule}
                        disabled={saving}
                      >
                        {saving ? "Saving…" : "Save schedule"}
                      </button>

                      {saveMessage && (
                        <div style={{ marginTop: 10, fontSize: 12.5, color: "var(--ink-soft)" }}>
                          {saveMessage}
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>

              <div className="db-panel">
                <div className="db-panel-head">
                  <div className="db-panel-title">Mark dates unavailable</div>
                </div>
                <div className="db-panel-body">
                  <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
                    <input
                      type="date"
                      className="db-input"
                      value={newUnavailableDate}
                      onChange={(e) => setNewUnavailableDate(e.target.value)}
                    />
                    <button className="db-btn outline sm" onClick={addUnavailableDate}>
                      Add
                    </button>
                  </div>

                  {dateActionError && (
                    <div style={{ marginBottom: 12, fontSize: 12.5, color: "var(--danger, #c0392b)" }}>
                      {dateActionError}
                    </div>
                  )}

                  {availLoading ? (
                    <div className="db-empty">Loading…</div>
                  ) : unavailableDates.length === 0 ? (
                    <div className="db-empty">No dates marked off.</div>
                  ) : (
                    unavailableDates.map((date) => (
                      <div className="db-row" key={date}>
                        <div className="db-row-main">
                          <div className="db-row-title">{date}</div>
                          <div className="db-row-sub">Bookings blocked for this date</div>
                        </div>
                        <button
                          className="db-btn danger sm"
                          onClick={() => removeUnavailableDate(date)}
                        >
                          Remove
                        </button>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
