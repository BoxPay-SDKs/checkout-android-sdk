package com.example.tray.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.tray.R
import com.example.tray.databinding.NetbankingBanksItemBinding
import com.example.tray.dataclasses.NetbankingDataClass
import java.util.Locale


class NetbankingBanksAdapter(
    private val banksDetails: ArrayList<NetbankingDataClass>,
    private val recyclerView: RecyclerView
) : RecyclerView.Adapter<NetbankingBanksAdapter.NetBankingAdapterViewHolder>() {
    private var checkedPosition = RecyclerView.NO_POSITION
    var checkPositionLiveData = MutableLiveData<Int>()
    private var filteredBankList: List<NetbankingDataClass> = banksDetails
    inner class NetBankingAdapterViewHolder(val binding: NetbankingBanksItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.apply {
                val bankName = banksDetails[position].bankName
                val bankImage = banksDetails[position].bankImage
                if (bankName.length >= 21) {
                    bankNameTextView.text = bankName.substring(0, 21) + "..."
                } else {
                    bankNameTextView.text = bankName // You were missing this assignment
                }
                bankLogo.setImageResource(bankImage)


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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetBankingAdapterViewHolder {
        return NetBankingAdapterViewHolder(
            NetbankingBanksItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return banksDetails.size
    }

    override fun onBindViewHolder(holder: NetBankingAdapterViewHolder, position: Int) {
        holder.bind(position)
    }

    private fun handleRadioButtonClick(position: Int) {
        if (checkedPosition != position) {
            // Change the background of the previously checked RadioButton
            val previousCheckedViewHolder =
                recyclerView.findViewHolderForAdapterPosition(checkedPosition) as? NetBankingAdapterViewHolder
            previousCheckedViewHolder?.binding?.radioButton?.setBackgroundResource(
                R.drawable.custom_radio_unchecked
            )

            // Change the background of the clicked RadioButton
            val clickedViewHolder =
                recyclerView.findViewHolderForAdapterPosition(position) as? NetBankingAdapterViewHolder
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