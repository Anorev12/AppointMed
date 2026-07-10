package com.example.appointmed.features.patient.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.appointmed.features.patient.DashboardActivity
import com.example.appointmed.databinding.FragmentProfileBinding
import com.example.appointmed.features.patient.models.PatientProfileUpdateRequest
import com.example.appointmed.core.network.RetrofitClient
import kotlinx.coroutines.launch

/**
 * Mirrors the React PatientDashboard's profile tab — wired to the real
 * backend (/api/patient/profile).
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSaveProfile.setOnClickListener { saveProfile() }
        binding.btnLogout.setOnClickListener {
            (requireActivity() as DashboardActivity).logout()
        }

        loadProfile()
    }

    private fun loadProfile() {
        binding.tvProfileLoading.visibility = View.VISIBLE
        binding.tvProfileError.visibility = View.GONE
        binding.profileContent.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getPatientApi(requireContext()).getProfile()
                binding.tvProfileLoading.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    binding.etFullName.setText(profile.fullName)
                    binding.etEmail.setText(profile.email)
                    binding.etContact.setText(profile.contactNumber ?: "")
                    binding.etDob.setText(profile.dateOfBirth ?: "")
                    binding.etMedicalHistory.setText(profile.medicalHistory ?: "")
                    binding.profileContent.visibility = View.VISIBLE
                } else {
                    binding.tvProfileError.visibility = View.VISIBLE
                    binding.tvProfileError.text = "Couldn't load your profile."
                }
            } catch (e: Exception) {
                binding.tvProfileLoading.visibility = View.GONE
                binding.tvProfileError.visibility = View.VISIBLE
                binding.tvProfileError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    private fun saveProfile() {
        val fullName = binding.etFullName.text.toString()
        if (fullName.isBlank()) {
            showSaveMessage("Full name can't be empty.")
            return
        }

        binding.btnSaveProfile.isEnabled = false
        binding.btnSaveProfile.text = "Saving…"

        val request = PatientProfileUpdateRequest(
            fullName = fullName,
            contactNumber = binding.etContact.text.toString(),
            medicalHistory = binding.etMedicalHistory.text.toString()
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getPatientApi(requireContext()).updateProfile(request)

                binding.btnSaveProfile.isEnabled = true
                binding.btnSaveProfile.text = "Save changes"

                if (response.isSuccessful && response.body() != null) {
                    showSaveMessage("Saved.")
                } else {
                    showSaveMessage("Couldn't save your profile.")
                }
            } catch (e: Exception) {
                binding.btnSaveProfile.isEnabled = true
                binding.btnSaveProfile.text = "Save changes"
                showSaveMessage("Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showSaveMessage(message: String) {
        binding.tvProfileSaveMessage.text = message
        binding.tvProfileSaveMessage.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}