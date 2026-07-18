package com.example.appointmed.features.admin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appointmed.databinding.ItemNotificationTemplateBinding
import com.example.appointmed.features.admin.models.NotificationTemplateResponse

/** Row per notification type — tapping Edit opens the edit dialog in AdminProfileFragment. */
class NotificationTemplateAdapter(
    private val templates: List<NotificationTemplateResponse>,
    private val onEdit: (NotificationTemplateResponse) -> Unit
) : RecyclerView.Adapter<NotificationTemplateAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemNotificationTemplateBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationTemplateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val template = templates[position]
        holder.binding.tvTemplateLabel.text = template.label
        holder.binding.tvTemplateSubjectPreview.text = template.subjectTemplate
        holder.binding.btnEditTemplate.setOnClickListener { onEdit(template) }
    }

    override fun getItemCount() = templates.size
}
