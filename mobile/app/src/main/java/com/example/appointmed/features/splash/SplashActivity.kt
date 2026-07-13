package com.example.appointmed.features.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.appointmed.databinding.ActivitySplashBinding
import com.example.appointmed.features.auth.LoginActivity

/**
 * App entry point. Shows the AppointMed brand mark for a short beat while
 * the process warms up, then hands off to LoginActivity.
 *
 * The ink background + logo are also painted immediately via
 * Theme.AppointMed.Splash's windowBackground (see themes.xml / bg_splash.xml),
 * so there's no white flash before this layout inflates.
 */
class SplashActivity : AppCompatActivity() {

    private val splashDelayMillis = 900L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }, splashDelayMillis)
    }
}
