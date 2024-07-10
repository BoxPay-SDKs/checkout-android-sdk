package com.boxpay.checkout.sdk.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.boxpay.checkout.sdk.R
import com.boxpay.checkout.sdk.databinding.OrderSummaryItemBinding
import com.squareup.picasso.Picasso

class OrderSummaryItemsAdapter(
    private val imagesUrls: MutableList<String>?,
    private val items: MutableList<String>,
    private val prices: MutableList<String>,
    private val context: Context
) : RecyclerView.Adapter<OrderSummaryItemsAdapter.OrderSummaryViewHolder>() {
    val sharedPreferences =
        context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
    inner class OrderSummaryViewHolder(private val binding: OrderSummaryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            binding.apply {
                if(!imagesUrls.isNullOrEmpty() && imagesUrls[position] != "null" && imagesUrls[position].isNotEmpty()) {
                    Picasso.get()
                        .load(imagesUrls[position])
                        .into(binding.itemImage)
                } else {
                    binding.itemImage.setImageResource(R.drawable.ic_placeholder_image )
                }
                val currencySymbol = sharedPreferences.getString("currencySymbol","")
                itemPrice.text = currencySymbol+prices[position]
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