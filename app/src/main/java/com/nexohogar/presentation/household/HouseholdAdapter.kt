package com.nexohogar.presentation.household

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nexohogar.domain.model.Household
import com.nexohogar.databinding.ItemHouseholdBinding

/**
 * Adaptador para mostrar la lista de hogares en un RecyclerView.
 * Utiliza modelos de Dominio (Household) en lugar de DTOs.
 */
class HouseholdAdapter(
    private val onHouseholdClick: (Household) -> Unit
) : RecyclerView.Adapter<HouseholdAdapter.ViewHolder>() {

    private var items: List<Household> = emptyList()

    fun submitList(newItems: List<Household>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHouseholdBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemHouseholdBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Household) {
            binding.tvName.text = item.name
            binding.tvDescription.text = item.description
            binding.root.setOnClickListener { onHouseholdClick(item) }
        }
    }
}
