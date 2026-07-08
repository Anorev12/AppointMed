import { useState, useEffect } from "react";
import Login from "./Login";
import Register from "./Register";
import PatientDashboard from "./PatientDashboard";
import DoctorDashboard from "./DoctorDashboard";
import AdminDashboard from "./AdminDashboard";

const API_BASE_URL = "http://localhost:8080/api/auth";

function viewForRole(role) {
  if (role === "DOCTOR") return "doctor";
  if (role === "ADMIN") return "admin";
  return "patient";
}

function App() {
  const [view, setView] = useState("login");
  const [user, setUser] = useState(null);
  const [checkingSession, setCheckingSession] = useState(true);

  // On refresh, re-verify the token against the server (via /api/auth/me)
  // instead of trusting whatever's cached in localStorage. The old code
  // just trusted the saved JSON blob directly — someone could edit
  // localStorage by hand to say "role": "ADMIN" and get in. This way the
  // role actually comes from the signed token every time.
  useEffect(() => {
    const token = localStorage.getItem("am_token");

    if (!token) {
      setCheckingSession(false);
      return;
    }

    fetch(`${API_BASE_URL}/me`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => {
        if (!res.ok) throw new Error("Session invalid or expired");
        return res.json();
      })
      .then((data) => {
        localStorage.setItem("user", JSON.stringify(data));
        setUser(data);
        setView(viewForRole(data.role));
        setCheckingSession(false);
      })
      .catch(() => {
        localStorage.removeItem("user");
        localStorage.removeItem("am_token");
        setView("login");
        setCheckingSession(false);
      });
  }, []);

  function handleAuthSuccess(data) {
    // data = { id, fullName, email, role, token } — from Login/Register's
    // AuthResponse. Login.jsx / Register.jsx already save "am_token"
    // themselves; this just keeps the display info (name, role) alongside it.
    localStorage.setItem("user", JSON.stringify(data));
    setUser(data);
    setView(viewForRole(data.role));
  }

  function handleLogout() {
    localStorage.removeItem("user");
    localStorage.removeItem("am_token");
    setUser(null);
    setView("login");
  }

  if (checkingSession) {
    return (
      <div style={{ padding: 40, fontFamily: "sans-serif", color: "#4A7BA6" }}>
        Checking your session…
      </div>
    );
  }

  return (
    <div className="App">
      {view === "login" && (
        <Login
          onLogin={handleAuthSuccess}
          onNavigateToRegister={() => setView("register")}
        />
      )}

      {view === "register" && (
        <Register
          onRegister={handleAuthSuccess}
          onNavigateToLogin={() => setView("login")}
        />
      )}

      {view === "patient" && (
        <PatientDashboard patientName={user?.fullName} onLogout={handleLogout} />
      )}

      {view === "doctor" && (
        <DoctorDashboard doctorName={user?.fullName} onLogout={handleLogout} />
      )}

      {view === "admin" && (
        <AdminDashboard adminName={user?.fullName} onLogout={handleLogout} />
      )}
    </div>
  );
}

export default App;