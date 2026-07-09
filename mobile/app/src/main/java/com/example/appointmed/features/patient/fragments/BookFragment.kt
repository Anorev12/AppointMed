package com.example.appointmed.features.patient.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.R
import com.example.appointmed.features.patient.DashboardActivity
import com.example.appointmed.features.patient.adapters.DoctorAdapter
import com.example.appointmed.features.patient.adapters.SlotAdapter
import com.example.appointmed.databinding.FragmentBookBinding
import com.example.appointmed.features.patient.repository.AppointmentRepository
import com.example.appointmed.features.patient.models.Doctor
class BookFragment : Fragment() {

    private var _binding: FragmentBookBinding? = null
    private val binding get() = _binding!!

    private var selectedDoctor: Doctor? = null
    private var selectedTime: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvDoctors.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDoctors.adapter = DoctorAdapter(AppointmentRepository.doctors) { doctor ->
            selectedDoctor = doctor
            selectedTime = null
            showSlotsFor(doctor)
            updateSummary()
        }

        binding.btnConfirm.setOnClickListener { confirmBooking() }
        binding.btnViewAppointments.setOnClickListener {
            (requireActivity() as DashboardActivity).selectTab(R.id.nav_history)
        }
    }

    private fun showSlotsFor(doctor: Doctor) {
        binding.tvSlotsLabel.visibility = View.VISIBLE
        binding.rvSlots.visibility = View.VISIBLE
        binding.rvSlots.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvSlots.adapter = SlotAdapter(AppointmentRepository.slotsFor(doctor.id)) { slot ->
            selectedTime = slot.time
            updateSummary()
        }
    }

    private fun updateSummary() {
        binding.tvBookingSummary.text = when {
            selectedDoctor == null -> "Select a doctor to continue."
            selectedTime == null -> "${selectedDoctor!!.name} · select a time"
            else -> "${selectedDoctor!!.name} · $selectedTime"
        }
        binding.btnConfirm.isEnabled = selectedDoctor != null && selectedTime != null
    }

    private fun confirmBooking() {
        val doctor = selectedDoctor ?: return
        val time = selectedTime ?: return

        val appointment = AppointmentRepository.book(doctor.id, time)

        binding.tvConfirmedRef.text = appointment.id
        binding.confirmedTicket.visibility = View.VISIBLE
        binding.btnConfirm.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}