package com.example.appointmed.features.admin.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.databinding.DialogAddDoctorBinding
import com.example.appointmed.databinding.DialogManageDoctorAvailabilityBinding
import com.example.appointmed.databinding.FragmentAdminDoctorsBinding
import com.example.appointmed.features.admin.adapters.AdminDoctorAdapter
import com.example.appointmed.features.admin.models.AdminDoctor
import com.example.appointmed.features.admin.models.DoctorCreateRequest
import com.example.appointmed.features.admin.models.DoctorStatusUpdateRequest
import com.example.appointmed.features.admin.models.UnavailableDateRequest
import com.example.appointmed.features.admin.models.UpdateScheduleRequest
import com.example.appointmed.features.admin.models.toUi
import com.example.appointmed.features.doctor.adapters.UnavailableDateAdapter
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Wired to /api/admin/doctors (list + create), /api/admin/doctors/{id}/status,
 * and /api/admin/doctors/{id}/availability (FR-016: admin can view and
 * override any doctor's working days/hours/leave dates — not just override
 * individual appointments).
 */
class AdminDoctorsFragment : Fragment() {

    private var _binding: FragmentAdminDoctorsBinding? = null
    private val binding get() = _binding!!

    private var doctors: List<AdminDoctor> = emptyList()

    /** Scoped to whichever "manage availability" dialog is currently open — reset each time one opens. */
    private var dialogUnavailableDates = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDoctorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvAdminDoctors.layoutManager = LinearLayoutManager(requireContext())
        loadDoctors()

        binding.btnAddDoctor.setOnClickListener { showAddDoctorDialog() }
    }

    private fun loadDoctors() {
        binding.tvAdminDoctorsLoading.visibility = View.VISIBLE
        binding.tvAdminDoctorsError.visibility = View.GONE
        binding.rvAdminDoctors.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext()).listDoctors()
                if (response.isSuccessful && response.body() != null) {
                    doctors = response.body()!!.map { it.toUi() }
                    binding.tvAdminDoctorsLoading.visibility = View.GONE
                    binding.rvAdminDoctors.visibility = View.VISIBLE
                    refreshList()
                } else {
                    showLoadError("Couldn't load doctors.")
                }
            } catch (e: Exception) {
                showLoadError("Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showLoadError(message: String) {
        binding.tvAdminDoctorsLoading.visibility = View.GONE
        binding.tvAdminDoctorsError.visibility = View.VISIBLE
        binding.tvAdminDoctorsError.text = message
    }

    private fun refreshList() {
        binding.rvAdminDoctors.adapter = AdminDoctorAdapter(
            doctors,
            onToggleStatus = { doctor -> toggleStatus(doctor) },
            onManageAvailability = { doctor -> showManageAvailabilityDialog(doctor) },
            onDelete = { doctor -> confirmDeleteDoctor(doctor) }
        )
    }

    private fun confirmDeleteDoctor(doctor: AdminDoctor) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete ${doctor.name}?")
            .setMessage("This can't be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteDoctor(doctor) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteDoctor(doctor: AdminDoctor) {
        binding.tvAdminDoctorsError.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext()).deleteDoctor(doctor.id)
                if (response.isSuccessful) {
                    doctors = doctors.filter { it.id != doctor.id }
                    refreshList()
                } else {
                    binding.tvAdminDoctorsError.visibility = View.VISIBLE
                    binding.tvAdminDoctorsError.text = "Couldn't delete that doctor."
                }
            } catch (e: Exception) {
                binding.tvAdminDoctorsError.visibility = View.VISIBLE
                binding.tvAdminDoctorsError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    private fun toggleStatus(doctor: AdminDoctor) {
        binding.tvAdminDoctorsError.visibility = View.GONE
        val nextStatus = if (doctor.status == "active") "ON_LEAVE" else "ACTIVE"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext())
                    .updateDoctorStatus(doctor.id, DoctorStatusUpdateRequest(nextStatus))
                if (response.isSuccessful && response.body() != null) {
                    val updated = response.body()!!.toUi()
                    doctors = doctors.map { if (it.id == updated.id) updated else it }
                    refreshList()
                } else {
                    binding.tvAdminDoctorsError.visibility = View.VISIBLE
                    binding.tvAdminDoctorsError.text = "Couldn't update that doctor's status."
                }
            } catch (e: Exception) {
                binding.tvAdminDoctorsError.visibility = View.VISIBLE
                binding.tvAdminDoctorsError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    private fun showAddDoctorDialog() {
        val dialogBinding = DialogAddDoctorBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add doctor account")
            .setView(dialogBinding.root)
            .setPositiveButton("Create", null) // overridden below to prevent auto-dismiss on error
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                attemptCreateDoctor(dialogBinding, dialog)
            }
        }

        dialog.show()
    }

    private fun attemptCreateDoctor(dialogBinding: DialogAddDoctorBinding, dialog: AlertDialog) {
        dialogBinding.tvAddDoctorError.visibility = View.GONE

        val fullName = dialogBinding.etNewDoctorFullName.text.toString().trim()
        val email = dialogBinding.etNewDoctorEmail.text.toString().trim()
        val password = dialogBinding.etNewDoctorPassword.text.toString()
        val specialization = dialogBinding.etNewDoctorSpecialization.text.toString().trim()

        if (fullName.isEmpty() || password.isEmpty() || specialization.isEmpty()) {
            showAddDoctorError(dialogBinding, "Fill in all fields.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showAddDoctorError(dialogBinding, "Enter a valid email address.")
            return
        }
        if (!email.lowercase().endsWith("@appointmeddoctor.com")) {
            showAddDoctorError(dialogBinding, "Doctor email must end in @appointmeddoctor.com")
            return
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext())
                    .createDoctor(DoctorCreateRequest(fullName, email, password, specialization))

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    doctors = doctors + response.body()!!.toUi()
                    refreshList()
                    dialog.dismiss()
                } else {
                    showAddDoctorError(dialogBinding, response.errorBody()?.string() ?: "Couldn't create doctor account.")
                }
            } catch (e: Exception) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                showAddDoctorError(dialogBinding, "Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showAddDoctorError(dialogBinding: DialogAddDoctorBinding, message: String) {
        dialogBinding.tvAddDoctorError.text = message
        dialogBinding.tvAddDoctorError.visibility = View.VISIBLE
    }

    // ---------- Manage doctor availability (FR-016) ----------

    private val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    private fun dayCheckbox(dialogBinding: DialogManageDoctorAvailabilityBinding, day: String): CheckBox = when (day) {
        "Mon" -> dialogBinding.cbMon
        "Tue" -> dialogBinding.cbTue
        "Wed" -> dialogBinding.cbWed
        "Thu" -> dialogBinding.cbThu
        "Fri" -> dialogBinding.cbFri
        "Sat" -> dialogBinding.cbSat
        else -> dialogBinding.cbSun
    }

    private fun showManageAvailabilityDialog(doctor: AdminDoctor) {
        val dialogBinding = DialogManageDoctorAvailabilityBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.rvUnavailableDates.layoutManager = LinearLayoutManager(requireContext())
        dialogUnavailableDates = mutableListOf()

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("${doctor.name} — availability")
            .setView(dialogBinding.root)
            .setPositiveButton("Close", null)
            .create()
        dialog.show()

        dialogBinding.etStartTime.setOnClickListener { pickTime(dialogBinding.etStartTime) }
        dialogBinding.etEndTime.setOnClickListener { pickTime(dialogBinding.etEndTime) }
        dialogBinding.etNewUnavailableDate.setOnClickListener { pickDate(dialogBinding) }
        dialogBinding.btnSaveSchedule.setOnClickListener { saveDoctorSchedule(doctor.id, dialogBinding) }
        dialogBinding.btnAddUnavailableDate.setOnClickListener { addDoctorUnavailableDate(doctor.id, dialogBinding) }

        loadDoctorAvailability(doctor.id, dialogBinding)
    }

    private fun loadDoctorAvailability(doctorId: Long, dialogBinding: DialogManageDoctorAvailabilityBinding) {
        dialogBinding.tvAvailLoading.visibility = View.VISIBLE
        dialogBinding.tvAvailError.visibility = View.GONE
        dialogBinding.availabilityContent.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext()).getDoctorAvailability(doctorId)
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    weekdays.forEach { day -> dayCheckbox(dialogBinding, day).isChecked = data.workingDays.contains(day) }
                    dialogBinding.etStartTime.setText(data.startTime)
                    dialogBinding.etEndTime.setText(data.endTime)
                    dialogUnavailableDates = data.unavailableDates.toMutableList()
                    refreshDatesList(doctorId, dialogBinding)

                    dialogBinding.tvAvailLoading.visibility = View.GONE
                    dialogBinding.availabilityContent.visibility = View.VISIBLE
                } else {
                    showAvailLoadError(dialogBinding, "Couldn't load this doctor's availability.")
                }
            } catch (e: Exception) {
                showAvailLoadError(dialogBinding, "Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showAvailLoadError(dialogBinding: DialogManageDoctorAvailabilityBinding, message: String) {
        dialogBinding.tvAvailLoading.visibility = View.GONE
        dialogBinding.tvAvailError.visibility = View.VISIBLE
        dialogBinding.tvAvailError.text = message
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
        }, hour, minute, true).show()
    }

    private fun pickDate(dialogBinding: DialogManageDoctorAvailabilityBinding) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
            dialogBinding.etNewUnavailableDate.setText(date)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveDoctorSchedule(doctorId: Long, dialogBinding: DialogManageDoctorAvailabilityBinding) {
        val selectedDays = weekdays.filter { dayCheckbox(dialogBinding, it).isChecked }
        val start = dialogBinding.etStartTime.text.toString()
        val end = dialogBinding.etEndTime.text.toString()

        if (selectedDays.isEmpty()) {
            showScheduleSaveMessage(dialogBinding, "Select at least one working day.")
            return
        }
        if (start.isEmpty() || end.isEmpty() || start >= end) {
            showScheduleSaveMessage(dialogBinding, "Start time must be before end time.")
            return
        }

        dialogBinding.btnSaveSchedule.isEnabled = false
        dialogBinding.btnSaveSchedule.text = "Saving…"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext())
                    .updateDoctorAvailability(doctorId, UpdateScheduleRequest(selectedDays, start, end))

                dialogBinding.btnSaveSchedule.isEnabled = true
                dialogBinding.btnSaveSchedule.text = "Save schedule"

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    weekdays.forEach { day -> dayCheckbox(dialogBinding, day).isChecked = data.workingDays.contains(day) }
                    dialogBinding.etStartTime.setText(data.startTime)
                    dialogBinding.etEndTime.setText(data.endTime)
                    showScheduleSaveMessage(dialogBinding, "Schedule saved.")
                } else {
                    showScheduleSaveMessage(dialogBinding, "Couldn't save schedule.")
                }
            } catch (e: Exception) {
                dialogBinding.btnSaveSchedule.isEnabled = true
                dialogBinding.btnSaveSchedule.text = "Save schedule"
                showScheduleSaveMessage(dialogBinding, "Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showScheduleSaveMessage(dialogBinding: DialogManageDoctorAvailabilityBinding, message: String) {
        dialogBinding.tvSaveMessage.text = message
        dialogBinding.tvSaveMessage.visibility = View.VISIBLE
    }

    private fun addDoctorUnavailableDate(doctorId: Long, dialogBinding: DialogManageDoctorAvailabilityBinding) {
        val date = dialogBinding.etNewUnavailableDate.text.toString()
        if (date.isEmpty()) return

        dialogBinding.tvDateActionError.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext())
                    .addDoctorUnavailableDate(doctorId, UnavailableDateRequest(date))

                if (response.isSuccessful && response.body() != null) {
                    dialogUnavailableDates = response.body()!!.unavailableDates.toMutableList()
                    dialogBinding.etNewUnavailableDate.setText("")
                    refreshDatesList(doctorId, dialogBinding)
                } else {
                    showDateActionError(dialogBinding, "Couldn't add that date.")
                }
            } catch (e: Exception) {
                showDateActionError(dialogBinding, "Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun removeDoctorUnavailableDate(doctorId: Long, dialogBinding: DialogManageDoctorAvailabilityBinding, date: String) {
        dialogBinding.tvDateActionError.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext())
                    .removeDoctorUnavailableDate(doctorId, date)

                if (response.isSuccessful && response.body() != null) {
                    dialogUnavailableDates = response.body()!!.unavailableDates.toMutableList()
                    refreshDatesList(doctorId, dialogBinding)
                } else {
                    showDateActionError(dialogBinding, "Couldn't remove that date.")
                }
            } catch (e: Exception) {
                showDateActionError(dialogBinding, "Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showDateActionError(dialogBinding: DialogManageDoctorAvailabilityBinding, message: String) {
        dialogBinding.tvDateActionError.text = message
        dialogBinding.tvDateActionError.visibility = View.VISIBLE
    }

    private fun refreshDatesList(doctorId: Long, dialogBinding: DialogManageDoctorAvailabilityBinding) {
        dialogBinding.tvNoUnavailableDates.visibility = if (dialogUnavailableDates.isEmpty()) View.VISIBLE else View.GONE
        dialogBinding.rvUnavailableDates.adapter = UnavailableDateAdapter(dialogUnavailableDates) { date ->
            removeDoctorUnavailableDate(doctorId, dialogBinding, date)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
