import { useState, useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from "react-router-dom";
import Login from "./features/auth/Login";
import Register from "./features/auth/Register";
import PatientDashboard from "./features/patient/PatientDashboard";
import DoctorDashboard from "./features/doctor/DoctorDashboard";
import AdminDashboard from "./features/admin/AdminDashboard";
import { AuthAPI } from "./features/auth/api/authApi";
import { getToken, clearToken } from "./shared/api/httpClient";

function pathForRole(role) {
  if (role === "DOCTOR") return "/doctor";
  if (role === "ADMIN") return "/admin";
  return "/patient";
}

function AppRoutes() {
  const [user, setUser] = useState(null);
  const [checkingSession, setCheckingSession] = useState(true);
  const navigate = useNavigate();

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
        navigate(pathForRole(data.role), { replace: true });
      })
      .catch(() => {
        localStorage.removeItem("user");
        clearToken();
        navigate("/login", { replace: true });
      })
      .finally(() => setCheckingSession(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleAuthSuccess(data) {
    // data = { id, fullName, email, role, token } — from Login/Register's
    // AuthResponse. AuthAPI.login/register already persisted the token;
    // this just keeps the display info (name, role) alongside it.
    localStorage.setItem("user", JSON.stringify(data));
    setUser(data);
    navigate(pathForRole(data.role), { replace: true });
  }

  function handleLogout() {
    localStorage.removeItem("user");
    clearToken();
    setUser(null);
    navigate("/login", { replace: true });
  }

  if (checkingSession) {
    return (
      <div style={{ padding: 40, fontFamily: "sans-serif", color: "#4A7BA6" }}>
        Checking your session…
      </div>
    );
  }

  const homePath = user ? pathForRole(user.role) : "/login";

  return (
    <Routes>
      <Route
        path="/login"
        element={
          user ? (
            <Navigate to={homePath} replace />
          ) : (
            <Login onLogin={handleAuthSuccess} onNavigateToRegister={() => navigate("/register")} />
          )
        }
      />

      <Route
        path="/register"
        element={
          user ? (
            <Navigate to={homePath} replace />
          ) : (
            <Register onRegister={handleAuthSuccess} onNavigateToLogin={() => navigate("/login")} />
          )
        }
      />

      <Route
        path="/patient/*"
        element={
          user?.role === "PATIENT" ? (
            <PatientDashboard patientName={user.fullName} onLogout={handleLogout} />
          ) : (
            <Navigate to="/login" replace />
          )
        }
      />

      <Route
        path="/doctor/*"
        element={
          user?.role === "DOCTOR" ? (
            <DoctorDashboard doctorName={user.fullName} onLogout={handleLogout} />
          ) : (
            <Navigate to="/login" replace />
          )
        }
      />

      <Route
        path="/admin/*"
        element={
          user?.role === "ADMIN" ? (
            <AdminDashboard adminName={user.fullName} onLogout={handleLogout} />
          ) : (
            <Navigate to="/login" replace />
          )
        }
      />

      <Route path="/" element={<Navigate to={homePath} replace />} />
      <Route path="*" element={<Navigate to={homePath} replace />} />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  );
}

export default App;