package com.example.appointmed.features.admin
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appointmed.R
import com.example.appointmed.databinding.ActivityAdminDashboardBinding
import com.example.appointmed.features.admin.fragments.AdminAppointmentsFragment
import com.example.appointmed.features.admin.fragments.AdminDoctorsFragment
import com.example.appointmed.features.admin.fragments.AdminOverviewFragment
import com.example.appointmed.features.admin.fragments.AdminPatientsFragment
import com.example.appointmed.features.admin.fragments.AdminProfileFragment
import com.example.appointmed.core.utils.TokenManager
import com.example.appointmed.features.auth.LoginActivity
class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        val user = tokenManager.getUser()
        if (user == null) {
            goToLogin()
            return
        }

        if (savedInstanceState == null) {
            showFragment(AdminOverviewFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_overview -> showFragment(AdminOverviewFragment())
                R.id.nav_admin_appointments -> showFragment(AdminAppointmentsFragment())
                R.id.nav_admin_patients -> showFragment(AdminPatientsFragment())
                R.id.nav_admin_doctors -> showFragment(AdminDoctorsFragment())
                R.id.nav_admin_profile -> showFragment(AdminProfileFragment())
            }
            true
        }
    }

    fun selectTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
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