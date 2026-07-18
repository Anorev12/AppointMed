package com.example.appointmed.features.admin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appointmed.databinding.ItemReminderOffsetBinding

/** Row per configured reminder offset (hours before appointment), with a remove action. */
class ReminderOffsetAdapter(
    private val offsets: List<Int>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<ReminderOffsetAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemReminderOffsetBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReminderOffsetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hours = offsets[position]
        holder.binding.tvReminderOffset.text =
            if (hours == 1) "1 hour before" else "$hours hours before"
        holder.binding.btnRemoveOffset.setOnClickListener { onRemove(hours) }
    }

    override fun getItemCount() = offsets.size
}
