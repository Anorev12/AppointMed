package com.example.appointmed.features.auth
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.appointmed.databinding.ActivityRegisterBinding
import com.example.appointmed.features.auth.models.RegisterRequest
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.core.utils.TokenManager
import com.example.appointmed.features.patient.DashboardActivity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Calendar
import java.util.Locale

/**
 * Register screen (patients only). Mirrors React Register.jsx:
 *   1. Validate all fields client-side (fullName, dob, contact, email,
 *      password >= 8 chars, confirm matches password).
 *   2. POST /api/auth/register with fullName, dateOfBirth, contactNumber,
 *      email, password — confirmPassword is NOT sent.
 *   3. On success: save session, go to Dashboard.
 *   4. On failure (409 duplicate email / 400 validation): show the
 *      field-specific error returned by the backend.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var tokenManager: TokenManager
    private var selectedDob: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        binding.etDob.setOnClickListener {
            showDatePicker()
        }

        binding.btnRegister.setOnClickListener {
            attemptRegister()
        }

        binding.tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDob = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1,
                    selectedDay
                )
                binding.etDob.setText(selectedDob)
                binding.tvDobError.visibility = View.GONE
            },
            year, month, day
        )
        // Prevent selecting a future date of birth
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun attemptRegister() {
        // Clear previous errors
        binding.tvFullNameError.visibility = View.GONE
        binding.tvDobError.visibility = View.GONE
        binding.tvContactError.visibility = View.GONE
        binding.tvEmailError.visibility = View.GONE
        binding.tvPasswordError.visibility = View.GONE
        binding.tvConfirmPasswordError.visibility = View.GONE

        val fullName = binding.etFullName.text.toString().trim()
        val dob = selectedDob
        val contact = binding.etContact.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        var hasError = false

        if (fullName.isEmpty()) {
            showFieldError(binding.tvFullNameError, "Enter your full name.")
            hasError = true
        }

        if (dob.isEmpty()) {
            showFieldError(binding.tvDobError, "Enter your date of birth.")
            hasError = true
        }

        if (contact.isEmpty()) {
            showFieldError(binding.tvContactError, "Enter a contact number.")
            hasError = true
        }

        if (email.isEmpty()) {
            showFieldError(binding.tvEmailError, "Enter your email address.")
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showFieldError(binding.tvEmailError, "Enter a valid email address, like name@example.com.")
            hasError = true
        }

        if (password.isEmpty()) {
            showFieldError(binding.tvPasswordError, "Enter a password.")
            hasError = true
        } else if (password.length < 8) {
            showFieldError(binding.tvPasswordError, "Use at least 8 characters.")
            hasError = true
        }

        if (confirmPassword != password) {
            showFieldError(binding.tvConfirmPasswordError, "Passwords don't match.")
            hasError = true
        }

        if (hasError) return

        setLoading(true)

        val request = RegisterRequest(
            fullName = fullName,
            dateOfBirth = dob,
            contactNumber = contact,
            email = email,
            password = password
        )

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.getAuthApi(this@RegisterActivity).register(request)
                }

                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val registerResponse = response.body()!!
                    tokenManager.saveSession(registerResponse)
                    goToDashboard()
                } else {
                    handleErrorResponse(response.errorBody()?.string())
                }

            } catch (e: IOException) {
                setLoading(false)
                showFieldError(
                    binding.tvEmailError,
                    "Can't reach the server. Check that it's running and try again."
                )
            } catch (e: Exception) {
                setLoading(false)
                showFieldError(
                    binding.tvEmailError,
                    "Something went wrong. Try again."
                )
            }
        }
    }

    /**
     * Parses { "field": "...", "message": "..." } from the backend
     * (409 duplicate email, or 400 Bean Validation errors) and shows
     * it under the matching field.
     */
    private fun handleErrorResponse(errorBodyString: String?) {
        if (errorBodyString.isNullOrEmpty()) {
            showFieldError(binding.tvEmailError, "Something went wrong. Try again.")
            return
        }

        try {
            val errorMap = Gson().fromJson(errorBodyString, Map::class.java)
            val field = errorMap["field"] as? String
            val message = errorMap["message"] as? String ?: "Registration failed. Try again."

            when (field) {
                "fullName" -> showFieldError(binding.tvFullNameError, message)
                "dateOfBirth", "dob" -> showFieldError(binding.tvDobError, message)
                "contactNumber", "contact" -> showFieldError(binding.tvContactError, message)
                "email" -> showFieldError(binding.tvEmailError, message)
                "password" -> showFieldError(binding.tvPasswordError, message)
                else -> showFieldError(binding.tvEmailError, message)
            }
        } catch (e: Exception) {
            showFieldError(binding.tvEmailError, "Registration failed. Try again.")
        }
    }

    private fun showFieldError(target: android.widget.TextView, message: String) {
        target.text = message
        target.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
        binding.btnRegister.text = if (loading) "Creating account…" else "Create account"
    }

    private fun goToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}