package com.example.appointmed.features.patient.fragments
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.R
import com.example.appointmed.features.patient.adapters.AppointmentAdapter
import com.example.appointmed.features.patient.adapters.SlotAdapter
import com.example.appointmed.databinding.DialogRescheduleBinding
import com.example.appointmed.databinding.FragmentHistoryBinding
import com.example.appointmed.features.patient.models.Appointment
import com.example.appointmed.features.patient.models.AppointmentRescheduleRequest
import com.example.appointmed.features.patient.models.toUi
import com.example.appointmed.core.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Mirrors the React PatientDashboard's history tab — wired to the real
 * backend (/api/patient/appointments with search filters, .../{id}/cancel,
 * .../{id}/reschedule).
 *
 * FR-012 (searchable history): free-text search box + status filter row,
 * both sent to the backend as query params so filtering happens server-side
 * against the full dataset, not just whatever page happened to be loaded.
 *
 * FR-011 (reschedule): tapping "Reschedule" on a confirmed appointment opens
 * a dialog that reuses the same date-picker + open-slots flow as booking.
 */
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private var appointments: List<Appointment> = emptyList()
    private var cancellingId: Long? = null
    private var reschedulingId: Long? = null

    // "" means no status filter ("All")
    private var statusFilter: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())

        binding.etHistorySearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                loadAppointments()
                true
            } else {
                false
            }
        }

        binding.btnFilterAll.setOnClickListener { applyStatusFilter("") }
        binding.btnFilterConfirmed.setOnClickListener { applyStatusFilter("CONFIRMED") }
        binding.btnFilterCancelled.setOnClickListener { applyStatusFilter("CANCELLED") }
        binding.btnFilterCompleted.setOnClickListener { applyStatusFilter("COMPLETED") }
        highlightActiveFilter()

        loadAppointments()
    }

    private fun applyStatusFilter(status: String) {
        statusFilter = status
        highlightActiveFilter()
        loadAppointments()
    }

    private fun highlightActiveFilter() {
        val buttons = mapOf(
            "" to binding.btnFilterAll,
            "CONFIRMED" to binding.btnFilterConfirmed,
            "CANCELLED" to binding.btnFilterCancelled,
            "COMPLETED" to binding.btnFilterCompleted
        )
        buttons.forEach { (status, button) ->
            val active = status == statusFilter
            button.setBackgroundColor(
                androidx.core.content.ContextCompat.getColor(
                    requireContext(), if (active) R.color.stamp else R.color.paper_card
                )
            )
            button.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    requireContext(), if (active) android.R.color.white else R.color.ink
                )
            )
        }
    }

    private fun loadAppointments() {
        binding.tvHistoryLoading.visibility = View.VISIBLE
        binding.tvHistoryError.visibility = View.GONE
        binding.tvHistoryEmpty.visibility = View.GONE
        binding.rvHistory.visibility = View.GONE

        val keyword = binding.etHistorySearch.text?.toString()?.trim().orEmpty()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getPatientApi(requireContext()).getAppointments(
                    status = statusFilter.ifBlank { null },
                    keyword = keyword.ifBlank { null }
                )
                binding.tvHistoryLoading.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    appointments = response.body()!!.map { it.toUi() }
                    if (appointments.isEmpty()) {
                        binding.tvHistoryEmpty.visibility = View.VISIBLE
                        binding.tvHistoryEmpty.text = if (statusFilter.isBlank() && keyword.isBlank())
                            "You haven't booked any appointments yet."
                        else
                            "No appointments match your search."
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
        binding.rvHistory.adapter = AppointmentAdapter(
            appointments,
            onCancel = { apt -> cancelAppointment(apt) },
            onReschedule = { apt -> openRescheduleDialog(apt) }
        )
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
                    binding.tvHistoryError.text = response.errorBody()?.string()
                        ?: "Couldn't cancel that appointment."
                }
            } catch (e: Exception) {
                cancellingId = null
                binding.tvHistoryError.visibility = View.VISIBLE
                binding.tvHistoryError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    /** FR-011: reschedule dialog — pick a new date, reload that day's open slots, confirm the move. */
    private fun openRescheduleDialog(apt: Appointment) {
        val dialogBinding = DialogRescheduleBinding.inflate(LayoutInflater.from(requireContext()))
        var pickedDate = apt.date
        var pickedTime: String? = null

        dialogBinding.tvRescheduleCurrent.text = "Currently: ${apt.doctor} · ${apt.date} · ${apt.time}"
        dialogBinding.etRescheduleDate.setText(pickedDate)
        dialogBinding.rvRescheduleSlots.layoutManager = GridLayoutManager(requireContext(), 2)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        fun loadSlotsFor(date: String) {
            dialogBinding.tvRescheduleSlotsLoading.visibility = View.VISIBLE
            dialogBinding.tvRescheduleSlotsError.visibility = View.GONE
            dialogBinding.rvRescheduleSlots.visibility = View.GONE
            dialogBinding.btnRescheduleConfirm.isEnabled = false
            pickedTime = null

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.getPatientApi(requireContext()).getSlots(apt.doctorId, date)
                    dialogBinding.tvRescheduleSlotsLoading.visibility = View.GONE

                    if (response.isSuccessful && response.body() != null) {
                        val slots = response.body()!!
                        if (slots.isEmpty()) {
                            dialogBinding.tvRescheduleSlotsError.visibility = View.VISIBLE
                            dialogBinding.tvRescheduleSlotsError.text =
                                "This doctor has no open hours on that day. Try another date."
                        } else {
                            dialogBinding.rvRescheduleSlots.visibility = View.VISIBLE
                            dialogBinding.rvRescheduleSlots.adapter = SlotAdapter(slots) { slot ->
                                pickedTime = slot.time
                                dialogBinding.btnRescheduleConfirm.isEnabled = true
                            }
                        }
                    } else {
                        dialogBinding.tvRescheduleSlotsError.visibility = View.VISIBLE
                        dialogBinding.tvRescheduleSlotsError.text = "Couldn't load open slots for that day."
                    }
                } catch (e: Exception) {
                    dialogBinding.tvRescheduleSlotsLoading.visibility = View.GONE
                    dialogBinding.tvRescheduleSlotsError.visibility = View.VISIBLE
                    dialogBinding.tvRescheduleSlotsError.text = "Can't reach the server. Try again."
                }
            }
        }

        dialogBinding.etRescheduleDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                pickedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                dialogBinding.etRescheduleDate.setText(pickedDate)
                loadSlotsFor(pickedDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.minDate = System.currentTimeMillis() - 1000
            }.show()
        }

        dialogBinding.btnRescheduleCancel.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnRescheduleConfirm.setOnClickListener {
            val time = pickedTime ?: return@setOnClickListener
            if (reschedulingId != null) return@setOnClickListener
            reschedulingId = apt.dbId

            dialogBinding.btnRescheduleConfirm.isEnabled = false
            dialogBinding.btnRescheduleConfirm.text = "Moving…"
            dialogBinding.tvRescheduleError.visibility = View.GONE

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.getPatientApi(requireContext())
                        .rescheduleAppointment(apt.dbId, AppointmentRescheduleRequest(pickedDate, time))

                    reschedulingId = null
                    dialogBinding.btnRescheduleConfirm.text = "Confirm move"

                    if (response.isSuccessful && response.body() != null) {
                        val updated = response.body()!!.toUi()
                        appointments = appointments.map { if (it.dbId == apt.dbId) updated else it }
                        refreshList()
                        dialog.dismiss()
                    } else {
                        dialogBinding.tvRescheduleError.visibility = View.VISIBLE
                        dialogBinding.tvRescheduleError.text = response.errorBody()?.string()
                            ?: "Couldn't reschedule that appointment."
                        dialogBinding.btnRescheduleConfirm.isEnabled = true
                    }
                } catch (e: Exception) {
                    reschedulingId = null
                    dialogBinding.btnRescheduleConfirm.isEnabled = true
                    dialogBinding.btnRescheduleConfirm.text = "Confirm move"
                    dialogBinding.tvRescheduleError.visibility = View.VISIBLE
                    dialogBinding.tvRescheduleError.text = "Can't reach the server. Check that it's running and try again."
                }
            }
        }

        dialog.show()
        loadSlotsFor(pickedDate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
