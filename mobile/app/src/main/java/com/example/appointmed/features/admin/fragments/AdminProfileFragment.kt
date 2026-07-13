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
import com.example.appointmed.features.admin.AdminDashboardActivity
import com.example.appointmed.databinding.DialogAddAdminBinding
import com.example.appointmed.databinding.DialogChangePasswordBinding
import com.example.appointmed.databinding.DialogManageAdminsBinding
import com.example.appointmed.databinding.FragmentAdminProfileBinding
import com.example.appointmed.features.admin.adapters.AdminSimpleAdapter
import com.example.appointmed.features.admin.models.AdminCreateRequest
import com.example.appointmed.features.admin.models.PasswordChangeRequest
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.core.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * Admin's profile tab — account info, change own password, manage admin
 * accounts (list + create; deliberately no delete — see AdminService on
 * the backend for the business rule), and logout.
 */
class AdminProfileFragment : Fragment() {

    private var _binding: FragmentAdminProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = TokenManager(requireContext()).getUser()
        binding.etAdminFullName.setText(user?.fullName)
        binding.etAdminEmail.setText(user?.email)

        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.btnManageAdmins.setOnClickListener { showManageAdminsDialog() }
        binding.btnAdminLogout.setOnClickListener {
            (requireActivity() as AdminDashboardActivity).logout()
        }
    }

    // ---------- Change own password ----------

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Change password")
            .setView(dialogBinding.root)
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                attemptChangePassword(dialogBinding, dialog)
            }
        }

        dialog.show()
    }

    private fun attemptChangePassword(dialogBinding: DialogChangePasswordBinding, dialog: AlertDialog) {
        dialogBinding.tvChangePasswordError.visibility = View.GONE

        val oldPassword = dialogBinding.etOldPassword.text.toString()
        val newPassword = dialogBinding.etNewPassword.text.toString()
        val confirmPassword = dialogBinding.etConfirmPassword.text.toString()

        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showDialogError(dialogBinding.tvChangePasswordError, "Fill in all fields.")
            return
        }
        if (newPassword != confirmPassword) {
            showDialogError(dialogBinding.tvChangePasswordError, "New password and confirmation don't match.")
            return
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext())
                    .changeOwnPassword(PasswordChangeRequest(oldPassword, newPassword, confirmPassword))

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true

                if (response.isSuccessful) {
                    dialog.dismiss()
                } else {
                    showDialogError(dialogBinding.tvChangePasswordError, response.errorBody()?.string() ?: "Couldn't update your password.")
                }
            } catch (e: Exception) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                showDialogError(dialogBinding.tvChangePasswordError, "Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    // ---------- Manage admin accounts (list + create; no delete) ----------

    private fun showManageAdminsDialog() {
        val dialogBinding = DialogManageAdminsBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.rvManageAdmins.layoutManager = LinearLayoutManager(requireContext())

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Admin accounts")
            .setView(dialogBinding.root)
            .setPositiveButton("Close", null)
            .create()
        dialog.show()

        dialogBinding.btnAddAdminInDialog.setOnClickListener { showAddAdminDialog(dialogBinding) }

        loadAdmins(dialogBinding)
    }

    private fun loadAdmins(dialogBinding: DialogManageAdminsBinding) {
        dialogBinding.tvManageAdminsLoading.visibility = View.VISIBLE
        dialogBinding.tvManageAdminsError.visibility = View.GONE
        dialogBinding.rvManageAdmins.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext()).listAdmins()
                dialogBinding.tvManageAdminsLoading.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    dialogBinding.rvManageAdmins.visibility = View.VISIBLE
                    dialogBinding.rvManageAdmins.adapter = AdminSimpleAdapter(response.body()!!)
                } else {
                    dialogBinding.tvManageAdminsError.visibility = View.VISIBLE
                    dialogBinding.tvManageAdminsError.text = "Couldn't load admins."
                }
            } catch (e: Exception) {
                dialogBinding.tvManageAdminsLoading.visibility = View.GONE
                dialogBinding.tvManageAdminsError.visibility = View.VISIBLE
                dialogBinding.tvManageAdminsError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    private fun showAddAdminDialog(parentDialogBinding: DialogManageAdminsBinding) {
        val dialogBinding = DialogAddAdminBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add admin account")
            .setView(dialogBinding.root)
            .setPositiveButton("Create", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                attemptCreateAdmin(dialogBinding, dialog, parentDialogBinding)
            }
        }

        dialog.show()
    }

    private fun attemptCreateAdmin(
        dialogBinding: DialogAddAdminBinding,
        dialog: AlertDialog,
        parentDialogBinding: DialogManageAdminsBinding
    ) {
        dialogBinding.tvAddAdminError.visibility = View.GONE

        val fullName = dialogBinding.etNewAdminFullName.text.toString().trim()
        val email = dialogBinding.etNewAdminEmail.text.toString().trim()
        val password = dialogBinding.etNewAdminPassword.text.toString()

        if (fullName.isEmpty() || password.isEmpty()) {
            showDialogError(dialogBinding.tvAddAdminError, "Fill in all fields.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showDialogError(dialogBinding.tvAddAdminError, "Enter a valid email address.")
            return
        }
        if (!email.lowercase().endsWith("@appointmedadmin.com")) {
            showDialogError(dialogBinding.tvAddAdminError, "Admin email must end in @appointmedadmin.com")
            return
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext())
                    .createAdmin(AdminCreateRequest(fullName, email, password))

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true

                if (response.isSuccessful) {
                    dialog.dismiss()
                    loadAdmins(parentDialogBinding)
                } else {
                    showDialogError(dialogBinding.tvAddAdminError, response.errorBody()?.string() ?: "Couldn't create admin account.")
                }
            } catch (e: Exception) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                showDialogError(dialogBinding.tvAddAdminError, "Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showDialogError(target: android.widget.TextView, message: String) {
        target.text = message
        target.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
