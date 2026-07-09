package com.example.appointmed.features.doctor.fragments
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.features.doctor.adapters.UnavailableDateAdapter
import com.example.appointmed.databinding.FragmentAvailabilityBinding
import com.example.appointmed.features.doctor.models.UnavailableDateRequest
import com.example.appointmed.features.doctor.models.UpdateScheduleRequest
import com.example.appointmed.core.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Mirrors the React DoctorDashboard's Availability tab — wired to the
 * real backend (/api/doctor/availability). The appointment list on the
 * Schedule tab is still mock data; this tab is fully live.
 *
 * All network calls launch on viewLifecycleOwner.lifecycleScope (not
 * lifecycleScope) so they're automatically cancelled if the view is
 * destroyed mid-request (e.g. switching bottom-nav tabs before a call
 * finishes) — otherwise a completed/cancelled coroutine can try to touch
 * `binding` after it's already null, causing a NullPointerException.
 */
class AvailabilityFragment : Fragment() {

    private var _binding: FragmentAvailabilityBinding? = null
    private val binding get() = _binding!!

    private val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private var unavailableDates = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAvailabilityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvUnavailableDates.layoutManager = LinearLayoutManager(requireContext())

        binding.etStartTime.setOnClickListener { pickTime(binding.etStartTime) }
        binding.etEndTime.setOnClickListener { pickTime(binding.etEndTime) }
        binding.etNewUnavailableDate.setOnClickListener { pickDate() }

        binding.btnSaveSchedule.setOnClickListener { saveSchedule() }
        binding.btnAddUnavailableDate.setOnClickListener { addUnavailableDate() }

        loadAvailability()
    }

    private fun dayCheckbox(day: String): CheckBox = when (day) {
        "Mon" -> binding.cbMon
        "Tue" -> binding.cbTue
        "Wed" -> binding.cbWed
        "Thu" -> binding.cbThu
        "Fri" -> binding.cbFri
        "Sat" -> binding.cbSat
        else -> binding.cbSun
    }

    private fun loadAvailability() {
        binding.tvAvailLoading.visibility = View.VISIBLE
        binding.tvAvailError.visibility = View.GONE
        binding.availabilityContent.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAvailabilityApi(requireContext()).getAvailability()
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    weekdays.forEach { day -> dayCheckbox(day).isChecked = data.workingDays.contains(day) }
                    binding.etStartTime.setText(data.startTime)
                    binding.etEndTime.setText(data.endTime)
                    unavailableDates = data.unavailableDates.toMutableList()
                    refreshDatesList()

                    binding.tvAvailLoading.visibility = View.GONE
                    binding.availabilityContent.visibility = View.VISIBLE
                } else {
                    showLoadError("Couldn't load your availability.")
                }
            } catch (e: Exception) {
                showLoadError("Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showLoadError(message: String) {
        binding.tvAvailLoading.visibility = View.GONE
        binding.tvAvailError.visibility = View.VISIBLE
        binding.tvAvailError.text = message
    }

    private fun pickTime(target: com.google.android.material.textfield.TextInputEditText) {
        val calendar = Calendar.getInstance()
        val current = target.text.toString()
        var hour = calendar.get(Calendar.HOUR_OF_DAY)
        var minute = calendar.get(Calendar.MINUTE)
        if (current.contains(":")) {
            val parts = current.split(":")
            hour = parts[0].toIntOrNull() ?: hour
            minute = parts[1].toIntOrNull() ?: minute
        }

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            target.setText(String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute))
            binding.tvSaveMessage.visibility = View.GONE
        }, hour, minute, true).show()
    }

    private fun pickDate() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
            binding.etNewUnavailableDate.setText(date)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveSchedule() {
        val selectedDays = weekdays.filter { dayCheckbox(it).isChecked }
        val start = binding.etStartTime.text.toString()
        val end = binding.etEndTime.text.toString()

        if (selectedDays.isEmpty()) {
            showSaveMessage("Select at least one working day.")
            return
        }
        if (start.isEmpty() || end.isEmpty() || start >= end) {
            showSaveMessage("Start time must be before end time.")
            return
        }

        binding.btnSaveSchedule.isEnabled = false
        binding.btnSaveSchedule.text = "Saving…"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAvailabilityApi(requireContext())
                    .updateSchedule(UpdateScheduleRequest(selectedDays, start, end))

                binding.btnSaveSchedule.isEnabled = true
                binding.btnSaveSchedule.text = "Save schedule"

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    weekdays.forEach { day -> dayCheckbox(day).isChecked = data.workingDays.contains(day) }
                    binding.etStartTime.setText(data.startTime)
                    binding.etEndTime.setText(data.endTime)
                    showSaveMessage("Schedule saved.")
                } else {
                    showSaveMessage("Couldn't save schedule.")
                }
            } catch (e: Exception) {
                binding.btnSaveSchedule.isEnabled = true
                binding.btnSaveSchedule.text = "Save schedule"
                showSaveMessage("Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showSaveMessage(message: String) {
        binding.tvSaveMessage.text = message
        binding.tvSaveMessage.visibility = View.VISIBLE
    }

    private fun addUnavailableDate() {
        val date = binding.etNewUnavailableDate.text.toString()
        if (date.isEmpty()) return

        binding.tvDateActionError.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAvailabilityApi(requireContext())
                    .addUnavailableDate(UnavailableDateRequest(date))

                if (response.isSuccessful && response.body() != null) {
                    unavailableDates = response.body()!!.unavailableDates.toMutableList()
                    binding.etNewUnavailableDate.setText("")
                    refreshDatesList()
                } else {
                    showDateError("Couldn't add that date.")
                }
            } catch (e: Exception) {
                showDateError("Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun removeUnavailableDate(date: String) {
        binding.tvDateActionError.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAvailabilityApi(requireContext())
                    .removeUnavailableDate(date)

                if (response.isSuccessful && response.body() != null) {
                    unavailableDates = response.body()!!.unavailableDates.toMutableList()
                    refreshDatesList()
                } else {
                    showDateError("Couldn't remove that date.")
                }
            } catch (e: Exception) {
                showDateError("Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showDateError(message: String) {
        binding.tvDateActionError.text = message
        binding.tvDateActionError.visibility = View.VISIBLE
    }

    private fun refreshDatesList() {
        binding.tvNoUnavailableDates.visibility = if (unavailableDates.isEmpty()) View.VISIBLE else View.GONE
        binding.rvUnavailableDates.adapter = UnavailableDateAdapter(unavailableDates) { date ->
            removeUnavailableDate(date)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}