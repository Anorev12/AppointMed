package com.example.appointmed.features.admin.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.features.admin.adapters.AdminAppointmentAdapter
import com.example.appointmed.databinding.FragmentAdminOverviewBinding
import com.example.appointmed.databinding.ItemStatCardBinding
import com.example.appointmed.features.admin.repository.AdminRepository
class AdminOverviewFragment : Fragment() {

    private var _binding: FragmentAdminOverviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindStatCard(binding.statPatients, AdminRepository.patients.size.toString(), "Registered patients")
        bindStatCard(binding.statDoctorsCount, AdminRepository.doctors.size.toString(), "Active doctors")
        bindStatCard(binding.statTodayApts, AdminRepository.todayCount().toString(), "Appointments today")
        bindStatCard(binding.statPendingApts, AdminRepository.pendingCount().toString(), "Pending requests")

        binding.rvRecentAppointments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentAppointments.adapter = AdminAppointmentAdapter(
            AdminRepository.appointments.take(4),
            showDoctorColumn = true,
            onOverrideCancel = null // read-only preview on Overview
        )
    }

    private fun bindStatCard(cardBinding: ItemStatCardBinding, value: String, label: String) {
        cardBinding.tvStatValue.text = value
        cardBinding.tvStatLabel.text = label
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}