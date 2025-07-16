package com.boxpay.checkout.sdk.adapters

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import coil.decode.SvgDecoder
import coil.load
import com.boxpay.checkout.sdk.R
import com.boxpay.checkout.sdk.databinding.RecommendedRowItemBinding
import com.boxpay.checkout.sdk.dataclasses.SavedRecommended

class SavedUpiItemsAdaptor(
    private val items: MutableList<SavedRecommended>,
    context: Context
) : RecyclerView.Adapter<SavedUpiItemsAdaptor.SavedUpiItemViewHolder>() {

    var checkPositionLiveData = MutableLiveData(RecyclerView.NO_POSITION)

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)

    var checkedPosition: Int
        get() = checkPositionLiveData.value ?: RecyclerView.NO_POSITION
        set(value) {
            handleRadioButtonClick(value)
        }

    inner class SavedUpiItemViewHolder(val binding: RecommendedRowItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    handleRadioButtonClick(position)
                }
            }
        }

        fun bind(position: Int) {
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
                }
            } else {
                binding.radioButton.setBackgroundResource(R.drawable.custom_radio_unchecked)
            }

            binding.recomededItemText.text = items[position].displayValue
            binding.recomendedLogo.load(items[position].logoUrl) {
                decoderFactory { result, options, _ -> SvgDecoder(result.source, options) }
                size(70, 70)
            }

            binding.divider.visibility = if (position == items.size - 1) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedUpiItemViewHolder {
        return SavedUpiItemViewHolder(
            RecommendedRowItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: SavedUpiItemViewHolder, position: Int) {
        holder.bind(position)
    }

    private fun handleRadioButtonClick(position: Int) {
        val previousPosition = checkPositionLiveData.value ?: RecyclerView.NO_POSITION

        if (previousPosition != position) {
            checkPositionLiveData.value = position
            if (previousPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousPosition)
            }
            notifyItemChanged(position)
        }
    }
}