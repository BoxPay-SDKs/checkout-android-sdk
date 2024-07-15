package com.boxpay.checkout.sdk.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.widget.ImageView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import coil.decode.SvgDecoder
import coil.load
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.R
import com.boxpay.checkout.sdk.databinding.WalletItemBinding
import com.boxpay.checkout.sdk.dataclasses.WalletDataClass
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon
import org.json.JSONObject
import java.util.Locale

class WalletAdapter(
    private val walletDetails: ArrayList<WalletDataClass>,
    private val recyclerView: RecyclerView,
    private var liveDataPopularItemSelectedOrNot: MutableLiveData<Boolean>,
    private val context: Context,
    private val searchView : android.widget.SearchView,
    private val token : String
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
                    val layerDrawable = radioButtonDrawable as LayerDrawable

                    // Modify the solid color of the first item (assuming it's a GradientDrawable)
                    val shapeDrawable = layerDrawable.getDrawable(0) as? GradientDrawable
                    shapeDrawable?.setColor(Color.parseColor(sharedPreferences.getString("primaryButtonColor","#0D8EFF"))) // Change color to red dynamically

                    // Apply the modified drawable back to the radioButton ImageView
                    radioButton.background = layerDrawable
                }

                val walletName = walletDetails[position].walletName
                val walletImage = walletDetails[position].walletImage
                val displayMetrics = context.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val averageWidthPerCharacter = walletNameTextView.paint.measureText("A")

                val maxCharacters = (screenWidth / averageWidthPerCharacter).toInt()

                if (walletName.length > maxCharacters) {
                    walletNameTextView.text = "${walletName.substring(0, maxCharacters)}..."
                } else {
                    walletNameTextView.text = walletName
                }
                binding.walletLogo.load(walletImage){
                    decoderFactory{result,options,_ -> SvgDecoder(result.source,options) }
                    size(80, 80)
                }


                if(position == checkedPosition){
                    if (radioButtonDrawable is LayerDrawable) {
                        val layerDrawable = radioButtonDrawable as LayerDrawable

                        // Modify the solid color of the first item (assuming it's a GradientDrawable)
                        val shapeDrawable = layerDrawable.getDrawable(0) as? GradientDrawable
                        shapeDrawable?.setColor(Color.parseColor(sharedPreferences.getString("primaryButtonColor","#0D8EFF"))) // Change color to red dynamically

                        // Apply the modified drawable back to the radioButton ImageView
                        radioButton.background = layerDrawable
                    }
                }else{
                    radioButton.setBackgroundResource(R.drawable.custom_radio_unchecked)
                }


                val radioButtonColor = sharedPreferences.getString("primaryButtonColor","#0D8EFF")

                // Set a click listener for the RadioButton
                binding.root.setOnClickListener {
                    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(searchView.windowToken, 0)
                    handleRadioButtonClick(adapterPosition,binding.radioButton)
                    liveDataPopularItemSelectedOrNot.value = false
                    callUIAnalytics(context,"PAYMENT_INSTRUMENT_PROVIDED",walletDetails[position].walletBrand,"Wallet")
                    callUIAnalytics(context,"PAYMENT_METHOD_SELECTED",walletDetails[position].walletBrand,"Wallet")
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
    private fun callUIAnalytics(context: Context, event: String,paymentSubType : String, paymentType : String) {
        val environmentFetched = sharedPreferences.getString("environment", "null")

        val requestQueue = Volley.newRequestQueue(context)
        val userAgentHeader = WebSettings.getDefaultUserAgent(context)
        val browserLanguage = Locale.getDefault().toString()

        // Constructing the request body
        val requestBody = JSONObject().apply {
            put("callerToken", token)
            put("uiEvent", event)

            // Create eventAttrs JSON object
            val eventAttrs = JSONObject().apply {
                put("paymentType", paymentType)
                put("paymentSubType", paymentSubType)
            }
            put("eventAttrs", eventAttrs)

            // Create browserData JSON object
            val browserData = JSONObject().apply {
                put("userAgentHeader", userAgentHeader)
                put("browserLanguage", browserLanguage)
            }
            put("browserData", browserData)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, "https://${environmentFetched}apis.boxpay.tech/v0/ui-analytics", requestBody,
            Response.Listener { _ ->
                // no op
            },
            Response.ErrorListener { _ ->
               // no op
            }) {

        }.apply {
            // Set retry policy
            val timeoutMs = 100000 // Timeout in milliseconds
            val maxRetries = 0 // Max retry attempts
            val backoffMultiplier = 1.0f // Backoff multiplier
            retryPolicy = DefaultRetryPolicy(timeoutMs, maxRetries, backoffMultiplier)
        }

        // Add the request to the RequestQueue.
        requestQueue.add(jsonObjectRequest)
    }
    fun extractMessageFromErrorResponse(response: String): String? {
        try {
            // Parse the JSON string
            val jsonObject = JSONObject(response)
            // Retrieve the value associated with the "message" key
            return jsonObject.getString("message")
        } catch (e: Exception) {
            // Handle JSON parsing exception
        }
        return null
    }

    private fun handleRadioButtonClick(position: Int, imageView : ImageView) {
        if (checkedPosition != position) {
            // Change the background of the previously checked RadioButton
            val previousCheckedViewHolder =
                recyclerView.findViewHolderForAdapterPosition(checkedPosition) as? WalletAdapterViewHolder
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
                recyclerView.findViewHolderForAdapterPosition(position) as? WalletAdapterViewHolder
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
                recyclerView.findViewHolderForAdapterPosition(checkedPosition) as? WalletAdapterViewHolder
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