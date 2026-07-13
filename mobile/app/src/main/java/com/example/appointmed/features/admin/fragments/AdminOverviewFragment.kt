package com.example.appointmed.features.admin.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.databinding.FragmentAdminOverviewBinding
import com.example.appointmed.databinding.ItemStatCardBinding
import com.example.appointmed.features.admin.adapters.AdminAppointmentAdapter
import com.example.appointmed.features.admin.models.toUi
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Wired to /api/admin/patients, /api/admin/doctors and /api/admin/appointments. */
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
        binding.rvRecentAppointments.layoutManager = LinearLayoutManager(requireContext())
        loadOverview()
    }

    private fun loadOverview() {
        binding.tvAdminOverviewLoading.visibility = View.VISIBLE
        binding.tvAdminOverviewError.visibility = View.GONE
        binding.overviewContent.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = RetrofitClient.getAdminApi(requireContext())
                val patientsResponse = api.listPatients()
                val doctorsResponse = api.listDoctors()
                val appointmentsResponse = api.listAppointments()

                if (patientsResponse.isSuccessful && doctorsResponse.isSuccessful && appointmentsResponse.isSuccessful &&
                    patientsResponse.body() != null && doctorsResponse.body() != null && appointmentsResponse.body() != null
                ) {
                    val patients = patientsResponse.body()!!
                    val doctors = doctorsResponse.body()!!.map { it.toUi() }
                    val appointments = appointmentsResponse.body()!!.map { it.toUi() }

                    val today = LocalDate.now().toString()
                    val todayCount = appointments.count { it.date == today && it.status != "cancelled" }
                    val completedCount = appointments.count { it.status == "completed" }

                    bindStatCard(binding.statPatients, patients.size.toString(), "Registered patients")
                    bindStatCard(binding.statDoctorsCount, doctors.count { it.status == "active" }.toString(), "Active doctors")
                    bindStatCard(binding.statTodayApts, todayCount.toString(), "Appointments today")
                    bindStatCard(binding.statCompletedApts, completedCount.toString(), "Completed appointments")

                    binding.rvRecentAppointments.adapter = AdminAppointmentAdapter(
                        appointments.take(4),
                        showDoctorColumn = true,
                        onOverrideCancel = null // read-only preview on Overview
                    )

                    binding.tvAdminOverviewLoading.visibility = View.GONE
                    binding.overviewContent.visibility = View.VISIBLE
                } else {
                    showLoadError("Couldn't load the clinic overview.")
                }
            } catch (e: Exception) {
                showLoadError("Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showLoadError(message: String) {
        binding.tvAdminOverviewLoading.visibility = View.GONE
        binding.tvAdminOverviewError.visibility = View.VISIBLE
        binding.tvAdminOverviewError.text = message
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
