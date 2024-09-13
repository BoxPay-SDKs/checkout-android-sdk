package com.boxpay.checkout.sdk

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.databinding.FragmentAddUPIIDBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random


internal class AddUPIID : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentAddUPIIDBinding
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var overlayViewCurrentBottomSheet: View? = null
    private lateinit var Base_Session_API_URL: String
    private var token: String? = null
    private var proceedButtonIsEnabled = MutableLiveData<Boolean>()
    private var successScreenFullReferencePath: String? = null
    private var userVPA: String? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var transactionId: String? = null
    private var shippingEnabled: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddUPIIDBinding.inflate(inflater, container, false)
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        val baseUrl = sharedPreferences.getString("baseUrl", "null")
        Base_Session_API_URL = "https://${baseUrl}/v0/checkout/sessions/"


        val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())

        if (userAgentHeader.contains("Mobile", ignoreCase = true)) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }


        var checked = false
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        binding.progressBar.visibility = View.INVISIBLE
        binding.imageView3.setOnClickListener() {
            if (!binding.progressBar.isVisible) {
                if (!checked) {
                    binding.imageView3.setImageResource(R.drawable.checkbox)
                    checked = true
                } else {
                    binding.imageView3.setImageResource(0)
                    checked = false
                }
            }
        }






        fetchTransactionDetailsFromSharedPreferences()


        //---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        //testing purpose

        //---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------


        binding.backButton.setOnClickListener() {
            if (!binding.progressBar.isVisible) {
                dismissAndMakeButtonsOfMainBottomSheetEnabled()
            }
        }
        binding.proceedButton.isEnabled = false

        binding.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!binding.progressBar.isVisible) {
                    callUIAnalytics(
                        requireContext(),
                        "PAYMENT_INSTRUMENT_PROVIDED",
                        "UpiCollect",
                        "Upi"
                    )
                    val textNow = s.toString()
                    if (textNow.isNotBlank() && textNow.matches(Regex("[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{3,64}"))) {
                        enableProceedButton()
                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    } else {
                        disableProceedButton()
                        if (textNow.contains('@') && (textNow.split('@').getOrNull(1)?.length
                                ?: 0) >= 2
                        ) {
                            binding.ll1InvalidUPI.visibility = View.VISIBLE // Show specific error
                        } else {
                            binding.ll1InvalidUPI.visibility =
                                View.INVISIBLE // Hide error if not matching condition
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val textNow = s.toString()
                if (textNow.isBlank()) {
                    binding.proceedButtonRelativeLayout.isEnabled = false
                    binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
                    binding.ll1InvalidUPI.visibility = View.INVISIBLE
                }
            }
        })
        binding.ll1InvalidUPI.visibility = View.INVISIBLE

        binding.proceedButton.setOnClickListener() {
            userVPA = binding.editText.text.toString()
            closeKeyboard(this)


            callUIAnalytics(requireContext(), "PAYMENT_INITIATED", "UpiCollect", "Upi")

            if (checkString(userVPA!!)) {
                binding.ll1InvalidUPI.visibility = View.INVISIBLE
                validateAPICall(requireContext(), userVPA!!)
                showLoadingInButton()
            } else {
                binding.ll1InvalidUPI.visibility = View.VISIBLE
            }
        }



        return binding.root
    }

    fun checkString(input: String): Boolean {
        val regex = Regex(".+@.+")
        return regex.matches(input)
    }

    private fun validateAPICall(context: Context, userVPA: String) {
        val requestQueue = Volley.newRequestQueue(context)


        // Constructing the request body
        val requestBody = JSONObject().apply {
            put("vpa", userVPA)
            put("legalEntity", sharedPreferences.getString("legalEntity", "null")) // Example value
            put("merchantId", sharedPreferences.getString("merchantId", "null"))
            put("countryCode", sharedPreferences.getString("countryCode", "null"))
        }

        val baseUrl = sharedPreferences.getString("baseUrl", "null")
        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, "https://" + baseUrl + "/v0/platform/vpa-validation", requestBody,
            Response.Listener { response ->
                try {
                    val statusUserVPA = response.getBoolean("vpaValid")

                    if (statusUserVPA) {
                        binding.ll1InvalidUPI.visibility = View.INVISIBLE
                        postRequest(requireContext(), userVPA)
                    } else {
                        binding.ll1InvalidUPI.visibility = View.VISIBLE
                        hideLoadingInButton()
                    }
                } catch (e: Exception) {
                    postRequest(requireContext(), userVPA)
                }
            },
            Response.ErrorListener { error ->

                binding.ll1InvalidUPI.visibility = View.INVISIBLE
                postRequest(requireContext(), userVPA)

            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
//                headers["X-Client-Connector-Name"] =  "Android SDK"
//                headers["X-Client-Connector-Version"] =  BuildConfig.SDK_VERSION
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
        editor.putString("operationId", transactionIdArg)
        editor.apply()
    }


    private fun fetchTransactionDetailsFromSharedPreferences() {
        token = sharedPreferences.getString("token", "empty")
        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun dismissAndMakeButtonsOfMainBottomSheetEnabled() {
        val mainBottomSheetFragment =
            parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
        mainBottomSheetFragment?.enabledButtonsForAllPaymentMethods()
        dismiss()
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
                setBackgroundDrawable(
                    ColorDrawable(
                        Color.argb(
                            128,
                            0,
                            0,
                            0
                        )
                    )
                ) // Semi-transparent black background
            }

            val screenHeight = resources.displayMetrics.heightPixels
            val percentageOfScreenHeight = 0.7 // 90%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

            bottomSheetBehavior?.maxHeight = desiredHeight
            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.isHideable = false


            dialog.setCancelable(false)

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
//                            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                        }

                        BottomSheetBehavior.STATE_SETTLING -> {
                            // The BottomSheet is settling
//                            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                        }

                        BottomSheetBehavior.STATE_HIDDEN -> {
                            //Hidden
                            dismissAndMakeButtonsOfMainBottomSheetEnabled()

                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {

                }
            })
        }
        return dialog
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // Handle the back button press here
        // Dismiss the dialog when the back button is pressed
        dismissAndMakeButtonsOfMainBottomSheetEnabled()
    }

    override fun onStart() {
        super.onStart()
//        binding.editTextText.requestFocus()
    }

//    private fun fetchOTPAutomatically(){
//        val smsReceiver = otpFetcher(requireContext())
//        smsReceiver.onReceive()
//    }


    override fun onDismiss(dialog: DialogInterface) {
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

    private fun postRequest(context: Context, userVPA: String) {
        val requestQueue = Volley.newRequestQueue(context)


        // Constructing the request body
        val requestBody = JSONObject().apply {


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
                put("javaEnabled", true) // Example value
                put("packageId", requireActivity().packageName)
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


            val shopperObject = JSONObject().apply {
                put("email", sharedPreferences.getString("email", null))
                put("firstName", sharedPreferences.getString("firstName", null))
                if (sharedPreferences.getString("gender", null) == null)
                    put("gender", JSONObject.NULL)
                else
                    put("gender", sharedPreferences.getString("gender", null))
                put("lastName", sharedPreferences.getString("lastName", null))
                put("phoneNumber", sharedPreferences.getString("phoneNumber", null))
                put("uniqueReference", sharedPreferences.getString("uniqueReference", null))

                if (shippingEnabled) {
                    val deliveryAddressObject = JSONObject().apply {

                        put("address1", sharedPreferences.getString("address1", null))
                        put("address2", sharedPreferences.getString("address2", null))
                        put("city", sharedPreferences.getString("city", null))
                        put("countryCode", sharedPreferences.getString("countryCode", null))
                        put("postalCode", sharedPreferences.getString("postalCode", null))
                        put("state", sharedPreferences.getString("state", null))
                        put("city", sharedPreferences.getString("city", null))
                        put("email", sharedPreferences.getString("email", null))
                        put("phoneNumber", sharedPreferences.getString("phoneNumber", null))
                        put("countryName", sharedPreferences.getString("countryName", null))

                    }
                    put("deliveryAddress", deliveryAddressObject)
                }
            }

            put("shopper", shopperObject)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->

                val status = response.getJSONObject("status").getString("status")
                val reason = response.getJSONObject("status").getString("reason")
                val reasonCode = response.getJSONObject("status").getString("reasonCode")
                transactionId = response.getString("transactionId").toString()
                updateTransactionIDInSharedPreferences(transactionId!!)

                if (status.contains("Rejected", ignoreCase = true)) {
                    var cleanedMessage = reason.substringAfter(":")
                    if (cleanedMessage.contains("virtual address", true)) {
                        cleanedMessage = "Invalid UPI Id"
                    } else if (!reasonCode.startsWith("uf", true)) {
                        cleanedMessage =
                            "Please retry using other payment method or try again in sometime"
                    }
                    PaymentFailureScreen(errorMessage = cleanedMessage).show(
                        parentFragmentManager,
                        "FailureScreen"
                    )
                } else {

                    if (status.contains("RequiresAction", ignoreCase = true)) {
                        editor.putString("status", "RequiresAction")
                        editor.apply()
                        openUPITimerBottomSheet()
                    } else if (status.contains("Approved", ignoreCase = true)) {
                        editor.putString("status", "Success")
                        editor.apply()

                        val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                        bottomSheet.show(
                            parentFragmentManager,
                            "PaymentStatusBottomSheetWithDetails"
                        )
                        dismissAndMakeButtonsOfMainBottomSheetEnabled()
                    }
                }
                hideLoadingInButton()

            },
            Response.ErrorListener { error ->
                // Handle error
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    val errorMessage = extractMessageFromErrorResponse(errorResponse)

                    if (errorMessage?.contains("expired",true) == true) {
                        SessionExpireScreen().show(parentFragmentManager, "SessionScreen")
                    } else {
                        PaymentFailureScreen(
                            errorMessage = "Please retry using other payment method or try again in sometime"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                }
                hideLoadingInButton()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
                if (sharedPreferences.getString(
                        "shopperToken",
                        ""
                    ) != null && sharedPreferences.getString("shopperToken", "") != ""
                ) {
                    headers["Authorization"] =
                        "Session ${sharedPreferences.getString("shopperToken", "")}"
                }
                headers["X-Client-Connector-Name"] = "Android SDK"
                headers["X-Client-Connector-Version"] = BuildConfig.SDK_VERSION
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

    fun dismissCurrentBottomSheet() {
        dismiss()
    }

    fun hideLoadingInButton() {
        binding.progressBar.visibility = View.INVISIBLE
        binding.textView6.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
            )
        )
        binding.textView6.visibility = View.VISIBLE
        binding.proceedButtonRelativeLayout.setBackgroundColor(
            Color.parseColor(
                sharedPreferences.getString(
                    "primaryButtonColor",
                    "#000000"
                )
            )
        )
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.proceedButton.isEnabled = true
//        binding.textView6.setTextColor(Color.parseColor("#ADACB0"))
    }

    fun showLoadingInButton() {
        binding.textView6.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.VISIBLE
        val rotateAnimation =
            ObjectAnimator.ofFloat(binding.progressBar, "rotation", 0f, 360f)
        rotateAnimation.duration = 3000 // Set the duration of the rotation in milliseconds
        rotateAnimation.repeatCount = ObjectAnimator.INFINITE // Set to repeat indefinitely
        binding.proceedButton.isEnabled = false

        rotateAnimation.start()
    }

    private fun enableProceedButton() {
        binding.proceedButtonRelativeLayout.isEnabled = true
        binding.proceedButton.isEnabled = true
        binding.proceedButtonRelativeLayout.setBackgroundColor(
            Color.parseColor(
                sharedPreferences.getString("primaryButtonColor", "#000000")
            )
        )
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.ll1InvalidUPI.visibility = View.INVISIBLE
        binding.textView6.setTextColor(
            Color.parseColor(
                sharedPreferences.getString(
                    "buttonTextColor",
                    "#000000"
                )
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

    private fun openUPITimerBottomSheet() {
        val bottomSheetFragment = UPITimerBottomSheet.newInstance(userVPA)
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

        }
        return null
    }

    private fun closeKeyboard(fragment: Fragment) {
        val activity = fragment.activity
        val view = fragment.view
        if (activity != null && view != null) {
            val imm = ContextCompat.getSystemService(activity, InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun callUIAnalytics(
        context: Context,
        event: String,
        paymentSubType: String,
        paymentType: String
    ) {
        val baseUrl = sharedPreferences.getString("baseUrl", "null")
        val requestQueue = Volley.newRequestQueue(context)
        val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
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
            Method.POST, "https://${baseUrl}/v0/ui-analytics", requestBody,
            Response.Listener { /*no response handling */ },
            Response.ErrorListener { /*no response handling */ }) {}.apply {
            // Set retry policy
            val timeoutMs = 100000 // Timeout in milliseconds
            val maxRetries = 0 // Max retry attempts
            val backoffMultiplier = 1.0f // Backoff multiplier
            retryPolicy = DefaultRetryPolicy(timeoutMs, maxRetries, backoffMultiplier)
        }

        // Add the request to the RequestQueue.
        requestQueue.add(jsonObjectRequest)
    }


    companion object {
        fun newInstance(
            shippingEnabled: Boolean
        ): AddUPIID {
            val fragment = AddUPIID()
            fragment.shippingEnabled = shippingEnabled
            return fragment
        }
    }

    fun generateRandomAlphanumericString(length: Int): String {
        val charPool: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}