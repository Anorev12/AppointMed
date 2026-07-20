import { useState, useEffect, useRef } from "react";
import { formatTime12h } from "../../shared/utils/format";
import "../../shared/styles/Appointmed.css";
import { AuthAPI } from "./api/authApi";

/**
 * AppointMed — Login (patients, doctors, and admins all sign in here;
 * only patients can self-register, see Register.jsx).
 *
 * Maps to SRS FR-004: authenticate against email + password and issue a
 * JWT. Talks to the backend through AuthAPI.login (see ./api/authApi).
 *
 * Props:
 *   onLogin(values)        — called with { id, fullName, email, role, token } on valid submit
 *   onNavigateToRegister() — called when the user taps "Register"
 */

const UPCOMING_SLOTS = [
  { time: "09:00", doctor: "Dr. Reyes · Pediatrics", status: "open" },
  { time: "09:30", doctor: "Dr. Reyes · Pediatrics", status: "reserved" },
  { time: "10:00", doctor: "Dr. Tan · Internal Med.", status: "open" },
  { time: "10:30", doctor: "Dr. Tan · Internal Med.", status: "open" },
  { time: "11:00", doctor: "Dr. Cruz · Dermatology", status: "reserved" },
  { time: "11:30", doctor: "Dr. Cruz · Dermatology", status: "open" },
];

function makeReference() {
  const n = Math.floor(100000 + Math.random() * 900000);
  return `APT-${n}`;
}

function validateEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

export default function Login({ onLogin, onNavigateToRegister }) {
  const [values, setValues] = useState({ email: "", password: "" });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [activeSlot, setActiveSlot] = useState(0);
  const [reference, setReference] = useState(makeReference());

  const reduceMotion = useRef(
    typeof window !== "undefined" &&
      window.matchMedia &&
      window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );

  useEffect(() => {
    if (reduceMotion.current) return;
    const id = setInterval(() => {
      setActiveSlot((i) => (i + 1) % UPCOMING_SLOTS.length);
      setReference(makeReference());
    }, 2600);
    return () => clearInterval(id);
  }, []);

  function update(field, val) {
    setValues((v) => ({ ...v, [field]: val }));
    setErrors((e) => ({ ...e, [field]: undefined }));
  }

  function validate() {
    const e = {};
    if (!values.email.trim()) {
      e.email = "Enter your email address.";
    } else if (!validateEmail(values.email)) {
      e.email = "Enter a valid email address, like name@example.com.";
    }
    if (!values.password) {
      e.password = "Enter your password.";
    }
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  async function handleSubmit(ev) {
    ev.preventDefault();
    if (!validate()) return;
    setSubmitting(true);

    try {
      const data = await AuthAPI.login(values.email, values.password);
      setSubmitting(false);
      onLogin && onLogin(data);
    } catch (err) {
      setSubmitting(false);

      if (err.status === undefined) {
        // Network-level failure (server unreachable), not a 4xx from the API.
        setErrors((e) => ({
          ...e,
          password: "Can't reach the server. Check that it's running and try again.",
        }));
        return;
      }

      // Backend sends { field: "password", message: "Incorrect email or password." }
      // on bad credentials (401), or { field: "...", message: "..." } for validation errors.
      const field = err.data?.field || "password";
      const message = err.data?.message || "Password Incorrect. Please try again.";
      setErrors((e) => ({ ...e, [field]: message }));
    }
  }

  return (
    <div className="am-root">
      <div className="am-shell">
        <div className="am-left">
          <div>
            <div className="am-brand">
              Appoint<span>Med</span>
            </div>
            <div className="am-tagline">Your next appointment, reserved.</div>
            <p className="am-sub">
              Book a doctor, get a confirmation, and know exactly when to show up —
              no phone tag with the front desk.
            </p>

            <div className="am-ticket" aria-hidden="true">
              <div className="am-ticket-head">
                <span>Today's openings</span>
                <strong>{reference}</strong>
              </div>
              {UPCOMING_SLOTS.map((slot, i) => (
                <div
                  key={slot.time}
                  className={`am-slot-row${i === activeSlot ? " is-active" : ""}`}
                >
                  <span className="am-slot-time">{formatTime12h(slot.time)}</span>
                  <span className="am-slot-doctor">{slot.doctor}</span>
                  <span className={`am-slot-status ${slot.status}`}>
                    {slot.status === "open" ? "Open" : "Reserved"}
                  </span>
                </div>
              ))}
            </div>
          </div>

          <p className="am-foot-note">
            Reminders are sent 24 hours and 1 hour before every visit, by email — so nobody forgets.
          </p>

          <div className="am-perf" />
        </div>

        <div className="am-right">
          <div className="am-eyebrow">Patient · Doctor · Admin</div>
          <h1 className="am-heading">Welcome back</h1>
          <p className="am-heading-sub">
            Log in to book, reschedule, or check your upcoming visits.
          </p>

          <form className="am-form" onSubmit={handleSubmit} noValidate>
            <div className="am-field">
              <label className="am-label" htmlFor="email">
                Email address
              </label>
              <input
                id="email"
                className="am-input"
                type="email"
                autoComplete="email"
                value={values.email}
                onChange={(e) => update("email", e.target.value)}
                aria-invalid={Boolean(errors.email)}
                aria-describedby={errors.email ? "email-error" : undefined}
              />
              {errors.email && (
                <span className="am-error" id="email-error">
                  {errors.email}
                </span>
              )}
            </div>

            <div className="am-field">
              <label className="am-label" htmlFor="password">
                Password
              </label>
              <input
                id="password"
                className="am-input"
                type="password"
                autoComplete="current-password"
                value={values.password}
                onChange={(e) => update("password", e.target.value)}
                aria-invalid={Boolean(errors.password)}
                aria-describedby={errors.password ? "password-error" : undefined}
              />
              {errors.password && (
                <span className="am-error" id="password-error">
                  {errors.password}
                </span>
              )}
            </div>

            <div className="am-row-between">
              <button type="button" className="am-forgot">
                Forgot password?
              </button>
            </div>

            <button className="am-submit" type="submit" disabled={submitting}>
              <span className="am-stamp-dot" />
              {submitting ? "Logging in…" : "Log in"}
            </button>
          </form>

          <div className="am-switch">
            New to AppointMed?{" "}
            <button type="button" onClick={onNavigateToRegister}>
              Register as a patient
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
