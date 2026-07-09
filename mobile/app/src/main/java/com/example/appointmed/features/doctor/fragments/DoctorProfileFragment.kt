package com.example.appointmed.features.doctor.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.appointmed.features.doctor.DoctorDashboardActivity
import com.example.appointmed.databinding.FragmentDoctorProfileBinding
import com.example.appointmed.core.utils.TokenManager
/**
 * Doctor's profile tab — read-only account info plus logout. No PATCH
 * endpoint exists for doctor profile updates yet, same placeholder
 * pattern as the patient ProfileFragment.
 */
class DoctorProfileFragment : Fragment() {

    private var _binding: FragmentDoctorProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = TokenManager(requireContext()).getUser()
        binding.etDoctorFullName.setText(user?.fullName)
        binding.etDoctorEmail.setText(user?.email)
        binding.etDoctorSpecialization.setText(user?.specialization)

        binding.btnDoctorLogout.setOnClickListener {
            (requireActivity() as DoctorDashboardActivity).logout()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}