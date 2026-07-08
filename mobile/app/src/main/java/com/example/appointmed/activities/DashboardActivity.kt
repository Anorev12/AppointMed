package com.example.appointmed.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appointmed.databinding.ActivityDashboardBinding
import com.example.appointmed.utils.TokenManager

/**
 * Patient Dashboard. Mirrors React PatientDashboard.jsx:
 * displays the logged-in user's fullName, email, contactNumber, and
 * dateOfBirth — all read from the session saved by TokenManager during
 * login/register (no separate /me endpoint exists yet, per the backend
 * contract). Logout clears the session and returns to Login.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        val user = tokenManager.getUser()

        if (user == null) {
            // No valid session — send back to Login
            goToLogin()
            return
        }

        binding.tvWelcome.text = "Welcome, ${user.fullName}"
        binding.tvEmail.text = user.email
        binding.tvContact.text = user.contactNumber
        binding.tvDateOfBirth.text = user.dateOfBirth

        binding.btnLogout.setOnClickListener {
            tokenManager.clearSession()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}