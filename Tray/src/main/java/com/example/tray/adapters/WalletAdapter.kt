package com.example.tray.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.Image
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import coil.decode.SvgDecoder
import coil.load
import com.example.tray.R
import com.example.tray.databinding.WalletItemBinding
import com.example.tray.dataclasses.WalletDataClass
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon
import com.squareup.picasso.Picasso

class WalletAdapter(
    private val walletDetails: ArrayList<WalletDataClass>,
    private val recyclerView: RecyclerView,
    private var liveDataPopularItemSelectedOrNot: MutableLiveData<Boolean>,
    private val context: Context,
    private val searchView : android.widget.SearchView
) : RecyclerView.Adapter<WalletAdapter.WalletAdapterViewHolder>() {
    private var checkedPosition = RecyclerView.NO_POSITION
    var checkPositionLiveData = MutableLiveData<Int>()
    val sharedPreferences =
        context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)

    inner class WalletAdapterViewHolder(val binding: WalletItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.apply {

                binding.walletNameTextView.text = walletDetails[position].walletName

                // Get the background drawable of radioButton
                val radioButtonDrawable = radioButton.background

                // Check if the background drawable is a LayerDrawable
                if (radioButtonDrawable is LayerDrawable) {
                    Log.d("Drawable found","success")
                    val layerDrawable = radioButtonDrawable as LayerDrawable

                    // Modify the solid color of the first item (assuming it's a GradientDrawable)
                    val shapeDrawable = layerDrawable.getDrawable(0) as? GradientDrawable
                    Log.d("set color function",sharedPreferences.getString("primaryButtonColor","#0D8EFF").toString())
                    shapeDrawable?.setColor(Color.parseColor(sharedPreferences.getString("primaryButtonColor","#0D8EFF"))) // Change color to red dynamically

                    // Apply the modified drawable back to the radioButton ImageView
                    radioButton.background = layerDrawable
                }else{
                    Log.d("Drawable found","failure")
                }


                val walletName = walletDetails[position].walletName
                val walletImage = walletDetails[position].walletImage
                if (walletName.length >= 21) {
                    walletName.substring(0, 21) + "..."
                } else {
                    walletNameTextView.text = walletName
                }
                binding.walletLogo.load(walletImage){
                    decoderFactory{result,options,_ -> SvgDecoder(result.source,options) }
                    size(80, 80)
                }


                if(position == checkedPosition){
                    Log.d("Drawable found","failure if condition wallet")
                    if (radioButtonDrawable is LayerDrawable) {
                        Log.d("Drawable found","success")
                        val layerDrawable = radioButtonDrawable as LayerDrawable

                        // Modify the solid color of the first item (assuming it's a GradientDrawable)
                        val shapeDrawable = layerDrawable.getDrawable(0) as? GradientDrawable
                        shapeDrawable?.setColor(Color.parseColor(sharedPreferences.getString("primaryButtonColor","#0D8EFF"))) // Change color to red dynamically

                        // Apply the modified drawable back to the radioButton ImageView
                        radioButton.background = layerDrawable
                    }else{
                        Log.d("Drawable found","failure")
                    }
                }else{
                    Log.d("Drawable found","failure else condition wallet")
                    radioButton.setBackgroundResource(R.drawable.custom_radio_unchecked)
                }

//                if(position == checkedPosition){
//                    radioButton.setBackgroundResource(R.drawable.custom_radio_unchecked)
//                }else{
//                    radioButton.setBackgroundResource(R.drawable.custom_radio_checked)
//                }

                val radioButtonColor = sharedPreferences.getString("primaryButtonColor","#0D8EFF")
                Log.d("radioButtonColor wallet",radioButtonColor.toString())

                // Set a click listener for the RadioButton
                binding.root.setOnClickListener {
                    Log.d("keyboard should hide now","WalletAdapter")
                    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(searchView.windowToken, 0)
                    handleRadioButtonClick(adapterPosition,binding.radioButton)
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

    private fun handleRadioButtonClick(position: Int, imageView : ImageView) {
        if (checkedPosition != position) {
            // Change the background of the previously checked RadioButton
            val previousCheckedViewHolder =
                recyclerView.findViewHolderForAdapterPosition(checkedPosition) as? WalletAdapter.WalletAdapterViewHolder
            previousCheckedViewHolder?.binding?.radioButton?.setBackgroundResource(
                R.drawable.custom_radio_unchecked
            )

            imageView.setBackgroundResource(R.drawable.custom_radio_checked)

            val radioButtonDrawable = imageView.background

            // Check if the background drawable is a LayerDrawable
            if (radioButtonDrawable is LayerDrawable) {
                Log.d("Drawable found","success")
                val layerDrawable = radioButtonDrawable as LayerDrawable

                // Modify the solid color of the first item (assuming it's a GradientDrawable)
                val shapeDrawable = layerDrawable.getDrawable(0) as? GradientDrawable
                shapeDrawable?.setColor(Color.parseColor(sharedPreferences.getString("primaryButtonColor","#0D8EFF"))) // Change color to red dynamically

                // Apply the modified drawable back to the radioButton ImageView
                imageView.background = layerDrawable
            }else{
                Log.d("Drawable found","failure in handle click")
            }

//             Change the background of the clicked RadioButton
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