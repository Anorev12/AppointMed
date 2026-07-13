package com.example.appointmed.features.admin.adapters
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appointmed.databinding.ItemAdminSimpleBinding
import com.example.appointmed.features.admin.models.AdminSimpleResponse

/** Read-only admin roster row — deliberately no delete action, since admins can never delete another admin account. */
class AdminSimpleAdapter(
    private val admins: List<AdminSimpleResponse>
) : RecyclerView.Adapter<AdminSimpleAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAdminSimpleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminSimpleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val admin = admins[position]
        holder.binding.tvAdminSimpleName.text = admin.fullName
        holder.binding.tvAdminSimpleEmail.text = admin.email
    }

    override fun getItemCount() = admins.size
}
