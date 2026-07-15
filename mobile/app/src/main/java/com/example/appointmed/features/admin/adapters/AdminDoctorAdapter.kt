package com.example.appointmed.features.admin.adapters
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appointmed.R
import com.example.appointmed.databinding.ItemAdminDoctorBinding
import com.example.appointmed.features.admin.models.AdminDoctor
class AdminDoctorAdapter(
    private val doctors: List<AdminDoctor>,
    private val onToggleStatus: (AdminDoctor) -> Unit,
    private val onManageAvailability: (AdminDoctor) -> Unit,
    private val onDelete: (AdminDoctor) -> Unit
) : RecyclerView.Adapter<AdminDoctorAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAdminDoctorBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doctor = doctors[position]
        val context = holder.binding.root.context

        holder.binding.tvAdminDoctorName.text = doctor.name
        holder.binding.tvAdminDoctorSpecialization.text = doctor.specialization

        val isActive = doctor.status == "active"
        holder.binding.tvAdminDoctorStatus.text = doctor.status.replaceFirstChar { it.uppercase() }
        holder.binding.tvAdminDoctorStatus.setBackgroundColor(
            ContextCompat.getColor(context, if (isActive) R.color.success_soft else R.color.danger_soft)
        )
        holder.binding.tvAdminDoctorStatus.setTextColor(
            ContextCompat.getColor(context, if (isActive) R.color.success else R.color.danger)
        )

        holder.binding.btnToggleDoctorStatus.text = if (isActive) "Mark on leave" else "Mark active"
        holder.binding.btnToggleDoctorStatus.setOnClickListener { onToggleStatus(doctor) }
        holder.binding.btnManageDoctorAvailability.setOnClickListener { onManageAvailability(doctor) }
        holder.binding.btnDeleteDoctor.setOnClickListener { onDelete(doctor) }
    }

    override fun getItemCount() = doctors.size
}
