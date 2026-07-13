package com.example.appointmed.features.admin.fragments
import android.app.AlertDialog
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.databinding.DialogAddDoctorBinding
import com.example.appointmed.databinding.FragmentAdminDoctorsBinding
import com.example.appointmed.features.admin.adapters.AdminDoctorAdapter
import com.example.appointmed.features.admin.models.AdminDoctor
import com.example.appointmed.features.admin.models.DoctorCreateRequest
import com.example.appointmed.features.admin.models.DoctorStatusUpdateRequest
import com.example.appointmed.features.admin.models.toUi
import kotlinx.coroutines.launch

/** Wired to /api/admin/doctors (list + create) and /api/admin/doctors/{id}/status. */
class AdminDoctorsFragment : Fragment() {

    private var _binding: FragmentAdminDoctorsBinding? = null
    private val binding get() = _binding!!

    private var doctors: List<AdminDoctor> = emptyList()

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
            showDialogError(dialogBinding, "Fill in all fields.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showDialogError(dialogBinding, "Enter a valid email address.")
            return
        }
        if (!email.lowercase().endsWith("@appointmeddoctor.com")) {
            showDialogError(dialogBinding, "Doctor email must end in @appointmeddoctor.com")
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
                    showDialogError(dialogBinding, response.errorBody()?.string() ?: "Couldn't create doctor account.")
                }
            } catch (e: Exception) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                showDialogError(dialogBinding, "Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showDialogError(dialogBinding: DialogAddDoctorBinding, message: String) {
        dialogBinding.tvAddDoctorError.text = message
        dialogBinding.tvAddDoctorError.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
