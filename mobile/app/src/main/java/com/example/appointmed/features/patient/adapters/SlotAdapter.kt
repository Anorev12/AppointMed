package com.example.appointmed.features.patient.adapters
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appointmed.R
import com.example.appointmed.databinding.ItemSlotBinding
import com.example.appointmed.features.patient.models.TimeSlot
class SlotAdapter(
    private val slots: List<TimeSlot>,
    private val onSelect: (TimeSlot) -> Unit
) : RecyclerView.Adapter<SlotAdapter.SlotViewHolder>() {

    private var selectedTime: String? = null

    inner class SlotViewHolder(val binding: ItemSlotBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val binding = ItemSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = slots[position]
        val context = holder.binding.root.context
        val btn = holder.binding.btnSlot

        btn.text = slot.time
        btn.isEnabled = !slot.reserved

        when {
            slot.reserved -> {
                btn.setBackgroundColor(ContextCompat.getColor(context, R.color.border))
                btn.setTextColor(ContextCompat.getColor(context, R.color.ink_soft))
            }
            slot.time == selectedTime -> {
                btn.setBackgroundColor(ContextCompat.getColor(context, R.color.stamp))
                btn.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            }
            else -> {
                btn.setBackgroundColor(ContextCompat.getColor(context, R.color.paper_card))
                btn.setTextColor(ContextCompat.getColor(context, R.color.ink))
            }
        }

        btn.setOnClickListener {
            if (!slot.reserved) {
                selectedTime = slot.time
                notifyDataSetChanged()
                onSelect(slot)
            }
        }
    }

    override fun getItemCount() = slots.size
}