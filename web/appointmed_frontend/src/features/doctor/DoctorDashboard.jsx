import { useState, useEffect } from "react";
import "../../shared/styles/Appointmed.css";
import { AvailabilityAPI } from "./api/availabilityApi";

/**
 * AppointMed — Doctor Dashboard
 * Covers: FR-014, FR-015, FR-017, FR-018, FR-019 (availability + schedule)
 *
 * Availability panel is wired to the backend (/api/doctor/availability).
 * The appointments table below is still local mock data — confirm/decline
 * wiring is a separate task.
 */

const INITIAL_APPOINTMENTS = [
  { id: "APT-102938", patient: "Juan Dela Cruz", date: "2026-07-10", time: "09:00", status: "confirmed" },
  { id: "APT-103012", patient: "Maria Santos", date: "2026-07-10", time: "09:30", status: "pending" },
  { id: "APT-102890", patient: "Pedro Reyes", date: "2026-07-11", time: "10:00", status: "confirmed" },
  { id: "APT-101877", patient: "Ana Lim", date: "2026-06-30", time: "10:30", status: "cancelled" },
];

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
  const [appointments, setAppointments] = useState(INITIAL_APPOINTMENTS);
  const [filter, setFilter] = useState("all"); // all | today | week

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

  useEffect(() => {
    loadAvailability();
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

  const todayCount = appointments.filter((a) => a.date === "2026-07-10" && a.status !== "cancelled").length;
  const pendingCount = appointments.filter((a) => a.status === "pending").length;

  const visibleAppointments =
    filter === "today"
      ? appointments.filter((a) => a.date === "2026-07-10")
      : appointments;

  function updateStatus(id, status) {
    setAppointments((prev) => prev.map((a) => (a.id === id ? { ...a, status } : a)));
  }

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
              {view === "schedule" ? "My schedule" : "Manage availability"}
            </div>
            <div className="db-topbar-sub">
              {view === "schedule"
                ? "Your upcoming and recent appointments."
                : "Set your working days, hours, and time off."}
            </div>
          </div>
        </div>

        <div className="db-content">
          {/* ---------- SCHEDULE ---------- */}
          {view === "schedule" && (
            <>
              <div className="db-stats-grid">
                <div className="db-stat-card">
                  <div className="db-stat-value">{todayCount}</div>
                  <div className="db-stat-label">Appointments today</div>
                </div>
                <div className="db-stat-card">
                  <div className="db-stat-value">{pendingCount}</div>
                  <div className="db-stat-label">Pending confirmation</div>
                </div>
                <div className="db-stat-card">
                  <div className="db-stat-value">{appointments.length}</div>
                  <div className="db-stat-label">Total this month</div>
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
                          <td style={{ fontFamily: "var(--font-mono)", fontSize: 12.5 }}>{a.id}</td>
                          <td>{a.patient}</td>
                          <td>{a.date}</td>
                          <td>{a.time}</td>
                          <td>
                            <span className={`db-badge ${a.status}`}>{a.status}</span>
                          </td>
                          <td>
                            {a.status === "pending" && (
                              <div style={{ display: "flex", gap: 6 }}>
                                <button
                                  className="db-btn outline sm"
                                  onClick={() => updateStatus(a.id, "confirmed")}
                                >
                                  Confirm
                                </button>
                                <button
                                  className="db-btn danger sm"
                                  onClick={() => updateStatus(a.id, "cancelled")}
                                >
                                  Decline
                                </button>
                              </div>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
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
                              {hours.start} – {hours.end}
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
