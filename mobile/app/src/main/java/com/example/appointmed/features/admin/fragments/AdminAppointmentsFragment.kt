package com.example.appointmed.features.admin.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.databinding.FragmentAdminAppointmentsBinding
import com.example.appointmed.features.admin.adapters.AdminAppointmentAdapter
import com.example.appointmed.features.admin.models.AdminAppointment
import com.example.appointmed.features.admin.models.toUi
import kotlinx.coroutines.launch

/** Wired to /api/admin/appointments (clinic-wide) and the override-cancel endpoint. */
class AdminAppointmentsFragment : Fragment() {

    private var _binding: FragmentAdminAppointmentsBinding? = null
    private val binding get() = _binding!!

    private var appointments: List<AdminAppointment> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvAdminAppointments.layoutManager = LinearLayoutManager(requireContext())
        loadAppointments()
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
        binding.rvAdminAppointments.adapter = AdminAppointmentAdapter(
            appointments,
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
