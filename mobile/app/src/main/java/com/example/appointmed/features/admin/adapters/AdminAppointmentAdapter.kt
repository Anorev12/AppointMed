package com.example.appointmed.features.admin.adapters
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appointmed.R
import com.example.appointmed.databinding.ItemAdminAppointmentBinding
import com.example.appointmed.features.admin.models.AdminAppointment
class AdminAppointmentAdapter(
    private val appointments: List<AdminAppointment>,
    private val showDoctorColumn: Boolean = true,
    private val onOverrideCancel: ((AdminAppointment) -> Unit)?
) : RecyclerView.Adapter<AdminAppointmentAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAdminAppointmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val apt = appointments[position]
        val context = holder.binding.root.context

        holder.binding.tvAdminAptPatient.text = apt.patient
        holder.binding.tvAdminAptDoctor.text = apt.doctor
        holder.binding.tvAdminAptDoctor.visibility = if (showDoctorColumn) View.VISIBLE else View.GONE
        holder.binding.tvAdminAptDateTime.text = "${apt.date} · ${com.example.appointmed.core.utils.formatTime12h(apt.time)}"
        holder.binding.tvAdminAptRef.text = apt.id
        holder.binding.tvAdminAptStatus.text = apt.status.replaceFirstChar { it.uppercase() }

        val (bgColor, textColor) = when (apt.status) {
            "confirmed" -> R.color.success_soft to R.color.success
            "completed" -> R.color.border to R.color.ink_soft
            else -> R.color.danger_soft to R.color.danger
        }
        holder.binding.tvAdminAptStatus.setBackgroundColor(ContextCompat.getColor(context, bgColor))
        holder.binding.tvAdminAptStatus.setTextColor(ContextCompat.getColor(context, textColor))

        // Mirrors AppointmentService.cancelByAdmin: only an upcoming CONFIRMED
        // appointment can be override-cancelled — not one already completed,
        // and not one whose date/time has already passed. "Now" is anchored to
        // the clinic's timezone (Asia/Manila), matching the backend's CLINIC_ZONE,
        // since the device's local zone may not match the clinic's.
        val isUpcoming = try {
            val clinicZone = java.time.ZoneId.of("Asia/Manila")
            val appointmentStart = java.time.LocalDateTime.of(
                java.time.LocalDate.parse(apt.date),
                java.time.LocalTime.parse(apt.time)
            )
            appointmentStart.isAfter(java.time.LocalDateTime.now(clinicZone))
        } catch (e: Exception) {
            false
        }

        if (onOverrideCancel != null && apt.status == "confirmed" && isUpcoming) {
            holder.binding.btnOverrideCancel.visibility = View.VISIBLE
            holder.binding.btnOverrideCancel.setOnClickListener { onOverrideCancel.invoke(apt) }
        } else {
            holder.binding.btnOverrideCancel.visibility = View.GONE
        }
    }

    override fun getItemCount() = appointments.size
}