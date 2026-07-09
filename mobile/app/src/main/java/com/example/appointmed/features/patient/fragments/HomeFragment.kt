package com.example.appointmed.features.patient.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.appointmed.R
import com.example.appointmed.features.patient.DashboardActivity
import com.example.appointmed.databinding.FragmentHomeBinding
import com.example.appointmed.databinding.ItemStatCardBinding
import com.example.appointmed.features.patient.repository.AppointmentRepository
import com.example.appointmed.core.utils.TokenManager
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = TokenManager(requireContext()).getUser()
        binding.tvWelcome.text = "Welcome back, ${user?.fullName ?: ""}"

        bindStatCard(binding.statUpcoming, AppointmentRepository.upcoming().size.toString(), "Upcoming appointments")
        bindStatCard(binding.statTotal, AppointmentRepository.appointments.size.toString(), "Total visits on record")
        bindStatCard(binding.statDoctors, AppointmentRepository.doctors.size.toString(), "Doctors available")

        val upcoming = AppointmentRepository.upcoming()
        if (upcoming.isEmpty()) {
            binding.tvEmptyAppointment.visibility = View.VISIBLE
            binding.nextAppointmentRow.visibility = View.GONE
        } else {
            val next = upcoming.first()
            binding.tvEmptyAppointment.visibility = View.GONE
            binding.nextAppointmentRow.visibility = View.VISIBLE
            binding.tvNextDoctor.text = "${next.doctor} · ${next.specialization}"
            binding.tvNextDateTime.text = "${next.date} · ${next.time} · Ref ${next.id}"
        }

        binding.btnBookAnother.setOnClickListener {
            (requireActivity() as DashboardActivity).selectTab(R.id.nav_book)
        }
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