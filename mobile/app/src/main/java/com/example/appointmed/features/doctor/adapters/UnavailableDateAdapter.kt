package com.example.appointmed.features.doctor.adapters
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appointmed.databinding.ItemUnavailableDateBinding

class UnavailableDateAdapter(
    private val dates: List<String>,
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<UnavailableDateAdapter.DateViewHolder>() {

    inner class DateViewHolder(val binding: ItemUnavailableDateBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val binding = ItemUnavailableDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val date = dates[position]
        holder.binding.tvUnavailableDate.text = date
        holder.binding.btnRemoveDate.setOnClickListener { onRemove(date) }
    }

    override fun getItemCount() = dates.size
}