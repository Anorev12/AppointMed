package com.example.appointmed.core.utils
import android.content.Context
import android.content.SharedPreferences
import com.example.appointmed.features.auth.models.LoginResponse
/**
 * Persists the JWT and logged-in user's details locally using
 * SharedPreferences, so the session survives app restarts.
 *
 * Usage:
 *   TokenManager(context).saveSession(loginResponse)   // after login/register
 *   TokenManager(context).getToken()                   // read JWT for API calls
 *   TokenManager(context).getUser()                    // read user for Dashboard
 *   TokenManager(context).clearSession()                // on logout
 */
class TokenManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(response: LoginResponse) {
        prefs.edit().apply {
            putString(KEY_TOKEN, response.token)
            putLong(KEY_ID, response.id)
            putString(KEY_FULL_NAME, response.fullName)
            putString(KEY_EMAIL, response.email)
            putString(KEY_CONTACT_NUMBER, response.contactNumber)
            putString(KEY_DATE_OF_BIRTH, response.dateOfBirth)
            putString(KEY_SPECIALIZATION, response.specialization)
            putString(KEY_ROLE, response.role)
            apply()
        }
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getUser(): LoginResponse? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val id = prefs.getLong(KEY_ID, -1L)
        val fullName = prefs.getString(KEY_FULL_NAME, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val role = prefs.getString(KEY_ROLE, null) ?: return null

        // contactNumber and dateOfBirth are only present for PATIENT accounts —
        // specialization is only present for DOCTOR accounts. None of these
        // three should gate the return.
        val contactNumber = prefs.getString(KEY_CONTACT_NUMBER, null) ?: ""
        val dateOfBirth = prefs.getString(KEY_DATE_OF_BIRTH, null) ?: ""
        val specialization = prefs.getString(KEY_SPECIALIZATION, null)

        return LoginResponse(
            token = token,
            id = id,
            fullName = fullName,
            email = email,
            contactNumber = contactNumber,
            dateOfBirth = dateOfBirth,
            specialization = specialization,
            role = role
        )
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "appointmed_prefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_ID = "id"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_CONTACT_NUMBER = "contact_number"
        private const val KEY_DATE_OF_BIRTH = "date_of_birth"
        private const val KEY_SPECIALIZATION = "specialization"
        private const val KEY_ROLE = "role"
    }
}