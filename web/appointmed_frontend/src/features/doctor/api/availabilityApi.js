import { apiFetch } from "../../../shared/api/httpClient";

/**
 * FR-014, FR-015, FR-017, FR-018, FR-019 — doctor availability management.
 */
export const AvailabilityAPI = {
  get: () => apiFetch("/doctor/availability"),

  updateSchedule: (workingDays, startTime, endTime) =>
    apiFetch("/doctor/availability", {
      method: "PUT",
      body: JSON.stringify({ workingDays: Array.from(workingDays), startTime, endTime }),
    }),

  addUnavailableDate: (date) =>
    apiFetch("/doctor/availability/unavailable-dates", {
      method: "POST",
      body: JSON.stringify({ date }),
    }),

  removeUnavailableDate: (date) =>
    apiFetch(`/doctor/availability/unavailable-dates/${date}`, {
      method: "DELETE",
    }),
};
