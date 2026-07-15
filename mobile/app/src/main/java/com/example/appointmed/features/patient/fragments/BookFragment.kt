package com.example.appointmed.features.patient.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.R
import com.example.appointmed.features.patient.DashboardActivity
import com.example.appointmed.features.patient.adapters.DoctorAdapter
import com.example.appointmed.features.patient.adapters.SlotAdapter
import com.example.appointmed.databinding.FragmentBookBinding
import com.example.appointmed.features.patient.models.AppointmentBookRequest
import com.example.appointmed.features.patient.models.Doctor
import com.example.appointmed.features.patient.models.toUi
import com.example.appointmed.core.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Mirrors the React PatientDashboard's Book tab — wired to the real
 * backend (/api/doctors, /api/doctors/{id}/slots, /api/patient/appointments).
 * Slot length is fixed at 30 minutes server-side; this just displays
 * whatever the backend returns.
 */
class BookFragment : Fragment() {

    private var _binding: FragmentBookBinding? = null
    private val binding get() = _binding!!

    private var selectedDoctor: Doctor? = null
    private var selectedDate: String = todayStr()
    private var selectedTime: String? = null
    private var booking = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvDoctors.layoutManager = LinearLayoutManager(requireContext())
        binding.etAppointmentDate.setText(selectedDate)
        binding.etAppointmentDate.setOnClickListener { pickDate() }

        binding.btnConfirm.setOnClickListener { confirmBooking() }
        binding.btnViewAppointments.setOnClickListener {
            (requireActivity() as DashboardActivity).selectTab(R.id.nav_history)
        }

        loadDoctors()
    }

    private fun loadDoctors() {
        binding.tvDoctorsLoading.visibility = View.VISIBLE
        binding.tvDoctorsError.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getPatientApi(requireContext()).getDoctors()
                binding.tvDoctorsLoading.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val doctors = response.body()!!.map { it.toUi() }
                    binding.rvDoctors.adapter = DoctorAdapter(doctors) { doctor ->
                        selectedDoctor = doctor
                        selectedTime = null
                        binding.dateFieldWrapper.visibility = View.VISIBLE
                        loadSlots()
                        updateSummary()
                    }
                } else {
                    binding.tvDoctorsError.visibility = View.VISIBLE
                    binding.tvDoctorsError.text = "Couldn't load the list of doctors."
                }
            } catch (e: Exception) {
                binding.tvDoctorsLoading.visibility = View.GONE
                binding.tvDoctorsError.visibility = View.VISIBLE
                binding.tvDoctorsError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    private fun pickDate() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
            binding.etAppointmentDate.setText(selectedDate)
            selectedTime = null
            if (selectedDoctor != null) loadSlots()
            updateSummary()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    private fun loadSlots() {
        val doctor = selectedDoctor ?: return

        binding.tvSlotsLabel.visibility = View.VISIBLE
        binding.tvSlotsLoading.visibility = View.VISIBLE
        binding.tvSlotsError.visibility = View.GONE
        binding.rvSlots.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getPatientApi(requireContext()).getSlots(doctor.id, selectedDate)
                binding.tvSlotsLoading.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val slots = response.body()!!
                    if (slots.isEmpty()) {
                        binding.tvSlotsError.visibility = View.VISIBLE
                        binding.tvSlotsError.text = "This doctor has no open hours on that day. Try another date."
                    } else {
                        binding.rvSlots.visibility = View.VISIBLE
                        binding.rvSlots.layoutManager = GridLayoutManager(requireContext(), 2)
                        binding.rvSlots.adapter = SlotAdapter(slots) { slot ->
                            selectedTime = slot.time
                            updateSummary()
                        }
                    }
                } else {
                    binding.tvSlotsError.visibility = View.VISIBLE
                    binding.tvSlotsError.text = "Couldn't load open slots for that day."
                }
            } catch (e: Exception) {
                binding.tvSlotsLoading.visibility = View.GONE
                binding.tvSlotsError.visibility = View.VISIBLE
                binding.tvSlotsError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    private fun updateSummary() {
        binding.tvBookingSummary.text = when {
            selectedDoctor == null -> "Select a doctor to continue."
            selectedTime == null -> "${selectedDoctor!!.name} · select a date and time"
            else -> "${selectedDoctor!!.name} · $selectedDate · ${com.example.appointmed.core.utils.formatTime12h(selectedTime)}"
        }
        binding.btnConfirm.isEnabled = selectedDoctor != null && selectedTime != null && !booking
    }

    private fun confirmBooking() {
        val doctor = selectedDoctor ?: return
        val time = selectedTime ?: return

        booking = true
        binding.btnConfirm.isEnabled = false
        binding.btnConfirm.text = "Booking…"
        binding.tvBookError.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getPatientApi(requireContext())
                    .bookAppointment(AppointmentBookRequest(doctor.id, selectedDate, time))

                booking = false
                binding.btnConfirm.text = "Confirm appointment"

                if (response.isSuccessful && response.body() != null) {
                    val appointment = response.body()!!.toUi()
                    binding.tvConfirmedRef.text = appointment.id
                    binding.confirmedTicket.visibility = View.VISIBLE
                    binding.btnConfirm.visibility = View.GONE
                } else {
                    binding.tvBookError.visibility = View.VISIBLE
                    binding.tvBookError.text = response.errorBody()?.string()
                        ?: "Couldn't book that appointment. It may have just been taken — try another slot."
                    binding.btnConfirm.isEnabled = true
                    loadSlots() // refresh in case that slot is now reserved
                }
            } catch (e: Exception) {
                booking = false
                binding.btnConfirm.isEnabled = true
                binding.btnConfirm.text = "Confirm appointment"
                binding.tvBookError.visibility = View.VISIBLE
                binding.tvBookError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun todayStr(): String {
            val calendar = Calendar.getInstance()
            return String.format(
                Locale.US, "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
}