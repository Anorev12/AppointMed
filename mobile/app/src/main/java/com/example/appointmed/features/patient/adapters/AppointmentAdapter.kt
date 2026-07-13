package com.example.appointmed.features.patient.adapters
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appointmed.R
import com.example.appointmed.databinding.ItemAppointmentBinding
import com.example.appointmed.features.patient.models.Appointment
class AppointmentAdapter(
    private val appointments: List<Appointment>,
    private val onCancel: (Appointment) -> Unit,
    private val onReschedule: (Appointment) -> Unit
) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    inner class AppointmentViewHolder(val binding: ItemAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val binding = ItemAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val apt = appointments[position]
        val context = holder.binding.root.context

        holder.binding.tvAptDoctor.text = "${apt.doctor} · ${apt.specialization}"
        holder.binding.tvAptDateTime.text = "${apt.date} · ${com.example.appointmed.core.utils.formatTime12h(apt.time)}"
        holder.binding.tvAptRef.text = apt.id

        val isConfirmed = apt.status == "confirmed"
        val isCancelled = apt.status == "cancelled"
        holder.binding.tvAptStatus.text = apt.status.replaceFirstChar { it.uppercase() }

        val (bgColor, textColor) = when {
            isConfirmed -> R.color.success_soft to R.color.success
            isCancelled -> R.color.danger_soft to R.color.danger
            else -> R.color.border to R.color.ink_soft // "completed"
        }
        holder.binding.tvAptStatus.setBackgroundColor(ContextCompat.getColor(context, bgColor))
        holder.binding.tvAptStatus.setTextColor(ContextCompat.getColor(context, textColor))

        // Only a still-confirmed, upcoming appointment can be changed (FR-011).
        holder.binding.btnCancelApt.visibility = if (isConfirmed) View.VISIBLE else View.GONE
        holder.binding.btnRescheduleApt.visibility = if (isConfirmed) View.VISIBLE else View.GONE

        holder.binding.btnCancelApt.setOnClickListener {
            onCancel(apt)
        }
        holder.binding.btnRescheduleApt.setOnClickListener {
            onReschedule(apt)
        }
    }

    override fun getItemCount() = appointments.size
}