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
import com.example.appointmed.databinding.DialogEditNotificationTemplateBinding
import com.example.appointmed.databinding.DialogManageAdminsBinding
import com.example.appointmed.databinding.DialogManageNotificationTemplatesBinding
import com.example.appointmed.databinding.DialogReminderScheduleBinding
import com.example.appointmed.databinding.FragmentAdminProfileBinding
import com.example.appointmed.features.admin.adapters.AdminSimpleAdapter
import com.example.appointmed.features.admin.adapters.NotificationTemplateAdapter
import com.example.appointmed.features.admin.adapters.ReminderOffsetAdapter
import com.example.appointmed.features.admin.models.AdminCreateRequest
import com.example.appointmed.features.admin.models.NotificationTemplateResponse
import com.example.appointmed.features.admin.models.NotificationTemplateUpdateRequest
import com.example.appointmed.features.admin.models.PasswordChangeRequest
import com.example.appointmed.features.admin.models.ReminderSettingsUpdateRequest
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.core.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * Admin's profile tab — account info, change own password, manage admin
 * accounts (list + create; deliberately no delete — see AdminService on
 * the backend for the business rule), logout, and notification settings
 * (FR-024: editable subject/custom-message per notification template, and
 * a configurable reminder-offset schedule — mirrors the web AdminDashboard
 * Settings tab).
 */
class AdminProfileFragment : Fragment() {

    private var _binding: FragmentAdminProfileBinding? = null
    private val binding get() = _binding!!

    // Mutable working copy while the reminder-schedule dialog is open (FR-024).
    private var dialogReminderOffsets = mutableListOf<Int>()

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
        binding.btnNotificationTemplates.setOnClickListener { showManageTemplatesDialog() }
        binding.btnReminderSchedule.setOnClickListener { showReminderScheduleDialog() }
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

    // ---------- Notification templates (FR-024) ----------

    private fun showManageTemplatesDialog() {
        val dialogBinding = DialogManageNotificationTemplatesBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.rvNotificationTemplates.layoutManager = LinearLayoutManager(requireContext())

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Notification templates")
            .setView(dialogBinding.root)
            .setPositiveButton("Close", null)
            .create()
        dialog.show()

        loadTemplates(dialogBinding)
    }

    private fun loadTemplates(dialogBinding: DialogManageNotificationTemplatesBinding) {
        dialogBinding.tvTemplatesLoading.visibility = View.VISIBLE
        dialogBinding.tvTemplatesError.visibility = View.GONE
        dialogBinding.rvNotificationTemplates.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext()).listNotificationTemplates()
                dialogBinding.tvTemplatesLoading.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    dialogBinding.rvNotificationTemplates.visibility = View.VISIBLE
                    dialogBinding.rvNotificationTemplates.adapter = NotificationTemplateAdapter(response.body()!!) { template ->
                        showEditTemplateDialog(template, dialogBinding)
                    }
                } else {
                    dialogBinding.tvTemplatesError.visibility = View.VISIBLE
                    dialogBinding.tvTemplatesError.text = "Couldn't load notification templates."
                }
            } catch (e: Exception) {
                dialogBinding.tvTemplatesLoading.visibility = View.GONE
                dialogBinding.tvTemplatesError.visibility = View.VISIBLE
                dialogBinding.tvTemplatesError.text = "Can't reach the server. Check that it's running and try again."
            }
        }
    }

    private fun showEditTemplateDialog(
        template: NotificationTemplateResponse,
        parentDialogBinding: DialogManageNotificationTemplatesBinding
    ) {
        val dialogBinding = DialogEditNotificationTemplateBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.etTemplateSubject.setText(template.subjectTemplate)
        dialogBinding.etTemplateCustomMessage.setText(template.customMessage)
        dialogBinding.tvTemplatePlaceholders.text = "Available placeholders: ${template.availablePlaceholders}"

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(template.label)
            .setView(dialogBinding.root)
            .setPositiveButton("Save", null)
            .setNeutralButton("Reset to default", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                attemptSaveTemplate(template.type, dialogBinding, dialog, parentDialogBinding)
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                attemptResetTemplate(template.type, dialog, parentDialogBinding)
            }
        }

        dialog.show()
    }

    private fun attemptSaveTemplate(
        type: String,
        dialogBinding: DialogEditNotificationTemplateBinding,
        dialog: AlertDialog,
        parentDialogBinding: DialogManageNotificationTemplatesBinding
    ) {
        dialogBinding.tvEditTemplateError.visibility = View.GONE

        val subject = dialogBinding.etTemplateSubject.text.toString().trim()
        val customMessage = dialogBinding.etTemplateCustomMessage.text.toString().trim()

        if (subject.isEmpty()) {
            showDialogError(dialogBinding.tvEditTemplateError, "Subject line can't be empty.")
            return
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext())
                    .updateNotificationTemplate(type, NotificationTemplateUpdateRequest(subject, customMessage))

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true

                if (response.isSuccessful) {
                    dialog.dismiss()
                    loadTemplates(parentDialogBinding)
                } else {
                    showDialogError(dialogBinding.tvEditTemplateError, response.errorBody()?.string() ?: "Couldn't save the template.")
                }
            } catch (e: Exception) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                showDialogError(dialogBinding.tvEditTemplateError, "Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun attemptResetTemplate(
        type: String,
        dialog: AlertDialog,
        parentDialogBinding: DialogManageNotificationTemplatesBinding
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext()).resetNotificationTemplate(type)
                if (response.isSuccessful) {
                    dialog.dismiss()
                    loadTemplates(parentDialogBinding)
                }
            } catch (e: Exception) {
                // Silently ignore — the dialog stays open showing the unsaved edits, and the admin can retry.
            }
        }
    }

    // ---------- Reminder schedule (FR-024) ----------

    private fun showReminderScheduleDialog() {
        val dialogBinding = DialogReminderScheduleBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.rvReminderOffsets.layoutManager = LinearLayoutManager(requireContext())
        dialogReminderOffsets = mutableListOf()

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Reminder schedule")
            .setView(dialogBinding.root)
            .setPositiveButton("Close", null)
            .create()
        dialog.show()

        dialogBinding.btnAddReminderOffset.setOnClickListener { addReminderOffsetLocally(dialogBinding) }
        dialogBinding.btnSaveReminderSchedule.setOnClickListener { saveReminderSchedule(dialogBinding) }

        loadReminderSettings(dialogBinding)
    }

    private fun loadReminderSettings(dialogBinding: DialogReminderScheduleBinding) {
        dialogBinding.tvReminderLoading.visibility = View.VISIBLE
        dialogBinding.reminderContent.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext()).getReminderSettings()
                dialogBinding.tvReminderLoading.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    dialogBinding.reminderContent.visibility = View.VISIBLE
                    dialogReminderOffsets = response.body()!!.offsetHours.toMutableList()
                    renderReminderOffsets(dialogBinding)
                } else {
                    dialogBinding.tvReminderLoading.text = "Couldn't load the reminder schedule."
                    dialogBinding.tvReminderLoading.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                dialogBinding.tvReminderLoading.text = "Can't reach the server. Check that it's running and try again."
                dialogBinding.tvReminderLoading.visibility = View.VISIBLE
            }
        }
    }

    private fun addReminderOffsetLocally(dialogBinding: DialogReminderScheduleBinding) {
        dialogBinding.tvReminderActionError.visibility = View.GONE

        val hours = dialogBinding.etNewReminderOffset.text.toString().toIntOrNull()
        if (hours == null || hours < 1 || hours > 168) {
            showDialogError(dialogBinding.tvReminderActionError, "Enter a number of hours between 1 and 168.")
            return
        }
        if (dialogReminderOffsets.contains(hours)) {
            dialogBinding.etNewReminderOffset.setText("")
            return
        }
        if (dialogReminderOffsets.size >= 5) {
            showDialogError(dialogBinding.tvReminderActionError, "At most 5 reminder offsets are allowed.")
            return
        }

        dialogReminderOffsets.add(hours)
        dialogReminderOffsets.sortDescending()
        dialogBinding.etNewReminderOffset.setText("")
        renderReminderOffsets(dialogBinding)
    }

    private fun removeReminderOffsetLocally(hours: Int, dialogBinding: DialogReminderScheduleBinding) {
        dialogReminderOffsets.remove(hours)
        renderReminderOffsets(dialogBinding)
    }

    private fun renderReminderOffsets(dialogBinding: DialogReminderScheduleBinding) {
        dialogBinding.tvNoReminderOffsets.visibility = if (dialogReminderOffsets.isEmpty()) View.VISIBLE else View.GONE
        dialogBinding.rvReminderOffsets.adapter = ReminderOffsetAdapter(dialogReminderOffsets) { hours ->
            removeReminderOffsetLocally(hours, dialogBinding)
        }
        dialogBinding.tvReminderSaveMessage.visibility = View.GONE
    }

    private fun saveReminderSchedule(dialogBinding: DialogReminderScheduleBinding) {
        dialogBinding.tvReminderActionError.visibility = View.GONE

        if (dialogReminderOffsets.isEmpty()) {
            showDialogError(dialogBinding.tvReminderActionError, "Select at least one reminder offset.")
            return
        }

        dialogBinding.btnSaveReminderSchedule.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getAdminApi(requireContext())
                    .updateReminderSettings(ReminderSettingsUpdateRequest(dialogReminderOffsets))

                dialogBinding.btnSaveReminderSchedule.isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    dialogReminderOffsets = response.body()!!.offsetHours.toMutableList()
                    renderReminderOffsets(dialogBinding)
                    dialogBinding.tvReminderSaveMessage.text = "Saved."
                    dialogBinding.tvReminderSaveMessage.visibility = View.VISIBLE
                } else {
                    showDialogError(dialogBinding.tvReminderActionError, response.errorBody()?.string() ?: "Couldn't save the reminder schedule.")
                }
            } catch (e: Exception) {
                dialogBinding.btnSaveReminderSchedule.isEnabled = true
                showDialogError(dialogBinding.tvReminderActionError, "Can't reach the server. Check that it's running and try again.")
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
