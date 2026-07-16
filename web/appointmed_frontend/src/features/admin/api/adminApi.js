import { apiFetch } from "../../../shared/api/httpClient";

/**
 * FR-028, FR-030, FR-032, FR-034, FR-035 (user management, reports) and
 * FR-016 (override any appointment).
 */
export const AdminAPI = {
  // ---- Doctors ----
  createDoctor: (payload) =>
    apiFetch("/admin/doctors", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  listDoctors: (search) => {
    const qs = search ? `?search=${encodeURIComponent(search)}` : "";
    return apiFetch(`/admin/doctors${qs}`);
  },

  setDoctorStatus: (id, status) =>
    apiFetch(`/admin/doctors/${id}/status`, {
      method: "PUT",
      body: JSON.stringify({ status }),
    }),

  deleteDoctor: (id) =>
    apiFetch(`/admin/doctors/${id}`, {
      method: "DELETE",
    }),

  // ---- Patients ----
  listPatients: (search) => {
    const qs = search ? `?search=${encodeURIComponent(search)}` : "";
    return apiFetch(`/admin/patients${qs}`);
  },

  createPatient: (payload) =>
    apiFetch("/admin/patients", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  deletePatient: (id) =>
    apiFetch(`/admin/patients/${id}`, {
      method: "DELETE",
    }),

  patientHistory: (id) => apiFetch(`/admin/patients/${id}/appointments`),

  // ---- Admins ----
  listAdmins: () => apiFetch("/admin/admins"),

  createAdmin: (payload) =>
    apiFetch("/admin/admins", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  // Deliberately no deleteAdmin — admins can never delete another admin account.

  changeOwnPassword: (payload) =>
    apiFetch("/admin/password", {
      method: "PUT",
      body: JSON.stringify(payload),
    }),

  // ---- Appointments ----

  /** FR-035: keyword/status are optional server-side search filters, same shape as patient history search. */
  listAppointments: ({ status, keyword } = {}) => {
    const params = new URLSearchParams();
    if (status) params.set("status", status);
    if (keyword) params.set("keyword", keyword);
    const qs = params.toString();
    return apiFetch(`/admin/appointments${qs ? `?${qs}` : ""}`);
  },

  overrideCancel: (id) =>
    apiFetch(`/admin/appointments/${id}/cancel`, {
      method: "PUT",
    }),

  // ---- Doctor availability (FR-016) ----

  getDoctorAvailability: (doctorId) => apiFetch(`/admin/doctors/${doctorId}/availability`),

  updateDoctorAvailability: (doctorId, workingDays, startTime, endTime) =>
    apiFetch(`/admin/doctors/${doctorId}/availability`, {
      method: "PUT",
      body: JSON.stringify({ workingDays: Array.from(workingDays), startTime, endTime }),
    }),

  addDoctorUnavailableDate: (doctorId, date) =>
    apiFetch(`/admin/doctors/${doctorId}/availability/unavailable-dates`, {
      method: "POST",
      body: JSON.stringify({ date }),
    }),

  removeDoctorUnavailableDate: (doctorId, date) =>
    apiFetch(`/admin/doctors/${doctorId}/availability/unavailable-dates/${date}`, {
      method: "DELETE",
    }),

  // ---- Reports (FR-035) ----
  getReport: () => apiFetch("/admin/reports"),
};