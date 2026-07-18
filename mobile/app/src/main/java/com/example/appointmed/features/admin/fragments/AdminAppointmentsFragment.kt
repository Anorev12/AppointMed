package com.example.appointmed.features.admin.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.R
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.databinding.FragmentAdminAppointmentsBinding
import com.example.appointmed.features.admin.adapters.AdminAppointmentAdapter
import com.example.appointmed.features.admin.models.AdminAppointment
import com.example.appointmed.features.admin.models.toUi
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Wired to /api/admin/appointments (clinic-wide) and the override-cancel endpoint. */
class AdminAppointmentsFragment : Fragment() {

    private var _binding: FragmentAdminAppointmentsBinding? = null
    private val binding get() = _binding!!

    private var appointments: List<AdminAppointment> = emptyList()
    private var currentFilter = Filter.ALL

    private enum class Filter { ALL, CONFIRMED, CANCELLED, TODAY, UPCOMING }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvAdminAppointments.layoutManager = LinearLayoutManager(requireContext())

        binding.btnFilterAll.setOnClickListener { setFilter(Filter.ALL) }
        binding.btnFilterConfirmed.setOnClickListener { setFilter(Filter.CONFIRMED) }
        binding.btnFilterCancelled.setOnClickListener { setFilter(Filter.CANCELLED) }
        binding.btnFilterToday.setOnClickListener { setFilter(Filter.TODAY) }
        binding.btnFilterUpcoming.setOnClickListener { setFilter(Filter.UPCOMING) }
        updateFilterButtonStyles()

        loadAppointments()
    }

    private fun setFilter(filter: Filter) {
        currentFilter = filter
        updateFilterButtonStyles()
        refreshList()
    }

    private fun updateFilterButtonStyles() {
        val buttons = listOf(
            Filter.ALL to binding.btnFilterAll,
            Filter.CONFIRMED to binding.btnFilterConfirmed,
            Filter.CANCELLED to binding.btnFilterCancelled,
            Filter.TODAY to binding.btnFilterToday,
            Filter.UPCOMING to binding.btnFilterUpcoming
        )
        val context = requireContext()
        for ((filter, button: Button) in buttons) {
            val active = filter == currentFilter
            button.backgroundTintList = ContextCompat.getColorStateList(
                context, if (active) R.color.stamp else R.color.border
            )
            button.setTextColor(
                ContextCompat.getColor(context, if (active) R.color.paper_card else R.color.ink)
            )
        }
    }

    private fun loadAppointments() {
        binding.tvAdminAppointmentsLoading.visibility = View.VISIBLE
        binding.tvAdminAppointmentsError.visibility = View.GONE
        binding.rvAdminAppointments.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext()).listAppointments()
                if (response.isSuccessful && response.body() != null) {
                    appointments = response.body()!!.map { it.toUi() }
                    binding.tvAdminAppointmentsLoading.visibility = View.GONE
                    binding.rvAdminAppointments.visibility = View.VISIBLE
                    refreshList()
                } else {
                    showLoadError("Couldn't load appointments.")
                }
            } catch (e: Exception) {
                showLoadError("Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showLoadError(message: String) {
        binding.tvAdminAppointmentsLoading.visibility = View.GONE
        binding.tvAdminAppointmentsError.visibility = View.VISIBLE
        binding.tvAdminAppointmentsError.text = message
    }

    private fun refreshList() {
        val today = LocalDate.now().toString()
        val visible = when (currentFilter) {
            Filter.CONFIRMED -> appointments.filter { it.status == "confirmed" }
            Filter.CANCELLED -> appointments.filter { it.status == "cancelled" }
            Filter.TODAY -> appointments.filter { it.date == today }
            Filter.UPCOMING -> appointments
                .filter { it.date >= today && it.status == "confirmed" }
                .sortedWith(compareBy({ it.date }, { it.time }))
            Filter.ALL -> appointments
        }

        binding.rvAdminAppointments.adapter = AdminAppointmentAdapter(
            visible,
            showDoctorColumn = true,
            onOverrideCancel = { apt -> overrideCancel(apt.dbId) }
        )
    }

    private fun overrideCancel(dbId: Long) {
        binding.tvAdminAppointmentsError.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext()).overrideCancel(dbId)
                if (response.isSuccessful && response.body() != null) {
                    val updated = response.body()!!.toUi()
                    appointments = appointments.map { if (it.dbId == dbId) updated else it }
                    refreshList()
                } else {
                    binding.tvAdminAppointmentsError.visibility = View.VISIBLE
                    binding.tvAdminAppointmentsError.text = "Couldn't cancel that appointment."
                }
            } catch (e: Exception) {
                binding.tvAdminAppointmentsError.visibility = View.VISIBLE
                binding.tvAdminAppointmentsError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}