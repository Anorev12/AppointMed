package com.example.appointmed.features.admin.adapters
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appointmed.databinding.ItemAdminPatientBinding
import com.example.appointmed.features.admin.models.AdminPatient
class AdminPatientAdapter(
    private val patients: List<AdminPatient>
) : RecyclerView.Adapter<AdminPatientAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAdminPatientBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminPatientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val patient = patients[position]
        holder.binding.tvPatientNameAdmin.text = patient.name
        holder.binding.tvPatientEmailAdmin.text = patient.email
        holder.binding.tvPatientContactAdmin.text = patient.contact
        // TODO: no per-patient history endpoint exists yet — wire this
        // button once /api/admin/patients/{id}/history (or similar) exists.
        holder.binding.btnViewHistory.setOnClickListener { }
    }

    override fun getItemCount() = patients.size
}