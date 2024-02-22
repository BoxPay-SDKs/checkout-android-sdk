package com.example.tray.adapters

import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.tray.R
import com.example.tray.databinding.NetbankingBanksItemBinding
import com.example.tray.dataclasses.NetbankingDataClass
import kotlinx.coroutines.newSingleThreadContext
import java.util.Locale


class NetbankingBanksAdapter(
    private val banksDetails: ArrayList<NetbankingDataClass>,
    private val recyclerView: RecyclerView,
    private var liveDataPopularItemSelectedOrNot : MutableLiveData<Boolean>
) : RecyclerView.Adapter<NetbankingBanksAdapter.NetBankingAdapterViewHolder>() {
    private var checkedPosition = RecyclerView.NO_POSITION
    var checkPositionLiveData = MutableLiveData<Int>()
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
                    liveDataPopularItemSelectedOrNot.value = false
                }

                binding.root.setOnLongClickListener { view ->
                    showTooltip(view, bankName)
                    Log.d("long click detected","net banking adapter")
                    true // Indicate that the long click event has been consumed
                }

            }
        }

    }

    private fun showTooltip(anchorView: View, text: String) {
        // Inflate your tooltip layout
        val tooltipView = LayoutInflater.from(anchorView.context).inflate(R.layout.tooltip_wallet_netbanking_layout, null)
        val textView = tooltipView.findViewById<TextView>(R.id.tooltip_text)
        textView.text = text

        // Create a PopupWindow
        val popupWindow = PopupWindow(
            tooltipView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.contentView = tooltipView

        // Calculate the position of the anchor view on screen
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)

        // Show tooltip above the anchor view
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, location[0], location[1] - tooltipView.height - anchorView.height)

        Handler().postDelayed({
            popupWindow.dismiss()
        }, 3000)
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
    fun deselectSelectedItem() {
        if (checkedPosition != RecyclerView.NO_POSITION) {
            val previousCheckedViewHolder =
                recyclerView.findViewHolderForAdapterPosition(checkedPosition) as? NetBankingAdapterViewHolder
            previousCheckedViewHolder?.binding?.radioButton?.setBackgroundResource(
                R.drawable.custom_radio_unchecked
            )
            checkedPosition = RecyclerView.NO_POSITION
            checkPositionLiveData.value = RecyclerView.NO_POSITION
        }
    }

    fun getCheckedPosition() : Int{
        return checkedPosition
    }
}