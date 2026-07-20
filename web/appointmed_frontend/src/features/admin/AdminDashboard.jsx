import { useState, useEffect, useCallback } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "../../shared/styles/Appointmed.css";
import { AdminAPI } from "./api/adminApi";
import { formatTime12h } from "../../shared/utils/format";

const WEEKDAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

function daysArrayToMap(daysArray) {
  const map = {};
  WEEKDAYS.forEach((day) => {
    map[day] = daysArray.includes(day);
  });
  return map;
}

/**
 * AppointMed — Admin Dashboard
 * Covers: FR-028, FR-030, FR-032, FR-034, FR-035 (overview, user mgmt, reports)
 * and FR-016 (override any appointment).
 *
 * Fully wired to the backend:
 *  - GET    /api/admin/patients                    (patient roster, ?search=)
 *  - POST   /api/admin/patients                     (create patient account)
 *  - DELETE /api/admin/patients/{id}                (delete patient account)
 *  - GET    /api/admin/patients/{id}/appointments   (per-patient history)
 *  - GET    /api/admin/doctors                      (doctor roster + status, ?search=)
 *  - POST   /api/admin/doctors                      (create doctor account)
 *  - PUT    /api/admin/doctors/{id}/status          (mark active / on leave)
 *  - DELETE /api/admin/doctors/{id}                 (delete doctor account)
 *  - GET    /api/admin/admins                        (admin roster — no delete action exists)
 *  - POST   /api/admin/admins                        (create another admin account)
 *  - PUT    /api/admin/password                      (change the logged-in admin's own password)
 *  - GET    /api/admin/appointments                 (clinic-wide appointments)
 *  - PUT    /api/admin/appointments/{id}/cancel     (override cancel)
 *  - GET    /api/admin/settings/templates            (FR-024: list notification templates)
 *  - PUT    /api/admin/settings/templates/{type}    (FR-024: edit a template's subject/custom message)
 *  - POST   /api/admin/settings/templates/{type}/reset (FR-024: reset a template to its default)
 *  - GET    /api/admin/settings/reminders            (FR-024: current reminder offset hours)
 *  - PUT    /api/admin/settings/reminders            (FR-024: update reminder offset hours)
 *
 * Business rule: there is no delete button, and no backend endpoint, for
 * removing an admin account — admins can only ever change their own password.
 */

function todayStr() {
  return new Date().toISOString().slice(0, 10);
}

// Mirrors the backend's cancelByAdmin check: an appointment is only
// cancellable by an admin while it's still in the future (date + time,
// not just the date), same rule AppointmentService.cancelByAdmin enforces.
function isUpcoming(date, time) {
  return new Date(`${date}T${time}`) > new Date();
}

export default function AdminDashboard({ adminName = "Admin", onLogout }) {
  const location = useLocation();
const navigate = useNavigate();

const PATH_TO_VIEW = {
  "/admin": "overview",
  "/admin/overview": "overview",
  "/admin/appointments": "appointments",
  "/admin/patients": "patients",
  "/admin/doctors": "doctors",
  "/admin/admins": "admins",
  "/admin/reports": "reports",
  "/admin/settings": "settings",
  "/admin/password": "password",
};
const VIEW_TO_PATH = {
  overview: "/admin/overview",
  appointments: "/admin/appointments",
  patients: "/admin/patients",
  doctors: "/admin/doctors",
  admins: "/admin/admins",
  reports: "/admin/reports",
  settings: "/admin/settings",
  password: "/admin/password",
};

const view = PATH_TO_VIEW[location.pathname] || "overview";

function setView(next) {
  navigate(VIEW_TO_PATH[next] || "/admin/overview");
}
  const [search, setSearch] = useState("");

  // ---- Delete confirmation modal (doctor or patient) ----
  // { type: "doctor" | "patient", target: { id, fullName } } | null
  const [confirmDelete, setConfirmDelete] = useState(null);


  // ---- Patients ----
  const [patients, setPatients] = useState([]);
  const [patientsLoading, setPatientsLoading] = useState(true);
  const [patientsError, setPatientsError] = useState("");
  const [deletingPatientId, setDeletingPatientId] = useState(null);

  // ---- Doctors ----
  const [doctors, setDoctors] = useState([]);
  const [doctorsLoading, setDoctorsLoading] = useState(true);
  const [doctorsError, setDoctorsError] = useState("");
  const [statusUpdatingId, setStatusUpdatingId] = useState(null);
  const [deletingDoctorId, setDeletingDoctorId] = useState(null);

  // ---- Manage doctor availability (FR-016) ----
  const [availDoctor, setAvailDoctor] = useState(null); // { id, fullName } | null
  const [availLoading, setAvailLoading] = useState(false);
  const [availError, setAvailError] = useState("");
  const [availWorkingDays, setAvailWorkingDays] = useState({});
  const [availHours, setAvailHours] = useState({ start: "09:00", end: "17:00" });
  const [availUnavailableDates, setAvailUnavailableDates] = useState([]);
  const [newUnavailableDate, setNewUnavailableDate] = useState("");
  const [savingSchedule, setSavingSchedule] = useState(false);
  const [scheduleSaveMessage, setScheduleSaveMessage] = useState("");
  const [dateActionError, setDateActionError] = useState("");

  // ---- Admins ----
  const [admins, setAdmins] = useState([]);
  const [adminsLoading, setAdminsLoading] = useState(true);
  const [adminsError, setAdminsError] = useState("");

  // ---- Appointments ----
  const [appointments, setAppointments] = useState([]);
  const [apptsLoading, setApptsLoading] = useState(true);
  const [apptsError, setApptsError] = useState("");
  const [cancellingId, setCancellingId] = useState(null);
  const [apptFilter, setApptFilter] = useState("all"); // all | confirmed | cancelled | today | upcoming

  // ---- Add doctor form ----
  const [showAddDoctor, setShowAddDoctor] = useState(false);
  const [newDoctor, setNewDoctor] = useState({ fullName: "", email: "", password: "", specialization: "" });
  const [addDoctorError, setAddDoctorError] = useState("");
  const [addingDoctor, setAddingDoctor] = useState(false);

  // ---- Add patient form ----
  const [showAddPatient, setShowAddPatient] = useState(false);
  const [newPatient, setNewPatient] = useState({ fullName: "", email: "", password: "", contactNumber: "", dateOfBirth: "" });
  const [addPatientError, setAddPatientError] = useState("");
  const [addingPatient, setAddingPatient] = useState(false);

  // ---- Add admin form ----
  const [showAddAdmin, setShowAddAdmin] = useState(false);
  const [newAdmin, setNewAdmin] = useState({ fullName: "", email: "", password: "" });
  const [addAdminError, setAddAdminError] = useState("");
  const [addingAdmin, setAddingAdmin] = useState(false);

  // ---- Change own password ----
  const [passwordForm, setPasswordForm] = useState({ oldPassword: "", newPassword: "", confirmPassword: "" });
  const [passwordError, setPasswordError] = useState("");
  const [passwordSuccess, setPasswordSuccess] = useState("");
  const [changingPassword, setChangingPassword] = useState(false);

  // ---- View history modal ----
  const [historyPatient, setHistoryPatient] = useState(null); // { id, fullName } | null
  const [historyAppts, setHistoryAppts] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState("");

  // ---- Reports (FR-035) ----
  const [report, setReport] = useState(null);
  const [reportLoading, setReportLoading] = useState(true);
  const [reportError, setReportError] = useState("");

  // ---- Notification settings (FR-024) ----
  const [templates, setTemplates] = useState([]);
  const [templatesLoading, setTemplatesLoading] = useState(true);
  const [templatesError, setTemplatesError] = useState("");
  const [selectedTemplateType, setSelectedTemplateType] = useState(null);
  const [templateDraft, setTemplateDraft] = useState({ subjectTemplate: "", customMessage: "" });
  const [savingTemplate, setSavingTemplate] = useState(false);
  const [templateSaveMessage, setTemplateSaveMessage] = useState("");
  const [templateSaveError, setTemplateSaveError] = useState("");

  const [reminderOffsets, setReminderOffsets] = useState([]);
  const [reminderLoading, setReminderLoading] = useState(true);
  const [reminderError, setReminderError] = useState("");
  const [newOffsetInput, setNewOffsetInput] = useState("");
  const [savingReminders, setSavingReminders] = useState(false);
  const [reminderSaveMessage, setReminderSaveMessage] = useState("");

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

  const loadAdmins = useCallback(async () => {
    setAdminsLoading(true);
    setAdminsError("");
    try {
      const data = await AdminAPI.listAdmins();
      setAdmins(data);
    } catch (err) {
      console.error("ADMINS LOAD ERROR:", err);
      setAdminsError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't load admins.");
    } finally {
      setAdminsLoading(false);
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

  const loadReport = useCallback(async () => {
    setReportLoading(true);
    setReportError("");
    try {
      const data = await AdminAPI.getReport();
      setReport(data);
    } catch (err) {
      setReportError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't load the report.");
    } finally {
      setReportLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPatients();
    loadDoctors();
    loadAdmins();
    loadAppointments();
    loadReport();
  }, [loadPatients, loadDoctors, loadAdmins, loadAppointments, loadReport]);

  // Stats can go stale while the admin works on other tabs, so refresh on every visit to Reports.
  useEffect(() => {
    if (view === "reports") {
      loadReport();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view]);

  const loadTemplates = useCallback(async () => {
    setTemplatesLoading(true);
    setTemplatesError("");
    try {
      const data = await AdminAPI.listTemplates();
      setTemplates(data);
      // Keep whatever's selected in view; otherwise default to the first template in the list.
      setSelectedTemplateType((prev) => prev || (data[0] && data[0].type) || null);
    } catch (err) {
      setTemplatesError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't load notification templates.");
    } finally {
      setTemplatesLoading(false);
    }
  }, []);

  const loadReminderSettings = useCallback(async () => {
    setReminderLoading(true);
    setReminderError("");
    try {
      const data = await AdminAPI.getReminderSettings();
      setReminderOffsets(data.offsetHours || []);
    } catch (err) {
      setReminderError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't load the reminder schedule.");
    } finally {
      setReminderLoading(false);
    }
  }, []);

  // Settings data isn't needed until the admin actually visits the tab.
  useEffect(() => {
    if (view === "settings") {
      loadTemplates();
      loadReminderSettings();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view]);

  // Load the draft form whenever the selected template (or the underlying list) changes.
  useEffect(() => {
    const t = templates.find((tpl) => tpl.type === selectedTemplateType);
    if (t) {
      setTemplateDraft({ subjectTemplate: t.subjectTemplate, customMessage: t.customMessage || "" });
      setTemplateSaveMessage("");
      setTemplateSaveError("");
    }
  }, [selectedTemplateType, templates]);

  async function saveTemplate() {
    if (!selectedTemplateType) return;
    setSavingTemplate(true);
    setTemplateSaveError("");
    setTemplateSaveMessage("");
    try {
      const updated = await AdminAPI.updateTemplate(
        selectedTemplateType,
        templateDraft.subjectTemplate,
        templateDraft.customMessage
      );
      setTemplates((prev) => prev.map((t) => (t.type === updated.type ? updated : t)));
      setTemplateSaveMessage("Saved.");
    } catch (err) {
      setTemplateSaveError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't save the template.");
    } finally {
      setSavingTemplate(false);
    }
  }

  async function resetTemplate() {
    if (!selectedTemplateType) return;
    setSavingTemplate(true);
    setTemplateSaveError("");
    setTemplateSaveMessage("");
    try {
      const updated = await AdminAPI.resetTemplate(selectedTemplateType);
      setTemplates((prev) => prev.map((t) => (t.type === updated.type ? updated : t)));
      setTemplateSaveMessage("Reset to default.");
    } catch (err) {
      setTemplateSaveError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't reset the template.");
    } finally {
      setSavingTemplate(false);
    }
  }

  function addReminderOffset() {
    const hours = parseInt(newOffsetInput, 10);
    if (!Number.isFinite(hours) || hours < 1 || hours > 168) {
      setReminderError("Enter a number of hours between 1 and 168.");
      return;
    }
    if (reminderOffsets.includes(hours)) {
      setNewOffsetInput("");
      return;
    }
    setReminderError("");
    setReminderOffsets((prev) => [...prev, hours].sort((a, b) => b - a));
    setNewOffsetInput("");
  }

  function removeReminderOffset(hours) {
    setReminderOffsets((prev) => prev.filter((h) => h !== hours));
  }

  async function saveReminderSettings() {
    setSavingReminders(true);
    setReminderError("");
    setReminderSaveMessage("");
    try {
      const updated = await AdminAPI.updateReminderSettings(reminderOffsets);
      setReminderOffsets(updated.offsetHours || []);
      setReminderSaveMessage("Saved.");
    } catch (err) {
      setReminderError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't save the reminder schedule.");
    } finally {
      setSavingReminders(false);
    }
  }

  const todayCount = appointments.filter((a) => a.date === todayStr() && a.status !== "CANCELLED").length;
  const completedCount = appointments.filter((a) => a.status === "COMPLETED").length;

  const filteredPatients = patients.filter((p) =>
    p.fullName.toLowerCase().includes(search.toLowerCase()) || p.email.toLowerCase().includes(search.toLowerCase())
  );
  const filteredDoctors = doctors.filter((d) =>
    d.fullName.toLowerCase().includes(search.toLowerCase()) || d.email.toLowerCase().includes(search.toLowerCase())
  );
  const filteredAdmins = admins.filter((a) =>
    a.fullName.toLowerCase().includes(search.toLowerCase()) || a.email.toLowerCase().includes(search.toLowerCase())
  );
  const filteredAppointments = appointments
    .filter(
      (a) =>
        a.patientName.toLowerCase().includes(search.toLowerCase()) ||
        a.doctorName.toLowerCase().includes(search.toLowerCase()) ||
        a.reference.toLowerCase().includes(search.toLowerCase())
    )
    .filter((a) => {
      if (apptFilter === "confirmed") return a.status === "CONFIRMED";
      if (apptFilter === "cancelled") return a.status === "CANCELLED";
      if (apptFilter === "today") return a.date === todayStr();
      if (apptFilter === "upcoming") return a.date >= todayStr() && a.status === "CONFIRMED";
      return true;
    })
    .sort((a, b) => {
      if (apptFilter !== "upcoming") return 0;
      return a.date === b.date ? a.time.localeCompare(b.time) : a.date.localeCompare(b.date);
    });

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

  async function deleteDoctor(doctor) {
    setConfirmDelete(null);
    setDeletingDoctorId(doctor.id);
    setDoctorsError("");
    try {
      await AdminAPI.deleteDoctor(doctor.id);
      setDoctors((prev) => prev.filter((d) => d.id !== doctor.id));
    } catch (err) {
      setDoctorsError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't delete that doctor.");
    } finally {
      setDeletingDoctorId(null);
    }
  }

  // ---- Manage doctor availability (FR-016) ----

  async function openManageAvailability(doctor) {
    setAvailDoctor(doctor);
    setAvailLoading(true);
    setAvailError("");
    setScheduleSaveMessage("");
    setDateActionError("");
    try {
      const data = await AdminAPI.getDoctorAvailability(doctor.id);
      setAvailWorkingDays(daysArrayToMap(data.workingDays || []));
      setAvailHours({ start: data.startTime || "09:00", end: data.endTime || "17:00" });
      setAvailUnavailableDates(data.unavailableDates || []);
    } catch (err) {
      setAvailError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't load this doctor's availability.");
    } finally {
      setAvailLoading(false);
    }
  }

  function closeManageAvailability() {
    setAvailDoctor(null);
    setAvailWorkingDays({});
    setAvailUnavailableDates([]);
    setNewUnavailableDate("");
  }

  function toggleAvailDay(day) {
    setAvailWorkingDays((prev) => ({ ...prev, [day]: !prev[day] }));
  }

  async function saveDoctorSchedule() {
    setSavingSchedule(true);
    setAvailError("");
    setScheduleSaveMessage("");
    try {
      const selectedDays = WEEKDAYS.filter((day) => availWorkingDays[day]);
      const data = await AdminAPI.updateDoctorAvailability(availDoctor.id, selectedDays, availHours.start, availHours.end);
      setAvailWorkingDays(daysArrayToMap(data.workingDays || []));
      setAvailHours({ start: data.startTime, end: data.endTime });
      setScheduleSaveMessage("Schedule updated.");
    } catch (err) {
      setAvailError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't save this schedule.");
    } finally {
      setSavingSchedule(false);
    }
  }

  async function addDoctorUnavailableDate() {
    if (!newUnavailableDate) return;
    setDateActionError("");
    try {
      const data = await AdminAPI.addDoctorUnavailableDate(availDoctor.id, newUnavailableDate);
      setAvailUnavailableDates(data.unavailableDates || []);
      setNewUnavailableDate("");
    } catch (err) {
      setDateActionError(err.message || "Couldn't mark that date unavailable.");
    }
  }

  async function removeDoctorUnavailableDate(date) {
    setDateActionError("");
    try {
      const data = await AdminAPI.removeDoctorUnavailableDate(availDoctor.id, date);
      setAvailUnavailableDates(data.unavailableDates || []);
    } catch (err) {
      setDateActionError(err.message || "Couldn't remove that date.");
    }
  }

  async function createPatient(ev) {
    ev.preventDefault();
    setAddPatientError("");
    setAddingPatient(true);
    try {
      const data = await AdminAPI.createPatient(newPatient);
      setPatients((prev) => [...prev, data]);
      setNewPatient({ fullName: "", email: "", password: "", contactNumber: "", dateOfBirth: "" });
      setShowAddPatient(false);
    } catch (err) {
      if (err.status === undefined) {
        setAddPatientError("Can't reach the server. Check that it's running and try again.");
      } else {
        setAddPatientError(err.data?.message || err.message || "Couldn't create patient account.");
      }
    } finally {
      setAddingPatient(false);
    }
  }

  async function deletePatient(patient) {
    setConfirmDelete(null);
    setDeletingPatientId(patient.id);
    setPatientsError("");
    try {
      await AdminAPI.deletePatient(patient.id);
      setPatients((prev) => prev.filter((p) => p.id !== patient.id));
    } catch (err) {
      setPatientsError(err.status === undefined ? "Can't reach the server." : err.message || "Couldn't delete that patient.");
    } finally {
      setDeletingPatientId(null);
    }
  }

  async function createAdmin(ev) {
    ev.preventDefault();
    setAddAdminError("");

    if (!newAdmin.email.toLowerCase().endsWith("@appointmedadmin.com")) {
      setAddAdminError("Admin email must end in @appointmedadmin.com");
      return;
    }

    setAddingAdmin(true);
    try {
      const data = await AdminAPI.createAdmin(newAdmin);
      setAdmins((prev) => [...prev, data]);
      setNewAdmin({ fullName: "", email: "", password: "" });
      setShowAddAdmin(false);
    } catch (err) {
      if (err.status === undefined) {
        setAddAdminError("Can't reach the server. Check that it's running and try again.");
      } else {
        setAddAdminError(err.data?.message || err.message || "Couldn't create admin account.");
      }
    } finally {
      setAddingAdmin(false);
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
      await AdminAPI.changeOwnPassword(passwordForm);
      setPasswordSuccess("Password updated successfully.");
      setPasswordForm({ oldPassword: "", newPassword: "", confirmPassword: "" });
    } catch (err) {
      if (err.status === undefined) {
        setPasswordError("Can't reach the server. Check that it's running and try again.");
      } else {
        setPasswordError(err.data?.message || err.message || "Couldn't update your password.");
      }
    } finally {
      setChangingPassword(false);
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
          <button
            className={`db-nav-item${view === "admins" ? " is-active" : ""}`}
            onClick={() => setView("admins")}
          >
            Admins
          </button>
          <button
            className={`db-nav-item${view === "reports" ? " is-active" : ""}`}
            onClick={() => setView("reports")}
          >
            Reports
          </button>
          <button
            className={`db-nav-item${view === "settings" ? " is-active" : ""}`}
            onClick={() => setView("settings")}
          >
            Settings
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
              {view === "admins" && "Admins"}
              {view === "reports" && "Reports"}
              {view === "settings" && "Notification settings"}
              {view === "password" && "Change password"}
            </div>
            <div className="db-topbar-sub">
              {view === "overview" && "Today's snapshot across the clinic."}
              {view === "appointments" && "Manage and override any appointment."}
              {view === "patients" && "Registered patient accounts."}
              {view === "doctors" && "Manage doctor profiles and availability."}
              {view === "admins" && "Administrator accounts. Admins can't be deleted here — only created."}
              {view === "reports" && "Appointment statistics and system activity (FR-035)."}
              {view === "settings" && "Configure notification templates and the reminder schedule (FR-024)."}
              {view === "password" && "Update the password for your own account."}
            </div>
          </div>

          {["appointments", "patients", "doctors", "admins"].includes(view) && (
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
                            <td>{a.date} · {formatTime12h(a.time)}</td>
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
              <div className="db-panel-head">
                <div className="db-panel-title">All appointments</div>
                <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                  <button
                    className={`db-btn sm ${apptFilter === "all" ? "primary" : "outline"}`}
                    onClick={() => setApptFilter("all")}
                  >
                    All
                  </button>
                  <button
                    className={`db-btn sm ${apptFilter === "confirmed" ? "primary" : "outline"}`}
                    onClick={() => setApptFilter("confirmed")}
                  >
                    Confirmed
                  </button>
                  <button
                    className={`db-btn sm ${apptFilter === "cancelled" ? "primary" : "outline"}`}
                    onClick={() => setApptFilter("cancelled")}
                  >
                    Cancelled
                  </button>
                  <button
                    className={`db-btn sm ${apptFilter === "today" ? "primary" : "outline"}`}
                    onClick={() => setApptFilter("today")}
                  >
                    Today
                  </button>
                  <button
                    className={`db-btn sm ${apptFilter === "upcoming" ? "primary" : "outline"}`}
                    onClick={() => setApptFilter("upcoming")}
                  >
                    Upcoming
                  </button>
                </div>
              </div>
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
                          <td>{formatTime12h(a.time)}</td>
                          <td>
                            <span className={`db-badge ${a.status.toLowerCase()}`}>{a.status.toLowerCase()}</span>
                          </td>
                          <td>
                            {a.status === "CONFIRMED" && isUpcoming(a.date, a.time) && (
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
            <>
              <div className="db-panel">
                <div className="db-panel-head">
                  <div className="db-panel-title">All patients</div>
                  <button className="db-btn primary sm" onClick={() => setShowAddPatient(true)}>
                    + Add patient
                  </button>
                </div>
                {patientsError && (
                  <div style={{ color: "var(--alert)", fontSize: 13, padding: "0 20px 12px" }}>{patientsError}</div>
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
                            <td style={{ display: "flex", gap: 8 }}>
                              <button className="db-btn outline sm" onClick={() => openHistory(p)}>
                                View history
                              </button>
                              <button
                                className="db-btn danger sm"
                                disabled={deletingPatientId === p.id}
                                onClick={() => setConfirmDelete({ type: "patient", target: p })}
                              >
                                {deletingPatientId === p.id ? "Deleting…" : "Delete"}
                              </button>
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

          {/* ---------- DOCTORS ---------- */}
          {view === "doctors" && (
            <>
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
                              <td style={{ display: "flex", gap: 8 }}>
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
                                <button className="db-btn outline sm" onClick={() => openManageAvailability(d)}>
                                  Manage availability
                                </button>
                                <button
                                  className="db-btn danger sm"
                                  disabled={deletingDoctorId === d.id}
                                  onClick={() => setConfirmDelete({ type: "doctor", target: d })}
                                >
                                  {deletingDoctorId === d.id ? "Deleting…" : "Delete"}
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

          {/* ---------- ADMINS ---------- */}
          {view === "admins" && (
            <>
              <div className="db-panel">
                <div className="db-panel-head">
                  <div className="db-panel-title">All admins</div>
                  <button className="db-btn primary sm" onClick={() => setShowAddAdmin(true)}>
                    + Add admin
                  </button>
                </div>
                {adminsError && (
                  <div style={{ color: "var(--alert)", fontSize: 13, padding: "0 20px 12px" }}>{adminsError}</div>
                )}
                <div className="db-panel-body no-pad">
                  {adminsLoading ? (
                    <div className="db-empty">Loading admins…</div>
                  ) : filteredAdmins.length === 0 ? (
                    <div className="db-empty">No admins found.</div>
                  ) : (
                    <table className="db-table">
                      <thead>
                        <tr>
                          <th>Name</th>
                          <th>Email</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredAdmins.map((a) => (
                          <tr key={a.id}>
                            <td>{a.fullName}</td>
                            <td>{a.email}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              </div>
            </>
          )}

          {/* ---------- REPORTS (FR-035) ---------- */}
          {view === "reports" && (
            <>
              {reportLoading ? (
                <div className="db-panel">
                  <div className="db-panel-body">
                    <div className="db-empty">Loading report…</div>
                  </div>
                </div>
              ) : reportError ? (
                <div className="db-panel">
                  <div className="db-panel-body">
                    <div className="db-empty">{reportError}</div>
                  </div>
                </div>
              ) : (
                <>
                  <div className="db-stats-grid">
                    <div className="db-stat-card">
                      <div className="db-stat-value">{report.totalPatients}</div>
                      <div className="db-stat-label">Total patients</div>
                    </div>
                    <div className="db-stat-card">
                      <div className="db-stat-value">{report.totalDoctors}</div>
                      <div className="db-stat-label">Total doctors</div>
                    </div>
                    <div className="db-stat-card">
                      <div className="db-stat-value">{report.totalAppointments}</div>
                      <div className="db-stat-label">Total appointments</div>
                    </div>
                    <div className="db-stat-card">
                      <div className="db-stat-value">{report.appointmentsToday}</div>
                      <div className="db-stat-label">Appointments today</div>
                    </div>
                    <div className="db-stat-card">
                      <div className="db-stat-value">{report.appointmentsThisWeek}</div>
                      <div className="db-stat-label">Appointments this week</div>
                    </div>
                    <div className="db-stat-card">
                      <div className="db-stat-value">{report.totalNotifications}</div>
                      <div className="db-stat-label">Notifications logged</div>
                    </div>
                  </div>

                  <div className="db-panel">
                    <div className="db-panel-head">
                      <div className="db-panel-title">Appointments by status</div>
                    </div>
                    <div className="db-panel-body no-pad">
                      <table className="db-table">
                        <thead>
                          <tr>
                            <th>Status</th>
                            <th>Count</th>
                          </tr>
                        </thead>
                        <tbody>
                          {Object.entries(report.appointmentsByStatus || {}).map(([status, count]) => (
                            <tr key={status}>
                              <td>{status}</td>
                              <td>{count}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>

                  <div className="db-panel">
                    <div className="db-panel-head">
                      <div className="db-panel-title">Busiest doctors</div>
                    </div>
                    <div className="db-panel-body no-pad">
                      {(report.topDoctorsByAppointments || []).length === 0 ? (
                        <div className="db-empty">No appointment data yet.</div>
                      ) : (
                        <table className="db-table">
                          <thead>
                            <tr>
                              <th>Doctor</th>
                              <th>Specialization</th>
                              <th>Appointments</th>
                            </tr>
                          </thead>
                          <tbody>
                            {report.topDoctorsByAppointments.map((d) => (
                              <tr key={d.doctorId}>
                                <td>{d.doctorName}</td>
                                <td>{d.specialization || "—"}</td>
                                <td>{d.appointmentCount}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      )}
                    </div>
                  </div>

                  <div className="db-panel">
                    <div className="db-panel-head">
                      <div className="db-panel-title">Notification delivery</div>
                    </div>
                    <div className="db-panel-body no-pad">
                      <table className="db-table">
                        <thead>
                          <tr>
                            <th>Status</th>
                            <th>Count</th>
                          </tr>
                        </thead>
                        <tbody>
                          {Object.entries(report.notificationsByStatus || {}).map(([status, count]) => (
                            <tr key={status}>
                              <td>{status}</td>
                              <td>{count}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </>
              )}
            </>
          )}

          {/* ---------- CHANGE PASSWORD ---------- */}
          {view === "settings" && (
            <>
              {/* ---- Notification templates ---- */}
              <div className="db-panel" style={{ marginBottom: 24 }}>
                <div className="db-panel-head">
                  <div className="db-panel-title">Notification templates</div>
                </div>
                {templatesError && (
                  <div style={{ color: "var(--alert)", fontSize: 13, padding: "0 20px 12px" }}>{templatesError}</div>
                )}
                <div className="db-panel-body" style={{ display: "flex", gap: 24, flexWrap: "wrap" }}>
                  {templatesLoading ? (
                    <div className="db-empty">Loading templates…</div>
                  ) : (
                    <>
                      <div style={{ minWidth: 220 }}>
                        {templates.map((t) => (
                          <button
                            key={t.type}
                            type="button"
                            className={`db-tmpl-item${selectedTemplateType === t.type ? " is-active" : ""}`}
                            style={{ width: "100%", textAlign: "left", marginBottom: 4 }}
                            onClick={() => setSelectedTemplateType(t.type)}
                          >
                            {t.label}
                          </button>
                        ))}
                      </div>

                      {selectedTemplateType && (
                        <div style={{ flex: 1, minWidth: 320 }}>
                          <div className="db-field">
                            <label className="db-label">Subject line</label>
                            <input
                              className="db-input"
                              value={templateDraft.subjectTemplate}
                              onChange={(e) =>
                                setTemplateDraft({ ...templateDraft, subjectTemplate: e.target.value })
                              }
                            />
                          </div>
                          <div className="db-field">
                            <label className="db-label">Custom message (optional)</label>
                            <textarea
                              className="db-textarea"
                              rows={4}
                              placeholder="Shown as a highlighted note inside the email — e.g. clinic hours, a holiday notice. Leave blank to omit it."
                              value={templateDraft.customMessage}
                              onChange={(e) =>
                                setTemplateDraft({ ...templateDraft, customMessage: e.target.value })
                              }
                              style={{ resize: "vertical", fontFamily: "inherit" }}
                            />
                          </div>

                          <div style={{ fontSize: 12.5, color: "var(--text-muted, #5B6B7C)", marginBottom: 14 }}>
                            Available placeholders for the subject line:{" "}
                            {templates.find((t) => t.type === selectedTemplateType)?.availablePlaceholders}
                          </div>

                          {templateSaveError && (
                            <div style={{ color: "var(--alert)", fontSize: 13, marginBottom: 12 }}>{templateSaveError}</div>
                          )}
                          {templateSaveMessage && (
                            <div style={{ color: "var(--green)", fontSize: 13, marginBottom: 12 }}>{templateSaveMessage}</div>
                          )}

                          <div style={{ display: "flex", gap: 8 }}>
                            <button className="db-btn primary" disabled={savingTemplate} onClick={saveTemplate}>
                              {savingTemplate ? "Saving…" : "Save template"}
                            </button>
                            <button className="db-btn outline" disabled={savingTemplate} onClick={resetTemplate}>
                              Reset to default
                            </button>
                          </div>
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>

              {/* ---- Reminder schedule ---- */}
              <div className="db-panel" style={{ maxWidth: 480 }}>
                <div className="db-panel-head">
                  <div className="db-panel-title">Reminder schedule</div>
                </div>
                <div className="db-panel-body">
                  {reminderLoading ? (
                    <div className="db-empty">Loading…</div>
                  ) : (
                    <>
                      <div style={{ fontSize: 13, color: "var(--text-muted, #5B6B7C)", marginBottom: 14 }}>
                        Patients get a reminder email this many hours before their appointment. Add or remove
                        offsets below — at least one is required.
                      </div>

                      {reminderOffsets.length === 0 ? (
                        <div className="db-empty">No reminder offsets configured.</div>
                      ) : (
                        <div style={{ marginBottom: 16 }}>
                          {reminderOffsets.map((hours) => (
                            <div
                              key={hours}
                              style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "6px 0" }}
                            >
                              <span style={{ fontSize: 14 }}>{hours} hour{hours === 1 ? "" : "s"} before</span>
                              <button className="db-btn outline sm" onClick={() => removeReminderOffset(hours)}>
                                Remove
                              </button>
                            </div>
                          ))}
                        </div>
                      )}

                      <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
                        <input
                          className="db-input"
                          type="number"
                          min={1}
                          max={168}
                          placeholder="Hours before (e.g. 48)"
                          value={newOffsetInput}
                          onChange={(e) => setNewOffsetInput(e.target.value)}
                          style={{ flex: 1 }}
                        />
                        <button className="db-btn outline sm" type="button" onClick={addReminderOffset}>
                          Add
                        </button>
                      </div>

                      {reminderError && (
                        <div style={{ color: "var(--alert)", fontSize: 13, marginBottom: 12 }}>{reminderError}</div>
                      )}
                      {reminderSaveMessage && (
                        <div style={{ color: "var(--green)", fontSize: 13, marginBottom: 12 }}>{reminderSaveMessage}</div>
                      )}

                      <button className="db-btn primary" disabled={savingReminders} onClick={saveReminderSettings}>
                        {savingReminders ? "Saving…" : "Save schedule"}
                      </button>
                    </>
                  )}
                </div>
              </div>
            </>
          )}

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

      {/* ---------- Delete confirmation modal ---------- */}
      {confirmDelete && (
        <div className="db-modal-overlay" onClick={() => setConfirmDelete(null)}>
          <div className="db-modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 420 }}>
            <div className="db-modal-title">Delete {confirmDelete.target.fullName}?</div>
            <div className="db-modal-sub">This can't be undone.</div>
            <div className="db-modal-actions">
              <button type="button" className="db-btn outline" onClick={() => setConfirmDelete(null)}>
                Cancel
              </button>
              <button
                type="button"
                className="db-btn danger"
                onClick={() =>
                  confirmDelete.type === "doctor"
                    ? deleteDoctor(confirmDelete.target)
                    : deletePatient(confirmDelete.target)
                }
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ---------- Add patient modal ---------- */}
      {showAddPatient && (
        <div
          className="db-modal-overlay"
          onClick={() => {
            setShowAddPatient(false);
            setAddPatientError("");
          }}
        >
          <div className="db-modal" onClick={(e) => e.stopPropagation()}>
            <div className="db-modal-title">Add patient account</div>
            <div className="db-modal-sub">Create login credentials for a new patient.</div>
            <form onSubmit={createPatient}>
              <div className="db-field">
                <label className="db-label">Full name</label>
                <input
                  className="db-input"
                  value={newPatient.fullName}
                  onChange={(e) => setNewPatient({ ...newPatient, fullName: e.target.value })}
                  required
                />
              </div>
              <div className="db-field">
                <label className="db-label">Email</label>
                <input
                  className="db-input"
                  type="email"
                  value={newPatient.email}
                  onChange={(e) => setNewPatient({ ...newPatient, email: e.target.value })}
                  required
                />
              </div>
              <div className="db-field">
                <label className="db-label">Temporary password</label>
                <input
                  className="db-input"
                  type="password"
                  value={newPatient.password}
                  onChange={(e) => setNewPatient({ ...newPatient, password: e.target.value })}
                  required
                />
              </div>
              <div className="db-field">
                <label className="db-label">Contact number</label>
                <input
                  className="db-input"
                  value={newPatient.contactNumber}
                  onChange={(e) => setNewPatient({ ...newPatient, contactNumber: e.target.value })}
                />
              </div>
              <div className="db-field">
                <label className="db-label">Date of birth (optional)</label>
                <input
                  type="date"
                  className="db-input"
                  style={{ maxWidth: 220 }}
                  value={newPatient.dateOfBirth}
                  max={todayStr()}
                  onChange={(e) => setNewPatient({ ...newPatient, dateOfBirth: e.target.value })}
                />
              </div>

              {addPatientError && (
                <div style={{ color: "var(--alert)", fontSize: 13, marginBottom: 12 }}>{addPatientError}</div>
              )}

              <div className="db-modal-actions">
                <button
                  type="button"
                  className="db-btn outline"
                  onClick={() => {
                    setShowAddPatient(false);
                    setAddPatientError("");
                  }}
                >
                  Cancel
                </button>
                <button className="db-btn primary" type="submit" disabled={addingPatient}>
                  {addingPatient ? "Creating…" : "Create patient account"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ---------- Add doctor modal ---------- */}
      {showAddDoctor && (
        <div
          className="db-modal-overlay"
          onClick={() => {
            setShowAddDoctor(false);
            setAddDoctorError("");
          }}
        >
          <div className="db-modal" onClick={(e) => e.stopPropagation()}>
            <div className="db-modal-title">Add doctor account</div>
            <div className="db-modal-sub">Create login credentials for a new doctor.</div>
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
                <div style={{ color: "var(--alert)", fontSize: 13, marginBottom: 12 }}>{addDoctorError}</div>
              )}

              <div className="db-modal-actions">
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
                <button className="db-btn primary" type="submit" disabled={addingDoctor}>
                  {addingDoctor ? "Creating…" : "Create doctor account"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ---------- Add admin modal ---------- */}
      {showAddAdmin && (
        <div
          className="db-modal-overlay"
          onClick={() => {
            setShowAddAdmin(false);
            setAddAdminError("");
          }}
        >
          <div className="db-modal" onClick={(e) => e.stopPropagation()}>
            <div className="db-modal-title">Add admin account</div>
            <div className="db-modal-sub">Create login credentials for a new admin.</div>
            <form onSubmit={createAdmin}>
              <div className="db-field">
                <label className="db-label">Full name</label>
                <input
                  className="db-input"
                  value={newAdmin.fullName}
                  onChange={(e) => setNewAdmin({ ...newAdmin, fullName: e.target.value })}
                  required
                />
              </div>
              <div className="db-field">
                <label className="db-label">Email (must end in @appointmedadmin.com)</label>
                <input
                  className="db-input"
                  type="email"
                  placeholder="name@appointmedadmin.com"
                  value={newAdmin.email}
                  onChange={(e) => setNewAdmin({ ...newAdmin, email: e.target.value })}
                  required
                />
              </div>
              <div className="db-field">
                <label className="db-label">Temporary password</label>
                <input
                  className="db-input"
                  type="password"
                  value={newAdmin.password}
                  onChange={(e) => setNewAdmin({ ...newAdmin, password: e.target.value })}
                  required
                />
              </div>

              {addAdminError && (
                <div style={{ color: "var(--alert)", fontSize: 13, marginBottom: 12 }}>{addAdminError}</div>
              )}

              <div className="db-modal-actions">
                <button
                  type="button"
                  className="db-btn outline"
                  onClick={() => {
                    setShowAddAdmin(false);
                    setAddAdminError("");
                  }}
                >
                  Cancel
                </button>
                <button className="db-btn primary" type="submit" disabled={addingAdmin}>
                  {addingAdmin ? "Creating…" : "Create admin account"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

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
                      <td>{a.date} · {formatTime12h(a.time)}</td>
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

      {/* ---------- Manage doctor availability modal (FR-016) ---------- */}
      {availDoctor && (
        <div className="db-modal-overlay" onClick={closeManageAvailability}>
          <div className="db-modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 520 }}>
            <div className="db-modal-title">{availDoctor.fullName}</div>
            <div className="db-modal-sub">Working days, hours, and leave dates</div>

            {availLoading ? (
              <div className="db-empty">Loading…</div>
            ) : availError ? (
              <div className="db-empty">{availError}</div>
            ) : (
              <>
                <div style={{ marginBottom: 20 }}>
                  {WEEKDAYS.map((day) => (
                    <label
                      key={day}
                      style={{ display: "flex", alignItems: "center", gap: 10, padding: "6px 0", fontSize: 14 }}
                    >
                      <input
                        type="checkbox"
                        checked={!!availWorkingDays[day]}
                        onChange={() => toggleAvailDay(day)}
                      />
                      {day}
                    </label>
                  ))}
                </div>

                <div style={{ display: "flex", gap: 12, marginBottom: 16 }}>
                  <div className="db-field" style={{ flex: 1 }}>
                    <label className="db-label">Start time</label>
                    <input
                      className="db-input"
                      type="time"
                      value={availHours.start}
                      onChange={(e) => setAvailHours({ ...availHours, start: e.target.value })}
                    />
                  </div>
                  <div className="db-field" style={{ flex: 1 }}>
                    <label className="db-label">End time</label>
                    <input
                      className="db-input"
                      type="time"
                      value={availHours.end}
                      onChange={(e) => setAvailHours({ ...availHours, end: e.target.value })}
                    />
                  </div>
                </div>

                {scheduleSaveMessage && (
                  <div style={{ color: "var(--green)", fontSize: 13, marginBottom: 12 }}>{scheduleSaveMessage}</div>
                )}

                <button className="db-btn primary" disabled={savingSchedule} onClick={saveDoctorSchedule} style={{ marginBottom: 24 }}>
                  {savingSchedule ? "Saving…" : "Save schedule"}
                </button>

                <div className="db-panel-title" style={{ marginBottom: 10 }}>Unavailable dates</div>

                <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
                  <input
                    className="db-input"
                    type="date"
                    value={newUnavailableDate}
                    onChange={(e) => setNewUnavailableDate(e.target.value)}
                    style={{ flex: 1 }}
                  />
                  <button className="db-btn outline sm" onClick={addDoctorUnavailableDate}>
                    Add
                  </button>
                </div>

                {dateActionError && (
                  <div style={{ color: "var(--alert)", fontSize: 13, marginBottom: 12 }}>{dateActionError}</div>
                )}

                {availUnavailableDates.length === 0 ? (
                  <div className="db-empty">No dates marked off.</div>
                ) : (
                  <div>
                    {availUnavailableDates.map((date) => (
                      <div
                        key={date}
                        style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "6px 0" }}
                      >
                        <span style={{ fontSize: 14 }}>{date}</span>
                        <button className="db-btn outline sm" onClick={() => removeDoctorUnavailableDate(date)}>
                          Remove
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}

            <div className="db-modal-actions">
              <button className="db-btn outline" onClick={closeManageAvailability}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}