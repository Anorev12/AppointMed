package com.example.appointmed.features.admin.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.appointmed.features.admin.AdminDashboardActivity
import com.example.appointmed.databinding.FragmentAdminProfileBinding
import com.example.appointmed.core.utils.TokenManager
class AdminProfileFragment : Fragment() {

    private var _binding: FragmentAdminProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = TokenManager(requireContext()).getUser()
        binding.etAdminFullName.setText(user?.fullName)
        binding.etAdminEmail.setText(user?.email)

        binding.btnAdminLogout.setOnClickListener {
            (requireActivity() as AdminDashboardActivity).logout()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}