package com.example.tray

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.ViewModels.OverlayViewModel
import com.example.tray.databinding.FragmentAddUPIIDBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale


class AddUPIID : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentAddUPIIDBinding
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var overlayViewCurrentBottomSheet: View? = null
    private val Base_Session_API_URL = "https://test-apis.boxpay.tech/v0/checkout/sessions/"
    private var token : String ?= null
    private var proceedButtonIsEnabled = MutableLiveData<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            token = it.getString("token")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddUPIIDBinding.inflate(inflater, container, false)
        var checked = false
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        binding.progressBar.visibility = View.INVISIBLE
        binding.imageView3.setOnClickListener() {
            if (!checked) {
                binding.imageView3.setImageResource(R.drawable.checkbox)
                checked = true
            } else {
                binding.imageView3.setImageResource(0)
                checked = false
            }
        }
        binding.imageView2.setOnClickListener() {
            dismiss()
        }
        proceedButtonIsEnabled.observe(this, Observer { newValue ->
            if (newValue) {
                enableProceedButton()
            } else {
                disableProceedButton()
            }
        })
        proceedButtonIsEnabled.value = false

        binding.editTextText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("beforeTextChanged", s.toString())
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textNow = s.toString()
                Log.d("onTextChanged", s.toString())
                if (textNow.isNotBlank()) {
                    proceedButtonIsEnabled.value = true
                    binding.ll1InvalidUPI.visibility = View.GONE
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val textNow = s.toString()
                Log.d("afterTextChanged", s.toString())
                if (textNow.isBlank()) {
                    binding.proceedButtonRelativeLayout.isEnabled = false
                    binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
                    binding.ll1InvalidUPI.visibility = View.GONE
                }
            }

        })
        binding.ll1InvalidUPI.visibility = View.GONE

        binding.proceedButton.setOnClickListener() {
            val userVPA = binding.editTextText.text.toString()

            postRequest(requireContext(), userVPA)
            showLoadingInButton()
        }



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialog -> //Get the BottomSheetBehavior
            val d = dialog as BottomSheetDialog
            val bottomSheet =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
//                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            }
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        binding.editTextText.requestFocus()
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        (parentFragment as? MainBottomSheet)?.removeOverlayFromCurrentBottomSheet()
        super.onDismiss(dialog)
    }

    private fun showOverlayInCurrentBottomSheet() {
        // Create a semi-transparent overlay view
        overlayViewCurrentBottomSheet = View(requireContext())
        overlayViewCurrentBottomSheet?.setBackgroundColor(Color.parseColor("#80000000")) // Adjust color and transparency as needed

        // Add overlay view directly to the root view of the BottomSheet
        binding.root.addView(
            overlayViewCurrentBottomSheet,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    public fun removeOverlayFromCurrentBottomSheet() {
        overlayViewCurrentBottomSheet?.let {
            // Remove the overlay view directly from the root view
            binding.root.removeView(it)
        }
    }

    fun postRequest(context: Context,userVPA : String){
        Log.d("postRequestCalled",System.currentTimeMillis().toString())
        val requestQueue = Volley.newRequestQueue(context)


        // Constructing the request body
        val requestBody = JSONObject().apply {
            // Billing Address
            val billingAddressObject = JSONObject().apply {
                put("address1", "delivery address for the delivery")
                put("address2", "delivery")
                put("address3", JSONObject.NULL)
                put("city", "Saharanpur")
                put("countryCode", "IN")
                put("countryName", "India")
                put("postalCode", "247554")
                put("state", "Uttar Pradesh")
            }
            put("billingAddress", billingAddressObject)

            // Browser Data
            val browserDataObject = JSONObject().apply {
                val browserLanguage = Locale.getDefault().toLanguageTag()
                put("browserLanguage", browserLanguage)

                // Retrieve User-Agent header
                val webView = WebView(context)
                val userAgentHeader = webView.settings.userAgentString
                put("userAgentHeader", userAgentHeader)
            }
            put("browserData", browserDataObject)

            // Instrument Details
            val instrumentDetailsObject = JSONObject().apply {
                put("type", "upi/collect")

                val upiObject = JSONObject().apply {
                    put("shopperVpa", userVPA) // Replace with the actual shopper VPA value
                }
                put("upi", upiObject)
            }
            put("instrumentDetails", instrumentDetailsObject)
            // Shopper
            val shopperObject = JSONObject().apply {
                val deliveryAddressObject = JSONObject().apply {
                    put("address1", "delivery address for the delivery")
                    put("address2", "delivery")
                    put("address3", JSONObject.NULL)
                    put("city", "Saharanpur")
                    put("countryCode", "IN")
                    put("countryName", "India")
                    put("postalCode", "247554")
                    put("state", "Uttar Pradesh")
                }
                put("deliveryAddress", deliveryAddressObject)
                put("email", "test123@gmail.com")
                put("firstName", "test")
                put("gender", JSONObject.NULL)
                put("lastName", "last")
                put("phoneNumber", "919656262256")
                put("uniqueReference", "x123y")
            }
            put("shopper", shopperObject)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->
                // Handle response
                // Log.d("Response of Successful Post API call", response.toString())
                openUPITimerBottomSheet()
                hideLoadingInButton()

            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("Error", "Detailed error response: $errorResponse")
                    binding.ll1InvalidUPI.visibility = View.VISIBLE
                    binding.textView4.text = extractMessageFromErrorResponse(errorResponse)
                    hideLoadingInButton()
                }


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
    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body", jsonStr)
    }
    fun hideLoadingInButton(){
        binding.progressBar.visibility = View.INVISIBLE
        binding.textView6.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
            )
        )
        binding.textView6.visibility = View.VISIBLE
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
        binding.proceedButton.setBackgroundResource(R.drawable.disable_button)
        binding.textView6.setTextColor(Color.parseColor("#ADACB0"))
    }
    fun showLoadingInButton(){
        binding.textView6.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.VISIBLE
        val rotateAnimation = ObjectAnimator.ofFloat(binding.progressBar, "rotation", 0f, 360f)
        rotateAnimation.duration = 3000 // Set the duration of the rotation in milliseconds
        rotateAnimation.repeatCount = ObjectAnimator.INFINITE // Set to repeat indefinitely
        binding.proceedButton.isEnabled = false

        rotateAnimation.start()
    }
    private fun enableProceedButton(){
        binding.proceedButton.isEnabled = true
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.proceedButton.setBackgroundResource(R.drawable.button_bg)
        binding.textView6.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    }
    private fun disableProceedButton(){
        binding.textView6.visibility = View.VISIBLE
        binding.proceedButton.isEnabled = false
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
        binding.proceedButton.setBackgroundResource(R.drawable.disable_button)
        binding.textView6.setTextColor(Color.parseColor("#ADACB0"))
    }
    private fun openUPITimerBottomSheet(){
        val bottomSheetFragment = UPITimerBottomSheet.newInstance(token)
        bottomSheetFragment.show(parentFragmentManager, "UPITimerBottomSheet")
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


    companion object {
        fun newInstance(data: String?): AddUPIID {
            val fragment = AddUPIID()
            val args = Bundle()
            args.putString("token", data)
            fragment.arguments = args
            return fragment
        }
    }


}