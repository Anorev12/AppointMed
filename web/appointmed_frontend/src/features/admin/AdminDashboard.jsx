import { useState, useEffect, useCallback } from "react";
import "../../shared/styles/Appointmed.css";
import { AdminAPI } from "./api/adminApi";

/**
 * AppointMed — Admin Dashboard
 * Covers: FR-028, FR-030, FR-032, FR-034, FR-035 (overview, user mgmt, reports)
 * and FR-016 (override any appointment).
 *
 * Fully wired to the backend:
 *  - GET  /api/admin/patients                    (patient roster)
 *  - GET  /api/admin/patients/{id}/appointments   (per-patient history)
 *  - GET  /api/admin/doctors                      (doctor roster + status)
 *  - PUT  /api/admin/doctors/{id}/status          (mark active / on leave)
 *  - POST /api/admin/doctors                      (create doctor account)
 *  - GET  /api/admin/appointments                 (clinic-wide appointments)
 *  - PUT  /api/admin/appointments/{id}/cancel     (override cancel)
 */

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

export default function AdminDashboard({ adminName = "Admin", onLogout }) {
  const [view, setView] = useState("overview"); // overview | patients | doctors | appointments
  const [search, setSearch] = useState("");

  // ---- Patients ----
  const [patients, setPatients] = useState([]);
  const [patientsLoading, setPatientsLoading] = useState(true);
  const [patientsError, setPatientsError] = useState("");

  // ---- Doctors ----
  const [doctors, setDoctors] = useState([]);
  const [doctorsLoading, setDoctorsLoading] = useState(true);
  const [doctorsError, setDoctorsError] = useState("");
  const [statusUpdatingId, setStatusUpdatingId] = useState(null);

  // ---- Appointments ----
  const [appointments, setAppointments] = useState([]);
  const [apptsLoading, setApptsLoading] = useState(true);
  const [apptsError, setApptsError] = useState("");
  const [cancellingId, setCancellingId] = useState(null);

  // ---- Add doctor form ----
  const [showAddDoctor, setShowAddDoctor] = useState(false);
  const [newDoctor, setNewDoctor] = useState({ fullName: "", email: "", password: "", specialization: "" });
  const [addDoctorError, setAddDoctorError] = useState("");
  const [addingDoctor, setAddingDoctor] = useState(false);

  // ---- View history modal ----
  const [historyPatient, setHistoryPatient] = useState(null); // { id, fullName } | null
  const [historyAppts, setHistoryAppts] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState("");

  const loadPatients = useCallback(async () => {
    setPatientsLoading(true);
    setPatientsError("");
    try {
      const data = await AdminAPI.listPatients();
      setPatients(data);
    } catch (err) {
      setPatientsError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't load patients.");
    } finally {
      setPatientsLoading(false);
    }
  }, []);

  const loadDoctors = useCallback(async () => {
    setDoctorsLoading(true);
    setDoctorsError("");
    try {
      const data = await AdminAPI.listDoctors();
      setDoctors(data);
    } catch (err) {
      setDoctorsError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't load doctors.");
    } finally {
      setDoctorsLoading(false);
    }
  }, []);

  const loadAppointments = useCallback(async () => {
    setApptsLoading(true);
    setApptsError("");
    try {
      const data = await AdminAPI.listAppointments();
      setAppointments(data);
    } catch (err) {
      setApptsError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't load appointments.");
    } finally {
      setApptsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPatients();
    loadDoctors();
    loadAppointments();
  }, [loadPatients, loadDoctors, loadAppointments]);

  const todayCount = appointments.filter((a) => a.date === todayStr() && a.status !== "CANCELLED").length;
  const completedCount = appointments.filter((a) => a.status === "COMPLETED").length;

  const filteredPatients = patients.filter((p) =>
    p.fullName.toLowerCase().includes(search.toLowerCase())
  );
  const filteredDoctors = doctors.filter((d) =>
    d.fullName.toLowerCase().includes(search.toLowerCase())
  );
  const filteredAppointments = appointments.filter(
    (a) =>
      a.patientName.toLowerCase().includes(search.toLowerCase()) ||
      a.doctorName.toLowerCase().includes(search.toLowerCase()) ||
      a.reference.toLowerCase().includes(search.toLowerCase())
  );

  async function toggleDoctorStatus(doctor) {
    const nextStatus = doctor.status === "ACTIVE" ? "ON_LEAVE" : "ACTIVE";
    setStatusUpdatingId(doctor.id);
    setDoctorsError("");
    try {
      const updated = await AdminAPI.setDoctorStatus(doctor.id, nextStatus);
      setDoctors((prev) => prev.map((d) => (d.id === updated.id ? updated : d)));
    } catch (err) {
      setDoctorsError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't update doctor status.");
    } finally {
      setStatusUpdatingId(null);
    }
  }

  async function createDoctor(ev) {
    ev.preventDefault();
    setAddDoctorError("");

    if (!newDoctor.email.toLowerCase().endsWith("@appointmeddoctor.com")) {
      setAddDoctorError("Doctor email must end in @appointmeddoctor.com");
      return;
    }

    setAddingDoctor(true);
    try {
      const data = await AdminAPI.createDoctor(newDoctor);
      setDoctors((prev) => [...prev, data]);
      setNewDoctor({ fullName: "", email: "", password: "", specialization: "" });
      setShowAddDoctor(false);
    } catch (err) {
      if (err.status === undefined) {
        setAddDoctorError("Can't reach the server. Check that it's running and try again.");
      } else {
        setAddDoctorError(err.data?.message || err.message || "Couldn't create doctor account.");
      }
    } finally {
      setAddingDoctor(false);
    }
  }

  async function overrideCancel(id) {
    setCancellingId(id);
    setApptsError("");
    try {
      const updated = await AdminAPI.overrideCancel(id);
      setAppointments((prev) => prev.map((a) => (a.id === id ? updated : a)));
    } catch (err) {
      setApptsError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't cancel that appointment.");
    } finally {
      setCancellingId(null);
    }
  }

  async function openHistory(patient) {
    setHistoryPatient(patient);
    setHistoryLoading(true);
    setHistoryError("");
    setHistoryAppts([]);
    try {
      const data = await AdminAPI.patientHistory(patient.id);
      setHistoryAppts(data);
    } catch (err) {
      setHistoryError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't load that patient's history.");
    } finally {
      setHistoryLoading(false);
    }
  }

  function closeHistory() {
    setHistoryPatient(null);
    setHistoryAppts([]);
    setHistoryError("");
  }

  return (
    <div className="db-shell">
      {/* ---------- Sidebar ---------- */}
      <div className="db-sidebar">
        <div className="db-brand">
          Appoint<span>Med</span>
        </div>
        <div className="db-role-tag">Admin portal</div>

        <div className="db-nav">
          <button
            className={`db-nav-item${view === "overview" ? " is-active" : ""}`}
            onClick={() => setView("overview")}
          >
            Overview
          </button>
          <button
            className={`db-nav-item${view === "appointments" ? " is-active" : ""}`}
            onClick={() => setView("appointments")}
          >
            Appointments
          </button>
          <button
            className={`db-nav-item${view === "patients" ? " is-active" : ""}`}
            onClick={() => setView("patients")}
          >
            Patients
          </button>
          <button
            className={`db-nav-item${view === "doctors" ? " is-active" : ""}`}
            onClick={() => setView("doctors")}
          >
            Doctors
          </button>
        </div>

        <div className="db-sidebar-foot">
          <div className="db-avatar">
            {adminName.split(" ").map((w) => w[0]).slice(0, 2).join("")}
          </div>
          <div>
            <div className="db-foot-name">{adminName}</div>
            <div className="db-foot-role">Administrator</div>
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
              {view === "overview" && "Clinic overview"}
              {view === "appointments" && "All appointments"}
              {view === "patients" && "Patients"}
              {view === "doctors" && "Doctors"}
            </div>
            <div className="db-topbar-sub">
              {view === "overview" && "Today's snapshot across the clinic."}
              {view === "appointments" && "Manage and override any appointment."}
              {view === "patients" && "Registered patient accounts."}
              {view === "doctors" && "Manage doctor profiles and availability."}
            </div>
          </div>

          {view !== "overview" && (
            <div className="db-search">
              <span>⌕</span>
              <input
                placeholder={`Search ${view}...`}
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          )}
        </div>

        <div className="db-content">
          {/* ---------- OVERVIEW ---------- */}
          {view === "overview" && (
            <>
              <div className="db-stats-grid">
                <div className="db-stat-card">
                  <div className="db-stat-value">{patientsLoading ? "…" : patients.length}</div>
                  <div className="db-stat-label">Registered patients</div>
                </div>
                <div className="db-stat-card">
                  <div className="db-stat-value">
                    {doctorsLoading ? "…" : doctors.filter((d) => d.status === "ACTIVE").length}
                  </div>
                  <div className="db-stat-label">Active doctors</div>
                </div>
                <div className="db-stat-card">
                  <div className="db-stat-value">{apptsLoading ? "…" : todayCount}</div>
                  <div className="db-stat-label">Appointments today</div>
                </div>
                <div className="db-stat-card">
                  <div className="db-stat-value">{apptsLoading ? "…" : completedCount}</div>
                  <div className="db-stat-label">Completed appointments</div>
                </div>
              </div>

              <div className="db-panel">
                <div className="db-panel-head">
                  <div className="db-panel-title">Recent appointments</div>
                  <button className="db-btn outline sm" onClick={() => setView("appointments")}>
                    View all
                  </button>
                </div>
                <div className="db-panel-body no-pad">
                  {apptsLoading ? (
                    <div className="db-empty">Loading…</div>
                  ) : apptsError ? (
                    <div className="db-empty">{apptsError}</div>
                  ) : appointments.length === 0 ? (
                    <div className="db-empty">No appointments yet.</div>
                  ) : (
                    <table className="db-table">
                      <thead>
                        <tr>
                          <th>Reference</th>
                          <th>Patient</th>
                          <th>Doctor</th>
                          <th>Date</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {appointments.slice(0, 4).map((a) => (
                          <tr key={a.id}>
                            <td style={{ fontFamily: "var(--font-mono)", fontSize: 12.5 }}>{a.reference}</td>
                            <td>{a.patientName}</td>
                            <td>{a.doctorName}</td>
                            <td>{a.date} · {a.time}</td>
                            <td>
                              <span className={`db-badge ${a.status.toLowerCase()}`}>{a.status.toLowerCase()}</span>
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

          {/* ---------- APPOINTMENTS ---------- */}
          {view === "appointments" && (
            <div className="db-panel">
              {apptsError && (
                <div style={{ color: "var(--alert)", fontSize: 13, padding: "12px 20px 0" }}>{apptsError}</div>
              )}
              <div className="db-panel-body no-pad">
                {apptsLoading ? (
                  <div className="db-empty">Loading appointments…</div>
                ) : filteredAppointments.length === 0 ? (
                  <div className="db-empty">No appointments found.</div>
                ) : (
                  <table className="db-table">
                    <thead>
                      <tr>
                        <th>Reference</th>
                        <th>Patient</th>
                        <th>Doctor</th>
                        <th>Date</th>
                        <th>Time</th>
                        <th>Status</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredAppointments.map((a) => (
                        <tr key={a.id}>
                          <td style={{ fontFamily: "var(--font-mono)", fontSize: 12.5 }}>{a.reference}</td>
                          <td>{a.patientName}</td>
                          <td>{a.doctorName}</td>
                          <td>{a.date}</td>
                          <td>{a.time}</td>
                          <td>
                            <span className={`db-badge ${a.status.toLowerCase()}`}>{a.status.toLowerCase()}</span>
                          </td>
                          <td>
                            {a.status !== "CANCELLED" && (
                              <button
                                className="db-btn danger sm"
                                disabled={cancellingId === a.id}
                                onClick={() => overrideCancel(a.id)}
                              >
                                {cancellingId === a.id ? "Cancelling…" : "Override cancel"}
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

          {/* ---------- PATIENTS ---------- */}
          {view === "patients" && (
            <div className="db-panel">
              {patientsError && (
                <div style={{ color: "var(--alert)", fontSize: 13, padding: "12px 20px 0" }}>{patientsError}</div>
              )}
              <div className="db-panel-body no-pad">
                {patientsLoading ? (
                  <div className="db-empty">Loading patients…</div>
                ) : filteredPatients.length === 0 ? (
                  <div className="db-empty">No patients found.</div>
                ) : (
                  <table className="db-table">
                    <thead>
                      <tr>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Contact</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredPatients.map((p) => (
                        <tr key={p.id}>
                          <td>{p.fullName}</td>
                          <td>{p.email}</td>
                          <td>{p.contactNumber || "—"}</td>
                          <td>
                            <button className="db-btn outline sm" onClick={() => openHistory(p)}>
                              View history
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          )}

          {/* ---------- DOCTORS ---------- */}
          {view === "doctors" && (
            <>
              {showAddDoctor && (
                <div className="db-panel" style={{ maxWidth: 480 }}>
                  <div className="db-panel-head">
                    <div className="db-panel-title">Add doctor account</div>
                  </div>
                  <div className="db-panel-body">
                    <form onSubmit={createDoctor}>
                      <div className="db-field">
                        <label className="db-label">Full name</label>
                        <input
                          className="db-input"
                          value={newDoctor.fullName}
                          onChange={(e) => setNewDoctor({ ...newDoctor, fullName: e.target.value })}
                          required
                        />
                      </div>
                      <div className="db-field">
                        <label className="db-label">Email (must end in @appointmeddoctor.com)</label>
                        <input
                          className="db-input"
                          type="email"
                          placeholder="dr.name@appointmeddoctor.com"
                          value={newDoctor.email}
                          onChange={(e) => setNewDoctor({ ...newDoctor, email: e.target.value })}
                          required
                        />
                      </div>
                      <div className="db-field">
                        <label className="db-label">Temporary password</label>
                        <input
                          className="db-input"
                          type="password"
                          value={newDoctor.password}
                          onChange={(e) => setNewDoctor({ ...newDoctor, password: e.target.value })}
                          required
                        />
                      </div>
                      <div className="db-field">
                        <label className="db-label">Specialization</label>
                        <input
                          className="db-input"
                          value={newDoctor.specialization}
                          onChange={(e) => setNewDoctor({ ...newDoctor, specialization: e.target.value })}
                          required
                        />
                      </div>

                      {addDoctorError && (
                        <div style={{ color: "var(--alert)", fontSize: 13, marginBottom: 12 }}>
                          {addDoctorError}
                        </div>
                      )}

                      <div style={{ display: "flex", gap: 8 }}>
                        <button className="db-btn primary" type="submit" disabled={addingDoctor}>
                          {addingDoctor ? "Creating…" : "Create doctor account"}
                        </button>
                        <button
                          type="button"
                          className="db-btn outline"
                          onClick={() => {
                            setShowAddDoctor(false);
                            setAddDoctorError("");
                          }}
                        >
                          Cancel
                        </button>
                      </div>
                    </form>
                  </div>
                </div>
              )}

              <div className="db-panel">
                <div className="db-panel-head">
                  <div className="db-panel-title">All doctors</div>
                  <button className="db-btn primary sm" onClick={() => setShowAddDoctor(true)}>
                    + Add doctor
                  </button>
                </div>
                {doctorsError && (
                  <div style={{ color: "var(--alert)", fontSize: 13, padding: "0 20px 12px" }}>{doctorsError}</div>
                )}
                <div className="db-panel-body no-pad">
                  {doctorsLoading ? (
                    <div className="db-empty">Loading doctors…</div>
                  ) : filteredDoctors.length === 0 ? (
                    <div className="db-empty">No doctors found.</div>
                  ) : (
                    <table className="db-table">
                      <thead>
                        <tr>
                          <th>Name</th>
                          <th>Specialization</th>
                          <th>Status</th>
                          <th></th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredDoctors.map((d) => {
                          const isActive = d.status === "ACTIVE";
                          return (
                            <tr key={d.id}>
                              <td>{d.fullName}</td>
                              <td>{d.specialization}</td>
                              <td>
                                <span className={`db-badge ${isActive ? "confirmed" : "unavailable"}`}>
                                  {isActive ? "active" : "on leave"}
                                </span>
                              </td>
                              <td>
                                <button
                                  className="db-btn outline sm"
                                  disabled={statusUpdatingId === d.id}
                                  onClick={() => toggleDoctorStatus(d)}
                                >
                                  {statusUpdatingId === d.id
                                    ? "Updating…"
                                    : isActive
                                    ? "Mark on leave"
                                    : "Mark active"}
                                </button>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  )}
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* ---------- View history modal ---------- */}
      {historyPatient && (
        <div className="db-modal-overlay" onClick={closeHistory}>
          <div className="db-modal" onClick={(e) => e.stopPropagation()}>
            <div className="db-modal-title">{historyPatient.fullName}</div>
            <div className="db-modal-sub">Appointment history</div>

            {historyLoading ? (
              <div className="db-empty">Loading…</div>
            ) : historyError ? (
              <div className="db-empty">{historyError}</div>
            ) : historyAppts.length === 0 ? (
              <div className="db-empty">No appointments for this patient yet.</div>
            ) : (
              <table className="db-table">
                <thead>
                  <tr>
                    <th>Reference</th>
                    <th>Doctor</th>
                    <th>Date</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {historyAppts.map((a) => (
                    <tr key={a.id}>
                      <td style={{ fontFamily: "var(--font-mono)", fontSize: 12.5 }}>{a.reference}</td>
                      <td>{a.doctorName}</td>
                      <td>{a.date} · {a.time}</td>
                      <td>
                        <span className={`db-badge ${a.status.toLowerCase()}`}>{a.status.toLowerCase()}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}

            <div className="db-modal-actions">
              <button className="db-btn outline" onClick={closeHistory}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
