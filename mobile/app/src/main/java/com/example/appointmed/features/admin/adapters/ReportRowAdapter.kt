package com.example.appointmed.features.admin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appointmed.databinding.ItemReportRowBinding
import com.example.appointmed.features.admin.models.ReportRow

/** Generic label/value row list — reused for appointment-status, doctor-load, and notification-status tables (FR-035). */
class ReportRowAdapter(
    private val rows: List<ReportRow>
) : RecyclerView.Adapter<ReportRowAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemReportRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReportRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]
        holder.binding.tvReportRowLabel.text = row.label
        holder.binding.tvReportRowValue.text = row.value
    }

    override fun getItemCount() = rows.size
}