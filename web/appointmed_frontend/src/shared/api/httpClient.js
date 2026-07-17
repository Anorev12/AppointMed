const API_ROOT = "https://appointmed-vwfg.onrender.com/api";

function authHeaders() {
  const token = localStorage.getItem("am_token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

/**
 * Thin wrapper around fetch: prepends the API root, attaches the JWT from
 * localStorage, parses JSON, and throws on non-2xx responses so callers can
 * just try/catch instead of checking res.ok everywhere.
 *
 * Errors carry `.status` (HTTP status, undefined for network failures) and
 * `.data` (the parsed body, so callers can still read field-level errors
 * like { field: "email", message: "..." }).
 */
export async function apiFetch(path, options = {}) {
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
    const error = new Error(message);
    error.status = res.status;
    error.data = data;
    throw error;
  }

  return data;
}

export function setToken(token) {
  localStorage.setItem("am_token", token);
}

export function clearToken() {
  localStorage.removeItem("am_token");
}

export function getToken() {
  return localStorage.getItem("am_token");
}
