package com.example.appointmed.features.doctor.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.features.doctor.adapters.ScheduleAdapter
import com.example.appointmed.databinding.FragmentScheduleBinding
import com.example.appointmed.databinding.ItemStatCardBinding
import com.example.appointmed.features.doctor.repository.DoctorAppointmentRepository
class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private var showTodayOnly = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvSchedule.layoutManager = LinearLayoutManager(requireContext())

        bindStatCard(binding.statToday, DoctorAppointmentRepository.todayCount().toString(), "Appointments today")
        bindStatCard(binding.statPending, DoctorAppointmentRepository.pendingCount().toString(), "Pending confirmation")
        bindStatCard(binding.statTotalMonth, DoctorAppointmentRepository.appointments.size.toString(), "Total this month")

        binding.btnFilterAll.setOnClickListener {
            showTodayOnly = false
            refreshList()
        }
        binding.btnFilterToday.setOnClickListener {
            showTodayOnly = true
            refreshList()
        }

        refreshList()
    }

    private fun bindStatCard(cardBinding: ItemStatCardBinding, value: String, label: String) {
        cardBinding.tvStatValue.text = value
        cardBinding.tvStatLabel.text = label
    }

    private fun refreshList() {
        val source = DoctorAppointmentRepository.appointments
        val visible = if (showTodayOnly) {
            source.filter { it.date == DoctorAppointmentRepository.TODAY }
        } else {
            source
        }

        binding.rvSchedule.adapter = ScheduleAdapter(
            visible,
            onConfirm = { apt ->
                DoctorAppointmentRepository.updateStatus(apt.id, "confirmed")
                refreshList()
            },
            onDecline = { apt ->
                DoctorAppointmentRepository.updateStatus(apt.id, "cancelled")
                refreshList()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}