package com.example.tray.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.tray.R
import com.example.tray.databinding.WalletItemBinding
import com.example.tray.dataclasses.WalletDataClass
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon

class WalletAdapter(
    private val walletDetails: ArrayList<WalletDataClass>,
    private val recyclerView: RecyclerView,
    private var liveDataPopularItemSelectedOrNot: MutableLiveData<Boolean>,
    private val context: Context,
    private val searchView : android.widget.SearchView
) : RecyclerView.Adapter<WalletAdapter.WalletAdapterViewHolder>() {
    private var checkedPosition = RecyclerView.NO_POSITION
    var checkPositionLiveData = MutableLiveData<Int>()

    inner class WalletAdapterViewHolder(val binding: WalletItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
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
                    Log.d("keyboard should hide now","WalletAdapter")
                    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(searchView.windowToken, 0)
                    handleRadioButtonClick(adapterPosition)
                    liveDataPopularItemSelectedOrNot.value = false
                }

                val balloon = createBalloon(context) {
                    setArrowSize(10)
                    setWidthRatio(0.3f)
                    setHeight(65)
                    setArrowPosition(0.5f)
                    setCornerRadius(4f)
                    setAlpha(0.9f)
                    setText(walletName)
                    setTextColorResource(R.color.colorEnd)
//                    setIconDrawable(ContextCompat.getDrawable(context, R.drawable.ic_profile))
                    setBackgroundColorResource(R.color.tooltip_bg)
//                    setOnBalloonClickListener(onBalloonClickListener)
                    setBalloonAnimation(BalloonAnimation.FADE)
                    setLifecycleOwner(lifecycleOwner)
                }

                binding.root.setOnLongClickListener { view ->
                    balloon.showAlignTop(binding.root)
                    balloon.dismissWithDelay(2000L)
                    true // Indicate that the long click event has been consumed
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

    fun deselectSelectedItem() {
        if (checkedPosition != RecyclerView.NO_POSITION) {
            val previousCheckedViewHolder =
                recyclerView.findViewHolderForAdapterPosition(checkedPosition) as? WalletAdapter.WalletAdapterViewHolder
            previousCheckedViewHolder?.binding?.radioButton?.setBackgroundResource(
                R.drawable.custom_radio_unchecked
            )
            checkedPosition = RecyclerView.NO_POSITION
            checkPositionLiveData.value = RecyclerView.NO_POSITION
        }
    }

    fun getCheckedPosition(): Int {
        return checkedPosition
    }

}