package com.example.tray.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.tray.R
import com.example.tray.databinding.WalletItemBinding

import com.example.tray.dataclasses.WalletDataClass

class WalletAdapter(
    private val walletDetails: ArrayList<WalletDataClass>,
    private val recyclerView: RecyclerView
) : RecyclerView.Adapter<WalletAdapter.WalletAdapterViewHolder>() {
    private var checkedPosition = RecyclerView.NO_POSITION
    var checkPositionLiveData = MutableLiveData<Int>()
    inner class WalletAdapterViewHolder(val binding: WalletItemBinding) :
        RecyclerView.ViewHolder(binding.root){

        fun bind(position: Int) {
            binding.apply {
                val walletName = walletDetails[position].walletName
                val walletImage = walletDetails[position].walletImage
                if (walletName.length >= 21) {
                    walletName.substring(0, 21) + "..."
                } else {
                    walletNameTextView.text = walletName
                }
                walletLogo.setImageResource(walletImage)


                radioButton.setBackgroundResource(
                    if (position == checkedPosition) {
                        R.drawable.custom_radio_checked
                    } else {
                        R.drawable.custom_radio_unchecked
                    }
                )
                // Set a click listener for the RadioButton
                binding.root.setOnClickListener {
                    handleRadioButtonClick(adapterPosition)
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletAdapterViewHolder {
        return WalletAdapterViewHolder(
            WalletItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return walletDetails.size
    }

    override fun onBindViewHolder(holder: WalletAdapterViewHolder, position: Int) {
        holder.bind(position)
    }
    private fun handleRadioButtonClick(position: Int) {
        if (checkedPosition != position) {
            // Change the background of the previously checked RadioButton
            val previousCheckedViewHolder =
                recyclerView.findViewHolderForAdapterPosition(checkedPosition) as? WalletAdapter.WalletAdapterViewHolder
            previousCheckedViewHolder?.binding?.radioButton?.setBackgroundResource(
                R.drawable.custom_radio_unchecked
            )

            // Change the background of the clicked RadioButton
            val clickedViewHolder =
                recyclerView.findViewHolderForAdapterPosition(position) as? WalletAdapter.WalletAdapterViewHolder
            clickedViewHolder?.binding?.radioButton?.setBackgroundResource(
                R.drawable.custom_radio_checked
            )

            // Update the checked position
            checkedPosition = position
            checkPositionLiveData.value = checkedPosition
        }
    }
    fun getCheckedPosition() : Int{
        return checkedPosition
    }

}