package com.example.appointmed.features.doctor.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.features.doctor.adapters.ScheduleAdapter
import com.example.appointmed.databinding.FragmentScheduleBinding
import com.example.appointmed.databinding.ItemStatCardBinding
import com.example.appointmed.features.doctor.models.DoctorAppointment
import com.example.appointmed.features.doctor.models.toUi
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Wired to the real backend (/api/doctor/appointments). Appointments are
 * booked as CONFIRMED by the patient; the doctor can only cancel or mark
 * them completed here — there's no "pending" state on the server, so the
 * old confirm/decline mock flow is replaced with cancel/complete.
 *
 * Network calls launch on viewLifecycleOwner.lifecycleScope so they're
 * cancelled automatically if the view is destroyed mid-request (e.g.
 * switching bottom-nav tabs before a call finishes).
 */
class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private var showTodayOnly = false
    private var appointments: List<DoctorAppointment> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvSchedule.layoutManager = LinearLayoutManager(requireContext())

        binding.btnFilterAll.setOnClickListener {
            showTodayOnly = false
            refreshList()
        }
        binding.btnFilterToday.setOnClickListener {
            showTodayOnly = true
            refreshList()
        }

        loadAppointments()
    }

    private fun loadAppointments() {
        binding.tvScheduleLoading.visibility = View.VISIBLE
        binding.tvScheduleError.visibility = View.GONE
        binding.scheduleContent.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getDoctorAppointmentApi(requireContext()).getAppointments()
                if (response.isSuccessful && response.body() != null) {
                    appointments = response.body()!!.map { it.toUi() }

                    binding.tvScheduleLoading.visibility = View.GONE
                    binding.scheduleContent.visibility = View.VISIBLE
                    refreshStats()
                    refreshList()
                } else {
                    showLoadError("Couldn't load your appointments.")
                }
            } catch (e: Exception) {
                showLoadError("Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showLoadError(message: String) {
        binding.tvScheduleLoading.visibility = View.GONE
        binding.tvScheduleError.visibility = View.VISIBLE
        binding.tvScheduleError.text = message
    }

    /** Same message slot as showLoadError, but doesn't hide an already-loaded list. */
    private fun showActionError(message: String) {
        binding.tvScheduleError.visibility = View.VISIBLE
        binding.tvScheduleError.text = message
    }

    private fun refreshStats() {
        val today = LocalDate.now().toString()
        val todayCount = appointments.count { it.date == today && it.status != "cancelled" }
        val confirmedCount = appointments.count { it.status == "confirmed" }

        bindStatCard(binding.statToday, todayCount.toString(), "Appointments today")
        bindStatCard(binding.statConfirmed, confirmedCount.toString(), "Confirmed")
        bindStatCard(binding.statTotalMonth, appointments.size.toString(), "Total this month")
    }

    private fun bindStatCard(cardBinding: ItemStatCardBinding, value: String, label: String) {
        cardBinding.tvStatValue.text = value
        cardBinding.tvStatLabel.text = label
    }

    private fun refreshList() {
        val today = LocalDate.now().toString()
        val visible = if (showTodayOnly) {
            appointments.filter { it.date == today }
        } else {
            appointments
        }

        binding.rvSchedule.adapter = ScheduleAdapter(
            visible,
            onComplete = { apt -> updateStatus(apt.dbId) { api -> api.completeAppointment(apt.dbId) } },
            onCancel = { apt -> updateStatus(apt.dbId) { api -> api.cancelAppointment(apt.dbId) } }
        )
    }

    private fun updateStatus(
        dbId: Long,
        call: suspend (com.example.appointmed.features.doctor.api.DoctorAppointmentApiService) -> retrofit2.Response<com.example.appointmed.features.doctor.models.DoctorAppointmentApiResponse>
    ) {
        binding.tvScheduleError.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = RetrofitClient.getDoctorAppointmentApi(requireContext())
                val response = call(api)
                if (response.isSuccessful && response.body() != null) {
                    val updated = response.body()!!.toUi()
                    appointments = appointments.map { if (it.dbId == dbId) updated else it }
                    refreshStats()
                    refreshList()
                } else {
                    showActionError("Couldn't update that appointment.")
                }
            } catch (e: Exception) {
                showActionError("Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
