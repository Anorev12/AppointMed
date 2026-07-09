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
  list: () => apiFetch("/patient/appointments"),

  book: (doctorId, date, time) =>
    apiFetch("/patient/appointments", {
      method: "POST",
      body: JSON.stringify({ doctorId, date, time }),
    }),

  cancel: (id) =>
    apiFetch(`/patient/appointments/${id}/cancel`, {
      method: "PUT",
    }),
};

export const PatientProfileAPI = {
  get: () => apiFetch("/patient/profile"),

  update: (fullName, contactNumber, medicalHistory) =>
    apiFetch("/patient/profile", {
      method: "PUT",
      body: JSON.stringify({ fullName, contactNumber, medicalHistory }),
    }),
};
