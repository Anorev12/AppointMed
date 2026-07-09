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
import com.example.appointmed.features.admin.adapters.AdminDoctorAdapter
import com.example.appointmed.databinding.DialogAddDoctorBinding
import com.example.appointmed.databinding.FragmentAdminDoctorsBinding
import com.example.appointmed.features.admin.models.DoctorCreateRequest
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.features.admin.repository.AdminRepository
import kotlinx.coroutines.launch

class AdminDoctorsFragment : Fragment() {

    private var _binding: FragmentAdminDoctorsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDoctorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvAdminDoctors.layoutManager = LinearLayoutManager(requireContext())
        refreshList()

        binding.btnAddDoctor.setOnClickListener { showAddDoctorDialog() }
    }

    private fun refreshList() {
        binding.rvAdminDoctors.adapter = AdminDoctorAdapter(AdminRepository.doctors) { doctor ->
            AdminRepository.toggleDoctorStatus(doctor.id)
            refreshList()
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
                    val created = response.body()!!
                    AdminRepository.addDoctor(created.id, created.fullName, created.specialization)
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