package com.example.appointmed.features.doctor
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appointmed.R
import com.example.appointmed.databinding.ActivityDoctorDashboardBinding
import com.example.appointmed.features.auth.LoginActivity
import com.example.appointmed.features.doctor.fragments.AvailabilityFragment
import com.example.appointmed.features.doctor.fragments.DoctorProfileFragment
import com.example.appointmed.features.doctor.fragments.ScheduleFragment
import com.example.appointmed.core.utils.TokenManager
class DoctorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorDashboardBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        val user = tokenManager.getUser()
        if (user == null) {
            goToLogin()
            return
        }

        if (savedInstanceState == null) {
            showFragment(ScheduleFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_schedule -> showFragment(ScheduleFragment())
                R.id.nav_availability -> showFragment(AvailabilityFragment())
                R.id.nav_doctor_profile -> showFragment(DoctorProfileFragment())
            }
            true
        }
    }

    private fun showFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun logout() {
        tokenManager.clearSession()
        goToLogin()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}