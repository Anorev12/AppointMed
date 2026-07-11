import { apiFetch } from "../../../shared/api/httpClient";

/**
 * FR-028, FR-030, FR-032, FR-034, FR-035 (user management, reports) and
 * FR-016 (override any appointment).
 */
export const AdminAPI = {
  createDoctor: (payload) =>
    apiFetch("/admin/doctors", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  listDoctors: () => apiFetch("/admin/doctors"),

  setDoctorStatus: (id, status) =>
    apiFetch(`/admin/doctors/${id}/status`, {
      method: "PUT",
      body: JSON.stringify({ status }),
    }),

  listPatients: () => apiFetch("/admin/patients"),

  patientHistory: (id) => apiFetch(`/admin/patients/${id}/appointments`),

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
};
