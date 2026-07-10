import { apiFetch } from "../../../shared/api/httpClient";

/**
 * Doctor's view of appointments booked with them, plus cancel/complete.
 * Booking itself is patient-initiated — see features/patient/api/patientApi.js.
 */
export const DoctorAppointmentsAPI = {
  list: () => apiFetch("/doctor/appointments"),

  cancel: (id) =>
    apiFetch(`/doctor/appointments/${id}/cancel`, {
      method: "PUT",
    }),

  complete: (id) =>
    apiFetch(`/doctor/appointments/${id}/complete`, {
      method: "PUT",
    }),
};
