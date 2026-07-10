package com.example.appointmed.features.patient.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.features.patient.adapters.AppointmentAdapter
import com.example.appointmed.databinding.FragmentHistoryBinding
import com.example.appointmed.features.patient.models.Appointment
import com.example.appointmed.features.patient.models.toUi
import com.example.appointmed.core.network.RetrofitClient
import kotlinx.coroutines.launch

/**
 * Mirrors the React PatientDashboard's history tab — wired to the real
 * backend (/api/patient/appointments, .../{id}/cancel).
 */
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private var appointments: List<Appointment> = emptyList()
    private var cancellingId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        loadAppointments()
    }

    private fun loadAppointments() {
        binding.tvHistoryLoading.visibility = View.VISIBLE
        binding.tvHistoryError.visibility = View.GONE
        binding.tvHistoryEmpty.visibility = View.GONE
        binding.rvHistory.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getPatientApi(requireContext()).getAppointments()
                binding.tvHistoryLoading.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    appointments = response.body()!!.map { it.toUi() }
                    if (appointments.isEmpty()) {
                        binding.tvHistoryEmpty.visibility = View.VISIBLE
                    } else {
                        binding.rvHistory.visibility = View.VISIBLE
                        refreshList()
                    }
                } else {
                    binding.tvHistoryError.visibility = View.VISIBLE
                    binding.tvHistoryError.text = "Couldn't load your appointments."
                }
            } catch (e: Exception) {
                binding.tvHistoryLoading.visibility = View.GONE
                binding.tvHistoryError.visibility = View.VISIBLE
                binding.tvHistoryError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    private fun refreshList() {
        binding.rvHistory.adapter = AppointmentAdapter(appointments) { apt -> cancelAppointment(apt) }
    }

    private fun cancelAppointment(apt: Appointment) {
        if (cancellingId != null) return // already cancelling one — avoid double taps
        cancellingId = apt.dbId

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getPatientApi(requireContext()).cancelAppointment(apt.dbId)
                cancellingId = null

                if (response.isSuccessful && response.body() != null) {
                    appointments = appointments.map {
                        if (it.dbId == apt.dbId) it.copy(status = response.body()!!.status.lowercase()) else it
                    }
                    refreshList()
                } else {
                    binding.tvHistoryError.visibility = View.VISIBLE
                    binding.tvHistoryError.text = "Couldn't cancel that appointment."
                }
            } catch (e: Exception) {
                cancellingId = null
                binding.tvHistoryError.visibility = View.VISIBLE
                binding.tvHistoryError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}