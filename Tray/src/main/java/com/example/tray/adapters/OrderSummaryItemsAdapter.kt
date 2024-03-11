package com.example.tray.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tray.databinding.OrderSummaryItemBinding

class OrderSummaryItemsAdapter(
    private val images: MutableList<Int>,
    private val items: MutableList<String>,
    private val prices: MutableList<String>,
    private val taxes: MutableList<String>
) : RecyclerView.Adapter<OrderSummaryItemsAdapter.OrderSummaryViewHolder>() {
    inner class OrderSummaryViewHolder(private val binding: OrderSummaryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.apply {
                itemImage.setImageResource(images[position])
                subItemPrice.text = "₹${prices[position]}"
                itemName.text = items[position]
                if (taxes[position] == "null") {
                    binding.textView19.visibility = View.GONE
                    binding.taxTextView.visibility = View.GONE
                } else {
                    binding.taxTextView.text = "₹${taxes[position]}"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderSummaryViewHolder {
        return OrderSummaryViewHolder(
            OrderSummaryItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: OrderSummaryViewHolder, position: Int) {
        holder.bind(position)
    }
}