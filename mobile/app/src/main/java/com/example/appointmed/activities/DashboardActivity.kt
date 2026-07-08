package com.example.appointmed.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appointmed.R
import com.example.appointmed.databinding.ActivityDashboardBinding
import com.example.appointmed.fragments.BookFragment
import com.example.appointmed.fragments.HistoryFragment
import com.example.appointmed.fragments.HomeFragment
import com.example.appointmed.fragments.ProfileFragment
import com.example.appointmed.utils.TokenManager

/**
 * Patient Dashboard shell. Hosts four fragments (Home, Book, History,
 * Profile) swapped via the bottom navigation bar — the mobile equivalent
 * of the React PatientDashboard's sidebar + view-state routing.
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
            goToLogin()
            return
        }

        if (savedInstanceState == null) {
            showFragment(HomeFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showFragment(HomeFragment())
                R.id.nav_book -> showFragment(BookFragment())
                R.id.nav_history -> showFragment(HistoryFragment())
                R.id.nav_profile -> showFragment(ProfileFragment())
            }
            true
        }
    }

    /** Lets fragments (e.g. Book, after a successful booking) jump to another tab. */
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