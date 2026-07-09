package com.example.appointmed.features.admin.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.features.admin.adapters.AdminAppointmentAdapter
import com.example.appointmed.databinding.FragmentAdminAppointmentsBinding
import com.example.appointmed.features.admin.repository.AdminRepository
class AdminAppointmentsFragment : Fragment() {

    private var _binding: FragmentAdminAppointmentsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvAdminAppointments.layoutManager = LinearLayoutManager(requireContext())
        refreshList()
    }

    private fun refreshList() {
        binding.rvAdminAppointments.adapter = AdminAppointmentAdapter(
            AdminRepository.appointments,
            showDoctorColumn = true,
            onOverrideCancel = { apt ->
                AdminRepository.overrideCancel(apt.id)
                refreshList()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}