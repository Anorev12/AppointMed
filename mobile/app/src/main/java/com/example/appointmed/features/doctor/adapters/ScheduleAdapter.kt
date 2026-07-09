package com.example.appointmed.features.doctor.adapters
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appointmed.R
import com.example.appointmed.databinding.ItemScheduleAppointmentBinding
import com.example.appointmed.features.doctor.models.DoctorAppointment
class ScheduleAdapter(
    private val appointments: List<DoctorAppointment>,
    private val onConfirm: (DoctorAppointment) -> Unit,
    private val onDecline: (DoctorAppointment) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(val binding: ItemScheduleAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val apt = appointments[position]
        val context = holder.binding.root.context

        holder.binding.tvPatientName.text = apt.patient
        holder.binding.tvScheduleDateTime.text = "${apt.date} · ${apt.time}"
        holder.binding.tvScheduleRef.text = apt.id
        holder.binding.tvScheduleStatus.text = apt.status.replaceFirstChar { it.uppercase() }

        val (bgColor, textColor) = when (apt.status) {
            "confirmed" -> R.color.success_soft to R.color.success
            "pending" -> R.color.border to R.color.ink_soft
            else -> R.color.danger_soft to R.color.danger
        }
        holder.binding.tvScheduleStatus.setBackgroundColor(ContextCompat.getColor(context, bgColor))
        holder.binding.tvScheduleStatus.setTextColor(ContextCompat.getColor(context, textColor))

        val isPending = apt.status == "pending"
        holder.binding.pendingActions.visibility = if (isPending) View.VISIBLE else View.GONE
        holder.binding.btnConfirmApt.setOnClickListener { onConfirm(apt) }
        holder.binding.btnDeclineApt.setOnClickListener { onDecline(apt) }
    }

    override fun getItemCount() = appointments.size
}