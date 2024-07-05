package com.example.tray

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.databinding.FragmentQuickPayBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject
import java.util.Locale

class QuickPayBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentQuickPayBottomSheetBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var Base_Session_API_URL: String
    private var transactionId: String? = null
    private var token: String? = null
    private var instrumentRef = MutableLiveData<String>()
    private var displayValue : String ?= null
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding = FragmentQuickPayBottomSheetBinding.inflate(layoutInflater, container, false)

        instrumentRef.observe(this, Observer { instrumentRefObserved ->
            if(instrumentRefObserved == null){
                disableProceedButton()
            }else{
                enableProceedButton()
            }
        })
//        val seekBar = binding.sliderButton
//
//        val maxProgress = 100
//        val desiredMinProgress = (maxProgress * 0.15).toInt() // 15% progress
//        val desiredMaxProgress = (maxProgress * 0.85).toInt() // 85% progress
//
//        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                // Check if progress exceeds desired progress
//                if (progress > desiredMaxProgress) {
//                    seekBar?.progress = desiredMaxProgress
//                }
//                if(progress < desiredMinProgress) {
//                    seekBar?.progress = desiredMinProgress
//                }
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {
//                // Not needed for this implementation
//            }
//
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//                // Not needed for this implementation
//                var progress = 0
//                if(seekBar == null){
//                    progress = 0
//                }else{
//                    progress = seekBar.progress
//                }
//
//                if(progress < 80){
//                    seekBar?.progress = desiredMinProgress
//                }else{
//                    seekBar?.progress = desiredMaxProgress
//                }
//            }
//        })


        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        val baseUrl = sharedPreferences.getString("baseUrl", "null")


        token = sharedPreferences.getString("token", "empty")
        Base_Session_API_URL = "https://${baseUrl}/v0/checkout/sessions/"



        fetchLastUsedPaymentMethods()

        binding.proceedButton.setOnClickListener(){
            openUPITimerBottomSheet(displayValue.toString())

        }

        binding.moreOptionsTextView.setOnClickListener(){
            MainBottomSheet().show(parentFragmentManager,"MainBottomSheet")
            dismiss()
        }
        return binding.root
    }

    fun hideLoadingInButton() {
        binding.progressBar.visibility = View.INVISIBLE
        binding.textView6.setTextColor(Color.parseColor(sharedPreferences.getString("buttonTextColor","#000000")))
        binding.textView6.visibility = View.VISIBLE
        binding.proceedButtonRelativeLayout.setBackgroundColor(Color.parseColor(sharedPreferences.getString("primaryButtonColor","#000000")))
        binding.proceedButton.setBackgroundResource(R.drawable.button_bg)
        binding.proceedButton.isEnabled = true
    }

    fun showLoadingInButton() {
        binding.textView6.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.VISIBLE
        val rotateAnimation = ObjectAnimator.ofFloat(binding.progressBar, "rotation", 0f, 360f)
        rotateAnimation.duration = 3000
        rotateAnimation.repeatCount = ObjectAnimator.INFINITE
        binding.proceedButton.isEnabled = false
        rotateAnimation.start()
    }

    private fun openUPITimerBottomSheet(userVPA : String) {
        val bottomSheetFragment = UPITimerBottomSheet.newInstance(userVPA)
        bottomSheetFragment.show(parentFragmentManager, "UPITimerBottomSheet")
    }

    private fun fetchLastUsedPaymentMethods() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val requestQueue = Volley.newRequestQueue(context)
        val baseUrl = sharedPreferences.getString("baseUrl", "null")
        val url =
            "https://test-apis.boxpay.tech/v0/shoppers/+919818198330/instruments"
        val jsonArrayRequest = object : JsonArrayRequest(
            Method.GET, url, null,
            Response.Listener { response ->
                try {
                    if (response.length() >= 1) {
                        val latestUsedMethod = response.getJSONObject(0)
                        val type = latestUsedMethod.getString("type")
                        displayValue = latestUsedMethod.getString("displayValue")
                        val builder = SpannableStringBuilder()
                        builder.append(type)
                        builder.append(" | ")
                        val valueStartIndex = builder.length
                        builder.append(displayValue)
                        builder.setSpan(StyleSpan(Typeface.BOLD), valueStartIndex, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        binding.paymentDetailsTextView.text = builder
                        instrumentRef.value = latestUsedMethod.getString("instrumentRef")
                    }
                } catch (e: Exception) {
                }

            },
            Response.ErrorListener { _ ->

            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Session 803fdcc1-e0d7-4d1c-929d-5f45d804f38d"
                return headers
            }
        }.apply {
            // Set retry policy
            val timeoutMs = 100000 // Timeout in milliseconds
            val maxRetries = 0 // Max retry attempts
            val backoffMultiplier = 1.0f // Backoff multiplier
            retryPolicy = DefaultRetryPolicy(timeoutMs, maxRetries, backoffMultiplier)
        }
        // Add the request to the RequestQueue.
        requestQueue.add(jsonArrayRequest)
    }

    fun extractMessageFromErrorResponse(response: String): String? {
        try {
            // Parse the JSON string
            val jsonObject = JSONObject(response)
            // Retrieve the value associated with the "message" key
            return jsonObject.getString("message")
        } catch (e: Exception) {

        }
        return null
    }


    private fun enableProceedButton() {
        binding.proceedButton.isEnabled = true
        binding.proceedButtonRelativeLayout.setBackgroundColor(Color.parseColor(sharedPreferences.getString("primaryButtonColor","#000000")))
        binding.proceedButton.setBackgroundResource(R.drawable.button_bg)
        binding.textView6.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
            )
        )
    }

    private fun disableProceedButton() {
        binding.textView6.visibility = View.VISIBLE
        binding.proceedButton.isEnabled = false
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
        binding.proceedButton.setBackgroundResource(R.drawable.disable_button)
        binding.textView6.setTextColor(Color.parseColor("#ADACB0"))
    }

    private fun postRequestForUPICollect(context: Context, userVPA: String) {
        val requestQueue = Volley.newRequestQueue(context)
        val requestBody = JSONObject().apply {
            // Billing Address
            val billingAddressObject = JSONObject().apply {
                put("address1", sharedPreferences.getString("address1", "null"))
                put("address2", sharedPreferences.getString("address2", "null"))
                put("address3", sharedPreferences.getString("address3", "null"))
                put("city", sharedPreferences.getString("city", "null"))
                put("countryCode", sharedPreferences.getString("countryCode", "null"))
                put("countryName", sharedPreferences.getString("countryName", "null"))
                put("postalCode", sharedPreferences.getString("postalCode", "null"))
                put("state", sharedPreferences.getString("state", "null"))
            }
            put("billingAddress", billingAddressObject)

            // Create the browserData JSON object
            val browserData = JSONObject().apply {

                val webView = WebView(requireContext())

                // Get the default User-Agent string
                val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())

                // Get the screen height and width
                val displayMetrics = resources.displayMetrics
                put("screenHeight", displayMetrics.heightPixels.toString())
                put("screenWidth", displayMetrics.widthPixels.toString())
                put("acceptHeader", "application/json")
                put("userAgentHeader", userAgentHeader)
                put("browserLanguage", Locale.getDefault().toString())
                put("ipAddress", sharedPreferences.getString("ipAddress", "null"))
                put("colorDepth", 24) // Example value
                put("javaEnabled", true) // Example value
                put("timeZoneOffSet", 330) // Example value

                put("packageId",requireActivity().packageName)


            }
            put("browserData", browserData)

            // Instrument Details
            val instrumentDetailsObject = JSONObject().apply {
                put("type", "upi/collect")

                val upiObject = JSONObject().apply {
                    put("shopperVpa", userVPA)
                }
                put("upi", upiObject)
            }
            put("instrumentDetails", instrumentDetailsObject)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->
                transactionId = response.getString("transactionId").toString()
                updateTransactionIDInSharedPreferences(transactionId!!)
            },
            Response.ErrorListener { _ ->
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = token.toString()
                return headers
            }
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

    private fun updateTransactionIDInSharedPreferences(transactionIdArg: String) {
        editor.putString("transactionId", transactionIdArg)
        editor.apply()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialog -> //Get the BottomSheetBehavior
            val d = dialog as BottomSheetDialog
            val bottomSheet =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            }

            val window = d.window
            window?.apply {
                // Apply dim effect
                setDimAmount(0.5f) // 50% dimming
                setBackgroundDrawable(ColorDrawable(Color.argb(128, 0, 0, 0))) // Semi-transparent black background
            }


            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.isHideable = false
            bottomSheetBehavior?.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    // Handle state changes
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            // Fully expanded
                        }

                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            // Collapsed
                        }

                        BottomSheetBehavior.STATE_DRAGGING -> {
                            // The BottomSheet is being dragged
                        }

                        BottomSheetBehavior.STATE_SETTLING -> {
                            // The BottomSheet is settling
                        }

                        BottomSheetBehavior.STATE_HIDDEN -> {
                            //Hidden

                        }
                    }
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {

                }
            })
        }
        return dialog
    }
}