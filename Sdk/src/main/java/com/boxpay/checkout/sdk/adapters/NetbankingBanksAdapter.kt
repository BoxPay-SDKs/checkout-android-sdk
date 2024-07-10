package com.boxpay.checkout.sdk.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable

import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import coil.decode.SvgDecoder
import coil.load
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.R
import com.boxpay.checkout.sdk.databinding.NetbankingBanksItemBinding
import com.boxpay.checkout.sdk.dataclasses.NetbankingDataClass
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale


class NetbankingBanksAdapter(
    private val banksDetails: ArrayList<NetbankingDataClass>,
    private val recyclerView: RecyclerView,
    private var liveDataPopularItemSelectedOrNot : MutableLiveData<Boolean>,
    private val context : Context,
    private val searchView : android.widget.SearchView,
    private val token : String
) : RecyclerView.Adapter<NetbankingBanksAdapter.NetBankingAdapterViewHolder>() {
    private var checkedPosition = RecyclerView.NO_POSITION
    var checkPositionLiveData = MutableLiveData<Int>()
    val sharedPreferences =
        context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)

    inner class NetBankingAdapterViewHolder(val binding: NetbankingBanksItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.apply {

                val radioButtonDrawable = radioButton.background

                // Check if the background drawable is a LayerDrawable
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

                val bankName = banksDetails[position].bankName
                val bankImage = banksDetails[position].bankImage
                val displayMetrics = context.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val averageWidthPerCharacter = bankNameTextView.paint.measureText("A")
                Log.d("char before ellipsis",screenWidth.toString()+" "+averageWidthPerCharacter.toString())

                val maxCharacters = (screenWidth / averageWidthPerCharacter).toInt()

                if (bankName.length > maxCharacters) {
                    bankNameTextView.text = "${bankName.substring(0, maxCharacters)}..."
                } else {
                    bankNameTextView.text = bankName
                }

                Log.d("imageURL",banksDetails[position].bankImage)


                binding.bankLogo.load(bankImage){
                    decoderFactory{result,options,_ -> SvgDecoder(result.source,options)}
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
                    Log.d("Drawable found","failure else condition netbanking")
                    radioButton.setBackgroundResource(R.drawable.custom_radio_unchecked)
                }
                // Set a click listener for the RadioButton
                binding.root.setOnClickListener {
                    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(searchView.windowToken, 0)
                    handleRadioButtonClick(adapterPosition,binding.radioButton)
                    liveDataPopularItemSelectedOrNot.value = false

                    callUIAnalytics(context,"PAYMENT_INSTRUMENT_PROVIDED",banksDetails[position].bankBrand,"NetBanking")
                    callUIAnalytics(context,"PAYMENT_METHOD_SELECTED",banksDetails[position].bankBrand,"NetBanking")
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
    private fun callUIAnalytics(context: Context, event: String,paymentSubType : String, paymentType : String) {
        val environmentFetched = sharedPreferences.getString("environment", "null")

        Log.d("postRequestCalled", System.currentTimeMillis().toString())
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
            Response.Listener { response ->
                // Handle response
                try {

                } catch (e: JSONException) {
                    Log.d("status check error", e.toString())
                }

            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("Error", "Detailed error response: $errorResponse")
                    val errorMessage = extractMessageFromErrorResponse(errorResponse).toString()
                    Log.d("Error message", errorMessage)
                }

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
            e.printStackTrace()
        }
        return null
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

    private fun handleRadioButtonClick(position: Int,imageView : ImageView) {
        if (checkedPosition != position) {
            // Change the background of the previously checked RadioButton
            val previousCheckedViewHolder =
                recyclerView.findViewHolderForAdapterPosition(checkedPosition) as? NetBankingAdapterViewHolder
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