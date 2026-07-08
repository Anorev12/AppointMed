const API_ROOT = "http://localhost:8080/api";

function authHeaders() {
  const token = localStorage.getItem("am_token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

/**
 * Thin wrapper around fetch: prepends the API root, attaches the JWT from
 * localStorage, parses JSON, and throws on non-2xx responses so callers can
 * just try/catch instead of checking res.ok everywhere.
 */
async function apiFetch(path, options = {}) {
  const res = await fetch(`${API_ROOT}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(),
      ...(options.headers || {}),
    },
  });

  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!res.ok) {
    const message = typeof data === "string" ? data : data?.message || "Request failed.";
    throw new Error(message);
  }

  return data;
}

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