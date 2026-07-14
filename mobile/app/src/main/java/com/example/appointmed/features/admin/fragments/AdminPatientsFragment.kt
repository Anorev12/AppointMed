package com.example.appointmed.features.admin.fragments
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.databinding.DialogAddPatientBinding
import com.example.appointmed.databinding.DialogPatientHistoryBinding
import com.example.appointmed.databinding.FragmentAdminPatientsBinding
import com.example.appointmed.features.admin.adapters.AdminAppointmentAdapter
import com.example.appointmed.features.admin.adapters.AdminPatientAdapter
import com.example.appointmed.features.admin.models.AdminPatient
import com.example.appointmed.features.admin.models.PatientCreateRequest
import com.example.appointmed.features.admin.models.toUi
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/** Wired to /api/admin/patients (list/create/delete) and /api/admin/patients/{id}/appointments. */
class AdminPatientsFragment : Fragment() {

    private var _binding: FragmentAdminPatientsBinding? = null
    private val binding get() = _binding!!

    private var patients: List<AdminPatient> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvAdminPatients.layoutManager = LinearLayoutManager(requireContext())
        binding.btnAddPatient.setOnClickListener { showAddPatientDialog() }
        loadPatients()
    }

    private fun loadPatients() {
        binding.tvAdminPatientsLoading.visibility = View.VISIBLE
        binding.tvAdminPatientsError.visibility = View.GONE
        binding.rvAdminPatients.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext()).listPatients()
                if (response.isSuccessful && response.body() != null) {
                    patients = response.body()!!.map { it.toUi() }
                    binding.tvAdminPatientsLoading.visibility = View.GONE
                    binding.rvAdminPatients.visibility = View.VISIBLE
                    refreshList()
                } else {
                    showLoadError("Couldn't load patients.")
                }
            } catch (e: Exception) {
                showLoadError("Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showLoadError(message: String) {
        binding.tvAdminPatientsLoading.visibility = View.GONE
        binding.tvAdminPatientsError.visibility = View.VISIBLE
        binding.tvAdminPatientsError.text = message
    }

    private fun refreshList() {
        binding.rvAdminPatients.adapter = AdminPatientAdapter(
            patients,
            onViewHistory = { patient -> showHistory(patient) },
            onDelete = { patient -> confirmDeletePatient(patient) }
        )
    }

    private fun confirmDeletePatient(patient: AdminPatient) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete ${patient.name}?")
            .setMessage("This can't be undone.")
            .setPositiveButton("Delete") { _, _ -> deletePatient(patient) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePatient(patient: AdminPatient) {
        binding.tvAdminPatientsError.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext()).deletePatient(patient.id)
                if (response.isSuccessful) {
                    patients = patients.filter { it.id != patient.id }
                    refreshList()
                } else {
                    binding.tvAdminPatientsError.visibility = View.VISIBLE
                    binding.tvAdminPatientsError.text = "Couldn't delete that patient."
                }
            } catch (e: Exception) {
                binding.tvAdminPatientsError.visibility = View.VISIBLE
                binding.tvAdminPatientsError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    private fun showAddPatientDialog() {
        val dialogBinding = DialogAddPatientBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add patient account")
            .setView(dialogBinding.root)
            .setPositiveButton("Create", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                attemptCreatePatient(dialogBinding, dialog)
            }
        }
        dialogBinding.etNewPatientDob.setOnClickListener { showDobPicker(dialogBinding) }

        dialog.show()
    }

    /** Opens a date picker seeded with the current value (or today), and writes "yyyy-MM-dd" back into etNewPatientDob. */
    private fun showDobPicker(dialogBinding: DialogAddPatientBinding) {
        val calendar = Calendar.getInstance()
        val current = dialogBinding.etNewPatientDob.text?.toString().orEmpty()
        if (current.isNotBlank()) {
            val parts = current.split("-")
            if (parts.size == 3) {
                val year = parts[0].toIntOrNull()
                val month = parts[1].toIntOrNull()
                val day = parts[2].toIntOrNull()
                if (year != null && month != null && day != null) {
                    calendar.set(year, month - 1, day)
                }
            }
        }

        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                dialogBinding.etNewPatientDob.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }

    private fun attemptCreatePatient(dialogBinding: DialogAddPatientBinding, dialog: AlertDialog) {
        dialogBinding.tvAddPatientError.visibility = View.GONE

        val fullName = dialogBinding.etNewPatientFullName.text.toString().trim()
        val email = dialogBinding.etNewPatientEmail.text.toString().trim()
        val password = dialogBinding.etNewPatientPassword.text.toString()
        val contact = dialogBinding.etNewPatientContact.text.toString().trim()
        val dob = dialogBinding.etNewPatientDob.text.toString().trim()

        if (fullName.isEmpty() || password.isEmpty()) {
            showDialogError(dialogBinding, "Fill in all required fields.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showDialogError(dialogBinding, "Enter a valid email address.")
            return
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext())
                    .createPatient(PatientCreateRequest(fullName, email, password, contact, dob.ifEmpty { null }))

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    patients = patients + response.body()!!.toUi()
                    refreshList()
                    dialog.dismiss()
                } else {
                    showDialogError(dialogBinding, response.errorBody()?.string() ?: "Couldn't create patient account.")
                }
            } catch (e: Exception) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                showDialogError(dialogBinding, "Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showDialogError(dialogBinding: DialogAddPatientBinding, message: String) {
        dialogBinding.tvAddPatientError.text = message
        dialogBinding.tvAddPatientError.visibility = View.VISIBLE
    }

    private fun showHistory(patient: AdminPatient) {
        val dialogBinding = DialogPatientHistoryBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.rvHistory.layoutManager = LinearLayoutManager(requireContext())

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(patient.name)
            .setView(dialogBinding.root)
            .setPositiveButton("Close", null)
            .create()
        dialog.show()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext()).patientHistory(patient.id)
                dialogBinding.tvHistoryLoading.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val history = response.body()!!.map { it.toUi() }
                    if (history.isEmpty()) {
                        dialogBinding.tvHistoryEmpty.visibility = View.VISIBLE
                    } else {
                        dialogBinding.rvHistory.visibility = View.VISIBLE
                        dialogBinding.rvHistory.adapter = AdminAppointmentAdapter(
                            history,
                            showDoctorColumn = true,
                            onOverrideCancel = null // read-only in the history view
                        )
                    }
                } else {
                    dialogBinding.tvHistoryError.visibility = View.VISIBLE
                    dialogBinding.tvHistoryError.text = "Couldn't load that patient's history."
                }
            } catch (e: Exception) {
                dialogBinding.tvHistoryLoading.visibility = View.GONE
                dialogBinding.tvHistoryError.visibility = View.VISIBLE
                dialogBinding.tvHistoryError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
