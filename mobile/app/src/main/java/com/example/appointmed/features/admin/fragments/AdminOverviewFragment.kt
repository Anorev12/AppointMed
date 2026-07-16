package com.example.appointmed.features.admin.fragments
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.databinding.DialogAdminReportsBinding
import com.example.appointmed.databinding.FragmentAdminOverviewBinding
import com.example.appointmed.databinding.ItemStatCardBinding
import com.example.appointmed.features.admin.adapters.AdminAppointmentAdapter
import com.example.appointmed.features.admin.adapters.ReportRowAdapter
import com.example.appointmed.features.admin.models.ReportRow
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
        binding.btnViewReports.setOnClickListener { showReportsDialog() }
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

    // ---------- Reports (FR-035) ----------

    private fun showReportsDialog() {
        val dialogBinding = DialogAdminReportsBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.rvApptsByStatus.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvBusiestDoctors.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvNotifStatus.layoutManager = LinearLayoutManager(requireContext())
        // These RecyclerViews sit inside a ScrollView (same pattern as rvRecentAppointments
        // above), so let the outer ScrollView own the scroll gesture.
        dialogBinding.rvApptsByStatus.isNestedScrollingEnabled = false
        dialogBinding.rvBusiestDoctors.isNestedScrollingEnabled = false
        dialogBinding.rvNotifStatus.isNestedScrollingEnabled = false

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Clinic report")
            .setView(dialogBinding.root)
            .setPositiveButton("Close", null)
            .create()
        dialog.show()

        loadReport(dialogBinding)
    }

    private fun loadReport(dialogBinding: DialogAdminReportsBinding) {
        dialogBinding.tvReportLoading.visibility = View.VISIBLE
        dialogBinding.tvReportError.visibility = View.GONE
        dialogBinding.reportContent.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext()).getReport()
                dialogBinding.tvReportLoading.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val report = response.body()!!

                    bindStatCard(dialogBinding.statTotalPatients, report.totalPatients.toString(), "Total patients")
                    bindStatCard(dialogBinding.statTotalDoctors, report.totalDoctors.toString(), "Total doctors")
                    bindStatCard(dialogBinding.statTotalAppointments, report.totalAppointments.toString(), "Total appointments")
                    bindStatCard(dialogBinding.statApptsToday, report.appointmentsToday.toString(), "Appointments today")
                    bindStatCard(dialogBinding.statApptsWeek, report.appointmentsThisWeek.toString(), "Appointments this week")
                    bindStatCard(dialogBinding.statTotalNotifications, report.totalNotifications.toString(), "Notifications logged")

                    dialogBinding.rvApptsByStatus.adapter = ReportRowAdapter(
                        report.appointmentsByStatus.map { (status, count) -> ReportRow(status, count.toString()) }
                    )

                    if (report.topDoctorsByAppointments.isEmpty()) {
                        dialogBinding.tvNoDoctorLoad.visibility = View.VISIBLE
                        dialogBinding.rvBusiestDoctors.visibility = View.GONE
                    } else {
                        dialogBinding.tvNoDoctorLoad.visibility = View.GONE
                        dialogBinding.rvBusiestDoctors.visibility = View.VISIBLE
                        dialogBinding.rvBusiestDoctors.adapter = ReportRowAdapter(
                            report.topDoctorsByAppointments.map { doctor ->
                                val subtitle = doctor.specialization?.takeIf { it.isNotBlank() }
                                val label = if (subtitle != null) "${doctor.doctorName} — $subtitle" else doctor.doctorName
                                ReportRow(label, doctor.appointmentCount.toString())
                            }
                        )
                    }

                    dialogBinding.rvNotifStatus.adapter = ReportRowAdapter(
                        report.notificationsByStatus.map { (status, count) -> ReportRow(status, count.toString()) }
                    )

                    dialogBinding.reportContent.visibility = View.VISIBLE
                } else {
                    dialogBinding.tvReportError.visibility = View.VISIBLE
                    dialogBinding.tvReportError.text = "Couldn't load the report."
                }
            } catch (e: Exception) {
                dialogBinding.tvReportLoading.visibility = View.GONE
                dialogBinding.tvReportError.visibility = View.VISIBLE
                dialogBinding.tvReportError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}