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
    private val onCancel: (Appointment) -> Unit
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
        holder.binding.tvAptDateTime.text = "${apt.date} · ${apt.time}"
        holder.binding.tvAptRef.text = apt.id

        val isConfirmed = apt.status == "confirmed"
        holder.binding.tvAptStatus.text = apt.status.replaceFirstChar { it.uppercase() }
        holder.binding.tvAptStatus.setBackgroundColor(
            ContextCompat.getColor(context, if (isConfirmed) R.color.success_soft else R.color.danger_soft)
        )
        holder.binding.tvAptStatus.setTextColor(
            ContextCompat.getColor(context, if (isConfirmed) R.color.success else R.color.danger)
        )

        holder.binding.btnCancelApt.visibility = if (isConfirmed) View.VISIBLE else View.GONE
        holder.binding.btnCancelApt.setOnClickListener {
            onCancel(apt)
        }
    }

    override fun getItemCount() = appointments.size
}