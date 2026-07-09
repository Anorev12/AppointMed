import { apiFetch } from "../../../shared/api/httpClient";

/**
 * FR-028, FR-030, FR-032, FR-034, FR-035 (user management) and
 * FR-016 (override doctor availability).
 */
export const AdminAPI = {
  createDoctor: (payload) =>
    apiFetch("/admin/doctors", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
};
