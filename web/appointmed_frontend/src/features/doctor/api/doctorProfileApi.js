import { apiFetch } from "../../../shared/api/httpClient";

/** Doctor's own account actions — separate from DoctorAppointmentsAPI, which is patient-appointment-facing. */
export const DoctorProfileAPI = {
  changePassword: (payload) =>
    apiFetch("/doctor/profile/password", {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
};
