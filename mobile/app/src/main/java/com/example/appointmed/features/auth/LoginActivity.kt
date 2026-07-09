package com.example.appointmed.features.auth
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.appointmed.databinding.ActivityLoginBinding
import com.example.appointmed.features.auth.models.LoginRequest
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.core.utils.TokenManager
import com.example.appointmed.features.admin.AdminDashboardActivity
import com.example.appointmed.features.doctor.DoctorDashboardActivity
import com.example.appointmed.features.patient.DashboardActivity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Login screen. Mirrors the React Login.jsx handleSubmit logic:
 *   1. Validate email + password client-side.
 *   2. POST /api/auth/login with { email, password }.
 *   3. On success (200): save the token + user info, go to the
 *      role-appropriate dashboard.
 *   4. On failure (401/400): show the field-specific error message
 *      returned by the backend as { field, message }.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        // If already logged in, skip straight to the correct dashboard
        if (tokenManager.isLoggedIn()) {
            val user = tokenManager.getUser()
            goToDashboard(user?.role ?: "PATIENT")
            return
        }

        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }

        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnForgotPassword.setOnClickListener {
            // Not implemented in the backend yet — placeholder only.
        }
    }

    private fun attemptLogin() {
        // Clear previous errors
        binding.tvEmailError.visibility = View.GONE
        binding.tvPasswordError.visibility = View.GONE

        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        var hasError = false

        if (email.isEmpty()) {
            showFieldError(binding.tvEmailError, "Enter your email address.")
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showFieldError(binding.tvEmailError, "Enter a valid email address, like name@example.com.")
            hasError = true
        }

        if (password.isEmpty()) {
            showFieldError(binding.tvPasswordError, "Enter your password.")
            hasError = true
        }

        if (hasError) return

        setLoading(true)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.getAuthApi(this@LoginActivity)
                        .login(LoginRequest(email, password))
                }

                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    tokenManager.saveSession(loginResponse)
                    goToDashboard(loginResponse.role)
                } else {
                    handleErrorResponse(response.errorBody()?.string())
                }

            } catch (e: IOException) {
                setLoading(false)
                showFieldError(
                    binding.tvPasswordError,
                    "Can't reach the server. Check that it's running and try again."
                )
            } catch (e: Exception) {
                setLoading(false)
                showFieldError(
                    binding.tvPasswordError,
                    "Something went wrong. Try again."
                )
            }
        }
    }

    /**
     * Parses the backend's error shape: { "field": "email"|"password", "message": "..." }
     * and shows it under the matching field, same as the React version.
     */
    private fun handleErrorResponse(errorBodyString: String?) {
        if (errorBodyString.isNullOrEmpty()) {
            showFieldError(binding.tvPasswordError, "Something went wrong. Try again.")
            return
        }

        try {
            val errorMap = Gson().fromJson(errorBodyString, Map::class.java)
            val field = errorMap["field"] as? String
            val message = errorMap["message"] as? String ?: "Incorrect email or password."

            when (field) {
                "email" -> showFieldError(binding.tvEmailError, message)
                "password" -> showFieldError(binding.tvPasswordError, message)
                else -> showFieldError(binding.tvPasswordError, message)
            }
        } catch (e: Exception) {
            showFieldError(binding.tvPasswordError, "Incorrect email or password.")
        }
    }

    private fun showFieldError(target: android.widget.TextView, message: String) {
        target.text = message
        target.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "Logging in…" else "Log in"
    }

    private fun goToDashboard(role: String) {
        val intent = when (role) {
            "DOCTOR" -> Intent(this, DoctorDashboardActivity::class.java)
            "ADMIN" -> Intent(this, AdminDashboardActivity::class.java)
            else -> Intent(this, DashboardActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}