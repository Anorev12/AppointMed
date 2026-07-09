import { apiFetch, setToken } from "../../../shared/api/httpClient";

/**
 * FR-001..FR-004: registration + login + session verification.
 * Both login and register issue a JWT, which we persist here right after
 * a successful call so every other slice can rely on it being set.
 */
export const AuthAPI = {
  login: async (email, password) => {
    const data = await apiFetch("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
    setToken(data.token);
    return data;
  },

  register: async (payload) => {
    const data = await apiFetch("/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    setToken(data.token);
    return data;
  },

  me: () => apiFetch("/auth/me"),
};
