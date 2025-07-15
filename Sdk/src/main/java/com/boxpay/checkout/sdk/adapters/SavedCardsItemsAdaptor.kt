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
    private val recyclerView: RecyclerView,
    private val context: Context
) : RecyclerView.Adapter<SavedCardsItemsAdaptor.SavedCardsItemViewHolder>() {
    var checkPositionLiveData =
        MutableLiveData(RecyclerView.NO_POSITION)
    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)

    init {
        // Observe checkPositionLiveData to refresh the views whenever it changes
        checkPositionLiveData.observeForever {
            notifyDataSetChanged() // Refresh the entire list
        }
    }

    inner class SavedCardsItemViewHolder(val binding: SavedCardsRowItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            val radioButtonDrawable = binding.radioButton.background

            // Check if the background drawable is a LayerDrawable
            if (radioButtonDrawable is LayerDrawable) {
                val layerDrawable = radioButtonDrawable as LayerDrawable

                // Modify the solid color of the first item (assuming it's a GradientDrawable)
                val shapeDrawable = layerDrawable.getDrawable(0) as? GradientDrawable
                shapeDrawable?.setColor(
                    Color.parseColor(
                        sharedPreferences.getString(
                            "primaryButtonColor",
                            "#0D8EFF"
                        )
                    )
                ) // Change color to red dynamically

                // Apply the modified drawable back to the radioButton ImageView
                binding.radioButton.background = layerDrawable
            }
            if (position == checkPositionLiveData.value) {
                if (radioButtonDrawable is LayerDrawable) {
                    val layerDrawable = radioButtonDrawable as LayerDrawable

                    // Modify the solid color of the first item (assuming it's a GradientDrawable)
                    val shapeDrawable = layerDrawable.getDrawable(0) as? GradientDrawable
                    shapeDrawable?.setColor(
                        Color.parseColor(
                            sharedPreferences.getString(
                                "primaryButtonColor",
                                "#0D8EFF"
                            )
                        )
                    ) // Change color to red dynamically

                    // Apply the modified drawable back to the radioButton ImageView
                    binding.radioButton.background = layerDrawable
                }
            } else {
                binding.radioButton.setBackgroundResource(R.drawable.custom_radio_unchecked)
            }
            binding.apply {
                if(items[position].cardHolderName == null) {
                    savedCardHolderName.visibility = View.GONE
                } else {
                    savedCardHolderName.text = items[position].cardHolderName
                }
                savedCardNumber.text = items[position].cardNumber
                savedCardLogo.load(items[position].cardIcon){
                    decoderFactory{result,options,_ -> SvgDecoder(result.source,options) }
                    size(70, 70)
                }
            }
            binding.root.setOnClickListener {
                handleRadioButtonClick(adapterPosition, binding.radioButton)
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

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: SavedCardsItemViewHolder, position: Int) {
        holder.bind(position)
    }

    private fun handleRadioButtonClick(position: Int, imageView: ImageView) {
        if (checkPositionLiveData.value != position) {
            // Change the background of the previously checked RadioButton
            val previousCheckedViewHolder =
                recyclerView.findViewHolderForAdapterPosition(
                    checkPositionLiveData.value ?: 0
                ) as? SavedCardsItemViewHolder
            previousCheckedViewHolder?.binding?.radioButton?.setBackgroundResource(
                R.drawable.custom_radio_unchecked
            )

            imageView.setBackgroundResource(R.drawable.custom_radio_checked)

            val radioButtonDrawable = imageView.background

            // Check if the background drawable is a LayerDrawable
            if (radioButtonDrawable is LayerDrawable) {
                val layerDrawable = radioButtonDrawable as LayerDrawable

                // Modify the solid color of the first item (assuming it's a GradientDrawable)
                val shapeDrawable = layerDrawable.getDrawable(0) as? GradientDrawable
                shapeDrawable?.setColor(
                    Color.parseColor(
                        sharedPreferences.getString(
                            "primaryButtonColor",
                            "#0D8EFF"
                        )
                    )
                ) // Change color to red dynamically

                // Apply the modified drawable back to the radioButton ImageView
                imageView.background = layerDrawable
            } else {
            }

//             Change the background of the clicked RadioButton
            val clickedViewHolder =
                recyclerView.findViewHolderForAdapterPosition(position) as? SavedCardsItemViewHolder
            clickedViewHolder?.binding?.radioButton?.setBackgroundResource(
                R.drawable.custom_radio_checked
            )

            // Update the checked position
            checkPositionLiveData.value = position
        }
    }
}