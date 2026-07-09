import { useState } from "react";
import "../../shared/styles/Appointmed.css";
import { AdminAPI } from "./api/adminApi";

/**
 * AppointMed — Admin Dashboard
 * Covers: FR-028, FR-030, FR-032, FR-034, FR-035 (overview, user mgmt, reports)
 * and FR-016 (override doctor availability)
 */

const INITIAL_PATIENTS = [
  { id: 1, name: "Juan Dela Cruz", email: "juan.delacruz@email.com", contact: "0917 123 4567" },
  { id: 2, name: "Maria Santos", email: "maria.santos@email.com", contact: "0918 222 3333" },
  { id: 3, name: "Ana Lim", email: "ana.lim@email.com", contact: "0919 444 5555" },
];

const INITIAL_DOCTORS = [
  { id: 1, name: "Dr. Reyes", specialization: "Pediatrics", status: "active" },
  { id: 2, name: "Dr. Tan", specialization: "Internal Medicine", status: "active" },
  { id: 3, name: "Dr. Cruz", specialization: "Dermatology", status: "on leave" },
];

const INITIAL_APPOINTMENTS = [
  { id: "APT-102938", patient: "Juan Dela Cruz", doctor: "Dr. Reyes", date: "2026-07-10", time: "09:00", status: "confirmed" },
  { id: "APT-103012", patient: "Maria Santos", doctor: "Dr. Reyes", date: "2026-07-10", time: "09:30", status: "pending" },
  { id: "APT-102890", patient: "Ana Lim", doctor: "Dr. Tan", date: "2026-07-11", time: "10:00", status: "confirmed" },
  { id: "APT-101877", patient: "Juan Dela Cruz", doctor: "Dr. Cruz", date: "2026-06-30", time: "10:30", status: "cancelled" },
];

export default function AdminDashboard({ adminName = "Admin", onLogout }) {
  const [view, setView] = useState("overview"); // overview | patients | doctors | appointments
  const [search, setSearch] = useState("");
  const [patients] = useState(INITIAL_PATIENTS);
  const [doctors, setDoctors] = useState(INITIAL_DOCTORS);
  const [appointments, setAppointments] = useState(INITIAL_APPOINTMENTS);
  const [showAddDoctor, setShowAddDoctor] = useState(false);
  const [newDoctor, setNewDoctor] = useState({ fullName: "", email: "", password: "", specialization: "" });
  const [addDoctorError, setAddDoctorError] = useState("");
  const [addingDoctor, setAddingDoctor] = useState(false);

  const todayCount = appointments.filter((a) => a.date === "2026-07-10" && a.status !== "cancelled").length;
  const pendingCount = appointments.filter((a) => a.status === "pending").length;

  const filteredPatients = patients.filter((p) =>
    p.name.toLowerCase().includes(search.toLowerCase())
  );
  const filteredDoctors = doctors.filter((d) =>
    d.name.toLowerCase().includes(search.toLowerCase())
  );
  const filteredAppointments = appointments.filter(
    (a) =>
      a.patient.toLowerCase().includes(search.toLowerCase()) ||
      a.doctor.toLowerCase().includes(search.toLowerCase())
  );

  function toggleDoctorStatus(id) {
    setDoctors((prev) =>
      prev.map((d) =>
        d.id === id ? { ...d, status: d.status === "active" ? "on leave" : "active" } : d
      )
    );
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
      setDoctors((prev) => [
        ...prev,
        { id: data.id, name: data.fullName, specialization: data.specialization, status: "active" },
      ]);
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

  function overrideCancel(id) {
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
                  <div className="db-stat-value">{patients.length}</div>
                  <div className="db-stat-label">Registered patients</div>
                </div>
                <div className="db-stat-card">
                  <div className="db-stat-value">{doctors.length}</div>
                  <div className="db-stat-label">Active doctors</div>
                </div>
                <div className="db-stat-card">
                  <div className="db-stat-value">{todayCount}</div>
                  <div className="db-stat-label">Appointments today</div>
                </div>
                <div className="db-stat-card">
                  <div className="db-stat-value">{pendingCount}</div>
                  <div className="db-stat-label">Pending requests</div>
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
                          <td style={{ fontFamily: "var(--font-mono)", fontSize: 12.5 }}>{a.id}</td>
                          <td>{a.patient}</td>
                          <td>{a.doctor}</td>
                          <td>{a.date} · {a.time}</td>
                          <td>
                            <span className={`db-badge ${a.status}`}>{a.status}</span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}

          {/* ---------- APPOINTMENTS ---------- */}
          {view === "appointments" && (
            <div className="db-panel">
              <div className="db-panel-body no-pad">
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
                        <td style={{ fontFamily: "var(--font-mono)", fontSize: 12.5 }}>{a.id}</td>
                        <td>{a.patient}</td>
                        <td>{a.doctor}</td>
                        <td>{a.date}</td>
                        <td>{a.time}</td>
                        <td>
                          <span className={`db-badge ${a.status}`}>{a.status}</span>
                        </td>
                        <td>
                          {a.status !== "cancelled" && (
                            <button className="db-btn danger sm" onClick={() => overrideCancel(a.id)}>
                              Override cancel
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

          {/* ---------- PATIENTS ---------- */}
          {view === "patients" && (
            <div className="db-panel">
              <div className="db-panel-body no-pad">
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
                        <td>{p.name}</td>
                        <td>{p.email}</td>
                        <td>{p.contact}</td>
                        <td>
                          <button className="db-btn outline sm">View history</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
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
                <div className="db-panel-body no-pad">
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
                      {filteredDoctors.map((d) => (
                        <tr key={d.id}>
                          <td>{d.name}</td>
                          <td>{d.specialization}</td>
                          <td>
                            <span className={`db-badge ${d.status === "active" ? "confirmed" : "unavailable"}`}>
                              {d.status}
                            </span>
                          </td>
                          <td>
                            <button className="db-btn outline sm" onClick={() => toggleDoctorStatus(d.id)}>
                              {d.status === "active" ? "Mark on leave" : "Mark active"}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
