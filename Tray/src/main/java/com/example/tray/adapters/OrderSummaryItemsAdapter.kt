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
) : RecyclerView.Adapter<OrderSummaryItemsAdapter.OrderSummaryViewHolder>() {
    inner class OrderSummaryViewHolder(private val binding: OrderSummaryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.apply {
                itemImage.setImageResource(images[position])
                itemPrice.text = "₹"+prices[position]+"/M"
                itemName.text = items[position]
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