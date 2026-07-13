package com.example.appointmed.features.doctor.fragments
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.appointmed.features.doctor.DoctorDashboardActivity
import com.example.appointmed.databinding.DialogChangePasswordBinding
import com.example.appointmed.databinding.FragmentDoctorProfileBinding
import com.example.appointmed.features.doctor.models.PasswordChangeRequest
import com.example.appointmed.core.network.RetrofitClient
import com.example.appointmed.core.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * Doctor's profile tab — read-only account info, change password, and
 * logout. No PATCH endpoint exists for doctor profile field updates yet,
 * same placeholder pattern as before; change password is now wired to
 * PUT /api/doctor/profile/password.
 */
class DoctorProfileFragment : Fragment() {

    private var _binding: FragmentDoctorProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = TokenManager(requireContext()).getUser()
        binding.etDoctorFullName.setText(user?.fullName)
        binding.etDoctorEmail.setText(user?.email)
        binding.etDoctorSpecialization.setText(user?.specialization)

        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.btnDoctorLogout.setOnClickListener {
            (requireActivity() as DoctorDashboardActivity).logout()
        }
    }

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
            showDialogError(dialogBinding, "Fill in all fields.")
            return
        }
        if (newPassword != confirmPassword) {
            showDialogError(dialogBinding, "New password and confirmation don't match.")
            return
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.getDoctorProfileApi(requireContext())
                    .changePassword(PasswordChangeRequest(oldPassword, newPassword, confirmPassword))

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true

                if (response.isSuccessful) {
                    dialog.dismiss()
                } else {
                    showDialogError(dialogBinding, response.errorBody()?.string() ?: "Couldn't update your password.")
                }
            } catch (e: Exception) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                showDialogError(dialogBinding, "Can't reach the server. Check that it's running and try again.")
            }
        }
    }

    private fun showDialogError(dialogBinding: DialogChangePasswordBinding, message: String) {
        dialogBinding.tvChangePasswordError.text = message
        dialogBinding.tvChangePasswordError.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
