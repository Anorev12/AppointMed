package com.example.appointmed.core
import android.app.Application

/**
 * Custom Application class, referenced in AndroidManifest.xml.
 * Currently minimal — reserved for future app-wide initialization
 * (e.g. logging, crash reporting) if needed.
 */
class AppointMedApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}