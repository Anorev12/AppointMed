package com.example.appointmed.features.patient.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.appointmed.R
import com.example.appointmed.features.patient.DashboardActivity
import com.example.appointmed.databinding.FragmentHomeBinding
import com.example.appointmed.databinding.ItemNextAppointmentRowBinding
import com.example.appointmed.databinding.ItemStatCardBinding
import com.example.appointmed.features.patient.models.Appointment
import com.example.appointmed.features.patient.models.toUi
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.core.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * Mirrors the React PatientDashboard's home view — wired to the real
 * backend (/api/patient/appointments, /api/doctors). Network calls launch
 * on viewLifecycleOwner.lifecycleScope so they're cancelled automatically
 * if the fragment's view is destroyed mid-request.
 */
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

        binding.btnBookAnother.setOnClickListener {
            (requireActivity() as DashboardActivity).selectTab(R.id.nav_book)
        }

        loadDashboard()
    }

    private fun loadDashboard() {
        binding.tvHomeLoading.visibility = View.VISIBLE
        binding.tvHomeError.visibility = View.GONE
        binding.homeContent.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = RetrofitClient.getPatientApi(requireContext())
                val apptsResponse = api.getAppointments()
                val doctorsResponse = api.getDoctors()

                if (apptsResponse.isSuccessful && apptsResponse.body() != null &&
                    doctorsResponse.isSuccessful && doctorsResponse.body() != null
                ) {
                    val appointments = apptsResponse.body()!!.map { it.toUi() }
                    val doctorCount = doctorsResponse.body()!!.size

                    // Feature 1: the "Next appointment(s)" panel lists every upcoming
                    // CONFIRMED appointment (not just the nearest one), ordered by
                    // schedule date/time — never by booking/creation order or
                    // appointment id. date is "yyyy-MM-dd" and time is "HH:mm", both
                    // zero-padded, so plain string comparison sorts them chronologically.
                    val today = BookFragment.todayStr()
                    val upcoming = appointments
                        .filter { it.status == "confirmed" && it.date >= today }
                        .sortedWith(compareBy({ it.date }, { it.time }))

                    bindStatCard(binding.statUpcoming, upcoming.size.toString(), "Upcoming appointments")
                    bindStatCard(binding.statTotal, appointments.size.toString(), "Total visits on record")
                    bindStatCard(binding.statDoctors, doctorCount.toString(), "Doctors available")

                    binding.tvNextAppointmentTitle.text = if (upcoming.size > 1) "Next appointments" else "Next appointment"

                    if (upcoming.isEmpty()) {
                        binding.tvEmptyAppointment.visibility = View.VISIBLE
                        binding.tvEmptyAppointment.text = "You have no upcoming appointments."
                        binding.nextAppointmentsContainer.visibility = View.GONE
                    } else {
                        binding.tvEmptyAppointment.visibility = View.GONE
                        binding.nextAppointmentsContainer.visibility = View.VISIBLE
                        binding.nextAppointmentsContainer.removeAllViews()
                        upcoming.forEach { appt -> addNextAppointmentRow(appt) }
                    }

                    binding.tvHomeLoading.visibility = View.GONE
                    binding.homeContent.visibility = View.VISIBLE
                } else {
                    showError("Couldn't load your dashboard.")
                }
            } catch (e: Exception) {
                showError("Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun addNextAppointmentRow(appt: Appointment) {
        val rowBinding = ItemNextAppointmentRowBinding.inflate(
            LayoutInflater.from(requireContext()), binding.nextAppointmentsContainer, false
        )
        val context = rowBinding.root.context

        rowBinding.tvNextDoctor.text = "${appt.doctor} · ${appt.specialization}"
        rowBinding.tvNextDateTime.text =
            "${appt.date} · ${com.example.appointmed.core.utils.formatTime12h(appt.time)} · Ref ${appt.id}"

        if (appt.needsReschedule) {
            rowBinding.tvNextStatusBadge.text = "⚠ Needs reschedule"
            rowBinding.tvNextStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.amber_soft))
            rowBinding.tvNextStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.amber))
        } else {
            rowBinding.tvNextStatusBadge.text = "Confirmed"
            rowBinding.tvNextStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.success_soft))
            rowBinding.tvNextStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.success))
        }

        binding.nextAppointmentsContainer.addView(rowBinding.root)
    }

    private fun showError(message: String) {
        binding.tvHomeLoading.visibility = View.GONE
        binding.tvHomeError.visibility = View.VISIBLE
        binding.tvHomeError.text = message
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