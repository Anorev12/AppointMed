package com.example.appointmed.features.patient.adapters
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appointmed.R
import com.example.appointmed.databinding.ItemDoctorCardBinding
import com.example.appointmed.features.patient.models.Doctor
class DoctorAdapter(
    private val doctors: List<Doctor>,
    private val onSelect: (Doctor) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    private var selectedId: Long? = null

    inner class DoctorViewHolder(val binding: ItemDoctorCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val binding = ItemDoctorCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DoctorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        val doctor = doctors[position]
        holder.binding.tvDoctorName.text = doctor.name
        holder.binding.tvDoctorSpec.text = doctor.specialization

        val isSelected = doctor.id == selectedId
        val context = holder.binding.root.context
        holder.binding.doctorCard.strokeColor = ContextCompat.getColor(
            context, if (isSelected) R.color.stamp else R.color.border
        )
        holder.binding.doctorCard.strokeWidth = if (isSelected) 2 else 1

        holder.binding.root.setOnClickListener {
            selectedId = doctor.id
            notifyDataSetChanged()
            onSelect(doctor)
        }
    }

    override fun getItemCount() = doctors.size
}