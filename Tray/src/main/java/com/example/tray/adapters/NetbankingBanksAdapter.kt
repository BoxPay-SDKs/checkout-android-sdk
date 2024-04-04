package com.example.tray.adapters

import android.content.Context

import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import coil.decode.SvgDecoder
import coil.load
import com.example.tray.R
import com.example.tray.databinding.NetbankingBanksItemBinding
import com.example.tray.dataclasses.NetbankingDataClass
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon
import java.io.InputStream


class NetbankingBanksAdapter(
    private val banksDetails: ArrayList<NetbankingDataClass>,
    private val recyclerView: RecyclerView,
    private var liveDataPopularItemSelectedOrNot : MutableLiveData<Boolean>,
    private val context : Context,
    private val searchView : android.widget.SearchView
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

                Log.d("imageURL",banksDetails[position].bankImage)
//                Picasso.get()
//                    .load(banksDetails[position].bankImage)
//                    .placeholder(R.drawable.netbanking_sample_logo)
//                    .into(binding.bankLogo)


//                val options = RequestOptions()
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .skipMemoryCache(true) // Skip caching the placeholder image
//                    .placeholder(R.drawable.netbanking_sample_logo) // Placeholder image resource
//
//                Glide.with(context)
//                    .`as`(PictureDrawable::class.java)
//                    .load("https://checkout.boxpay.in/assets/Icons/Bank%20Icons/OrientalBankOfCommerce.svg")
//                    .apply(options)
//                    .into(binding.bankLogo)

//                imageView.setImageResource(R.drawable.)


                binding.bankLogo.load(bankImage){
                    decoderFactory{result,options,_ -> SvgDecoder(result.source,options)}
                    size(80, 80)
                }







                radioButton.setBackgroundResource(
                    if (position == checkedPosition) {
                        R.drawable.custom_radio_checked
                    } else {
                        R.drawable.custom_radio_unchecked
                    }
                )
                // Set a click listener for the RadioButton
                binding.root.setOnClickListener {
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
                    setText(bankName)
                    setTextColorResource(R.color.colorEnd)
//                    setIconDrawable(ContextCompat.getDrawable(context, R.drawable.ic_profile))
                    setBackgroundColorResource(R.color.tooltip_bg)
//                    setOnBalloonClickListener(onBalloonClickListener)
                    setBalloonAnimation(BalloonAnimation.FADE)
                    setLifecycleOwner(lifecycleOwner)
                }

                binding.root.setOnLongClickListener { view ->

                    Log.d("long click detected","net banking adapter")
                    balloon.showAlignTop(binding.root)
                    balloon.dismissWithDelay(2000L)
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