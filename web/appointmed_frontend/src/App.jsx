import { useState, useEffect } from "react";
import Login from "./features/auth/Login";
import Register from "./features/auth/Register";
import PatientDashboard from "./features/patient/PatientDashboard";
import DoctorDashboard from "./features/doctor/DoctorDashboard";
import AdminDashboard from "./features/admin/AdminDashboard";
import { AuthAPI } from "./features/auth/api/authApi";
import { getToken, clearToken } from "./shared/api/httpClient";

function viewForRole(role) {
  if (role === "DOCTOR") return "doctor";
  if (role === "ADMIN") return "admin";
  return "patient";
}

function App() {
  const [view, setView] = useState("login");
  const [user, setUser] = useState(null);
  const [checkingSession, setCheckingSession] = useState(true);

  // On refresh, re-verify the token against the server (via AuthAPI.me,
  // which hits /api/auth/me) instead of trusting whatever's cached in
  // localStorage — someone editing localStorage by hand to say
  // "role": "ADMIN" shouldn't get in. The role always comes from the
  // signed token.
  useEffect(() => {
    const token = getToken();

    if (!token) {
      setCheckingSession(false);
      return;
    }

    AuthAPI.me()
      .then((data) => {
        localStorage.setItem("user", JSON.stringify(data));
        setUser(data);
        setView(viewForRole(data.role));
      })
      .catch(() => {
        localStorage.removeItem("user");
        clearToken();
        setView("login");
      })
      .finally(() => setCheckingSession(false));
  }, []);

  function handleAuthSuccess(data) {
    // data = { id, fullName, email, role, token } — from Login/Register's
    // AuthResponse. AuthAPI.login/register already persisted the token;
    // this just keeps the display info (name, role) alongside it.
    localStorage.setItem("user", JSON.stringify(data));
    setUser(data);
    setView(viewForRole(data.role));
  }

  function handleLogout() {
    localStorage.removeItem("user");
    clearToken();
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
