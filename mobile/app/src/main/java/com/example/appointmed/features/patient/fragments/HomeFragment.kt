package com.example.appointmed.features.patient.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.appointmed.R
import com.example.appointmed.features.patient.DashboardActivity
import com.example.appointmed.databinding.FragmentHomeBinding
import com.example.appointmed.databinding.ItemStatCardBinding
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

                    // Feature 1: "next appointment" is the nearest upcoming CONFIRMED
                    // appointment by schedule date/time — never by booking/creation
                    // order or appointment id. date is "yyyy-MM-dd" and time is
                    // "HH:mm", both zero-padded, so plain string comparison sorts
                    // them chronologically.
                    val today = BookFragment.todayStr()
                    val upcoming = appointments
                        .filter { it.status == "confirmed" && it.date >= today }
                        .sortedWith(compareBy({ it.date }, { it.time }))

                    bindStatCard(binding.statUpcoming, upcoming.size.toString(), "Upcoming appointments")
                    bindStatCard(binding.statTotal, appointments.size.toString(), "Total visits on record")
                    bindStatCard(binding.statDoctors, doctorCount.toString(), "Doctors available")

                    if (upcoming.isEmpty()) {
                        binding.tvEmptyAppointment.visibility = View.VISIBLE
                        binding.tvEmptyAppointment.text = "You have no upcoming appointments."
                        binding.nextAppointmentRow.visibility = View.GONE
                    } else {
                        val next = upcoming.first()
                        binding.tvEmptyAppointment.visibility = View.GONE
                        binding.nextAppointmentRow.visibility = View.VISIBLE
                        binding.tvNextDoctor.text = "${next.doctor} · ${next.specialization}"
                        binding.tvNextDateTime.text = "${next.date} · ${com.example.appointmed.core.utils.formatTime12h(next.time)} · Ref ${next.id}"
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