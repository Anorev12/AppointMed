import { apiFetch } from "../../../shared/api/httpClient";

/**
 * FR-006 to FR-013 — patient-facing endpoints.
 * Mirrors the AvailabilityAPI/AuthAPI pattern used elsewhere in the app.
 */
export const DoctorsAPI = {
  list: () => apiFetch("/doctors"),

  getSlots: (doctorId, date) =>
    apiFetch(`/doctors/${doctorId}/slots?date=${encodeURIComponent(date)}`),
};

export const AppointmentsAPI = {
  /** FR-012: status/keyword/from/to are all optional server-side search filters. */
  list: ({ status, keyword, from, to } = {}) => {
    const params = new URLSearchParams();
    if (status) params.set("status", status);
    if (keyword) params.set("keyword", keyword);
    if (from) params.set("from", from);
    if (to) params.set("to", to);
    const qs = params.toString();
    return apiFetch(`/patient/appointments${qs ? `?${qs}` : ""}`);
  },

  book: (doctorId, date, time) =>
    apiFetch("/patient/appointments", {
      method: "POST",
      body: JSON.stringify({ doctorId, date, time }),
    }),

  cancel: (id) =>
    apiFetch(`/patient/appointments/${id}/cancel`, {
      method: "PUT",
    }),

  /** FR-011: move a confirmed appointment to a new date/time with the same doctor. */
  reschedule: (id, date, time) =>
    apiFetch(`/patient/appointments/${id}/reschedule`, {
      method: "PUT",
      body: JSON.stringify({ date, time }),
    }),
};

export const PatientProfileAPI = {
  get: () => apiFetch("/patient/profile"),

  update: (fullName, contactNumber, dateOfBirth, medicalHistory) =>
    apiFetch("/patient/profile", {
      method: "PUT",
      body: JSON.stringify({ fullName, contactNumber, dateOfBirth, medicalHistory }),
    }),

  changePassword: (payload) =>
    apiFetch("/patient/profile/password", {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
};