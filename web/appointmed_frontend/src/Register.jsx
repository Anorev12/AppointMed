import { useState, useEffect, useRef } from "react";
import "./Appointmed.css";

/**
 * AppointMed — Register (patients only, per the SRS business rules:
 * doctor / admin accounts are created by a clinic administrator).
 *
 * Maps to SRS:
 *   FR-001  Collect full name, DOB, contact number, email, password
 *   FR-002  Validate all required fields before submit
 *   FR-003  Reject duplicate email — wire the server's 409 response into
 *           errors.email once handleSubmit calls the real API
 *
 * Props:
 *   onRegister(values)   — called with form values on valid submit
 *   onNavigateToLogin()  — called when the user taps "Log in"
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

// Point this at your Spring Boot server. During development that's
// usually http://localhost:8080 unless you changed server.port.
const API_BASE_URL = "http://localhost:8080/api/auth";

export default function Register({ onRegister, onNavigateToLogin }) {
  const [values, setValues] = useState({
    fullName: "",
    dob: "",
    contact: "",
    email: "",
    password: "",
    confirm: "",
  });
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
    if (!values.fullName.trim()) e.fullName = "Enter your full name.";
    if (!values.dob) e.dob = "Enter your date of birth.";
    if (!values.contact.trim()) e.contact = "Enter a contact number.";
    if (!values.email.trim()) {
      e.email = "Enter your email address.";
    } else if (!validateEmail(values.email)) {
      e.email = "Enter a valid email address, like name@example.com.";
    }
    if (!values.password) {
      e.password = "Enter a password.";
    } else if (values.password.length < 8) {
      e.password = "Use at least 8 characters.";
    }
    if (values.confirm !== values.password) {
      e.confirm = "Passwords don't match.";
    }
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  async function handleSubmit(ev) {
    ev.preventDefault();
    if (!validate()) return;
    setSubmitting(true);

    // Map the form's field names to what RegisterRequest.java expects.
    const payload = {
      fullName: values.fullName,
      dateOfBirth: values.dob, // "YYYY-MM-DD", matches LocalDate on the backend
      contactNumber: values.contact,
      email: values.email,
      password: values.password,
    };

    try {
      const res = await fetch(`${API_BASE_URL}/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      const data = await res.json();

      if (!res.ok) {
        // 409 duplicate email -> { field: "email", message: "..." } (FR-003)
        // 400 validation error -> { fieldName: "message", ... } straight from Bean Validation
        if (data.field && data.message) {
          setErrors((e) => ({ ...e, [data.field]: data.message }));
        } else {
          setErrors((e) => ({ ...e, ...data }));
        }
        setSubmitting(false);
        return;
      }

      localStorage.setItem("am_token", data.token);

      setSubmitting(false);
      alert(`Registration successful! Welcome, ${data.fullName || values.fullName}.`);
      onRegister && onRegister(data);
    } catch (err) {
      setSubmitting(false);
      setErrors((e) => ({
        ...e,
        email: "Can't reach the server. Check that it's running and try again.",
      }));
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
                  <span className="am-slot-time">{slot.time}</span>
                  <span className="am-slot-doctor">{slot.doctor}</span>
                  <span className={`am-slot-status ${slot.status}`}>
                    {slot.status === "open" ? "Open" : "Reserved"}
                  </span>
                </div>
              ))}
            </div>
          </div>

          <p className="am-foot-note">
            Reminders are sent 24 hours and 1 hour before every visit, by SMS or
            email — so nobody forgets.
          </p>

          <div className="am-perf" />
        </div>

        <div className="am-right">
          <div className="am-eyebrow">Patient registration</div>
          <h1 className="am-heading">Create your account</h1>
          <p className="am-heading-sub">
            Takes about a minute. You'll be able to book right after.
          </p>

          <form className="am-form" onSubmit={handleSubmit} noValidate>
            <div className="am-field">
              <label className="am-label" htmlFor="fullName">
                Full name
              </label>
              <input
                id="fullName"
                className="am-input"
                type="text"
                autoComplete="name"
                value={values.fullName}
                onChange={(e) => update("fullName", e.target.value)}
                aria-invalid={Boolean(errors.fullName)}
                aria-describedby={errors.fullName ? "fullName-error" : undefined}
              />
              {errors.fullName && (
                <span className="am-error" id="fullName-error">
                  {errors.fullName}
                </span>
              )}
            </div>

            <div className="am-row">
              <div className="am-field">
                <label className="am-label" htmlFor="dob">
                  Date of birth
                </label>
                <input
                  id="dob"
                  className="am-input"
                  type="date"
                  autoComplete="bday"
                  value={values.dob}
                  onChange={(e) => update("dob", e.target.value)}
                  aria-invalid={Boolean(errors.dob)}
                  aria-describedby={errors.dob ? "dob-error" : undefined}
                />
                {errors.dob && (
                  <span className="am-error" id="dob-error">
                    {errors.dob}
                  </span>
                )}
              </div>
              <div className="am-field">
                <label className="am-label" htmlFor="contact">
                  Contact number
                </label>
                <input
                  id="contact"
                  className="am-input"
                  type="tel"
                  autoComplete="tel"
                  placeholder="09xx xxx xxxx"
                  value={values.contact}
                  onChange={(e) => update("contact", e.target.value)}
                  aria-invalid={Boolean(errors.contact)}
                  aria-describedby={errors.contact ? "contact-error" : undefined}
                />
                {errors.contact && (
                  <span className="am-error" id="contact-error">
                    {errors.contact}
                  </span>
                )}
              </div>
            </div>

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

            <div className="am-row">
              <div className="am-field">
                <label className="am-label" htmlFor="password">
                  Password
                </label>
                <input
                  id="password"
                  className="am-input"
                  type="password"
                  autoComplete="new-password"
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

              <div className="am-field">
                <label className="am-label" htmlFor="confirm">
                  Confirm password
                </label>
                <input
                  id="confirm"
                  className="am-input"
                  type="password"
                  autoComplete="new-password"
                  value={values.confirm}
                  onChange={(e) => update("confirm", e.target.value)}
                  aria-invalid={Boolean(errors.confirm)}
                  aria-describedby={errors.confirm ? "confirm-error" : undefined}
                />
                {errors.confirm && (
                  <span className="am-error" id="confirm-error">
                    {errors.confirm}
                  </span>
                )}
              </div>
            </div>

            <button className="am-submit" type="submit" disabled={submitting}>
              <span className="am-stamp-dot" />
              {submitting ? "Creating account…" : "Create account"}
            </button>
          </form>

          <div className="am-switch">
            Already have an account?{" "}
            <button type="button" onClick={onNavigateToLogin}>
              Log in
            </button>
          </div>

          <p className="am-staff-note">
            Doctor or clinic staff? Accounts are set up by your administrator —
            contact the front desk for access.
          </p>
        </div>
      </div>
    </div>
  );
}