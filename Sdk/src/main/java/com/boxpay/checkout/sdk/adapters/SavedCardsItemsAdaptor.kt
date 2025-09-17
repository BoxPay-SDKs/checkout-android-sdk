package com.boxpay.checkout.sdk.adapters

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import coil.decode.SvgDecoder
import coil.load
import com.boxpay.checkout.sdk.R
import com.boxpay.checkout.sdk.databinding.SavedCardsRowItemBinding
import com.boxpay.checkout.sdk.dataclasses.SavedCard

class SavedCardsItemsAdaptor(
    private val items: MutableList<SavedCard>,
    private val context: Context
) : RecyclerView.Adapter<SavedCardsItemsAdaptor.SavedCardsItemViewHolder>() {

    var checkPositionLiveData = MutableLiveData(RecyclerView.NO_POSITION)
    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)

    init {
        checkPositionLiveData.observeForever { newPos ->
            val oldPos = previousCheckedPos
            if (oldPos != RecyclerView.NO_POSITION) notifyItemChanged(oldPos)
            if (newPos != RecyclerView.NO_POSITION) notifyItemChanged(newPos)
            previousCheckedPos = newPos
        }
    }

    private var previousCheckedPos: Int = RecyclerView.NO_POSITION

    inner class SavedCardsItemViewHolder(val binding: SavedCardsRowItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            // Radio button state
            if (position == checkPositionLiveData.value) {
                binding.radioButton.setBackgroundResource(R.drawable.custom_radio_checked)

                val radioButtonDrawable = binding.radioButton.background
                if (radioButtonDrawable is LayerDrawable) {
                    val shapeDrawable = radioButtonDrawable.getDrawable(0) as? GradientDrawable
                    shapeDrawable?.setColor(
                        Color.parseColor(
                            sharedPreferences.getString("primaryButtonColor", "#0D8EFF")
                        )
                    )
                    binding.radioButton.background = radioButtonDrawable
                }
            } else {
                binding.radioButton.setBackgroundResource(R.drawable.custom_radio_unchecked)
            }

            // Card details
            binding.apply {
                if (items[position].cardHolderName == null) {
                    savedCardHolderName.visibility = View.GONE
                } else {
                    savedCardHolderName.text = items[position].cardHolderName
                }
                savedCardNumber.text = items[position].cardNumber
                savedCardLogo.load(items[position].cardIcon) {
                    decoderFactory { result, options, _ -> SvgDecoder(result.source, options) }
                    size(70, 70)
                }
            }

            // Click listener
            binding.root.setOnClickListener {
                handleRadioButtonClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedCardsItemViewHolder {
        return SavedCardsItemViewHolder(
            SavedCardsRowItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SavedCardsItemViewHolder, position: Int) {
        holder.bind(position)
    }

    private fun handleRadioButtonClick(position: Int) {
        if (checkPositionLiveData.value != position) {
            checkPositionLiveData.value = position
        }
    }
}
