package com.example.appointmed.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.appointmed.activities.DashboardActivity
import com.example.appointmed.databinding.FragmentProfileBinding
import com.example.appointmed.utils.TokenManager

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

        val user = TokenManager(requireContext()).getUser()
        binding.etFullName.setText(user?.fullName)
        binding.etEmail.setText(user?.email)
        binding.etContact.setText(user?.contactNumber)
        binding.etDob.setText(user?.dateOfBirth)

        binding.btnSaveProfile.setOnClickListener {
            // TODO: no PATCH/PUT /api/patient/profile endpoint exists yet —
            // wire this up once the backend supports profile updates.
            Toast.makeText(requireContext(), "Profile updates aren't available yet.", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            (requireActivity() as DashboardActivity).logout()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}