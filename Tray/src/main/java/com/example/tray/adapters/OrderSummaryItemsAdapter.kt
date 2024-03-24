package com.example.tray.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tray.databinding.OrderSummaryItemBinding
import com.squareup.picasso.Picasso

class OrderSummaryItemsAdapter(
    private val imagesUrls: MutableList<String>,
    private val items: MutableList<String>,
    private val prices: MutableList<String>,
) : RecyclerView.Adapter<OrderSummaryItemsAdapter.OrderSummaryViewHolder>() {
    inner class OrderSummaryViewHolder(private val binding: OrderSummaryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.apply {
                Log.d("imageURL",imagesUrls[position])
                Picasso.get()
                    .load(imagesUrls[position])
                    .into(binding.itemImage)

//                itemImage.setImageResource(images[position])
                itemPrice.text = "₹"+prices[position]
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