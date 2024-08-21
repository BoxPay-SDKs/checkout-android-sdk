package com.boxpay.checkout.sdk

import FailureScreenSharedViewModel
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.adapters.BnplAdapters
import com.boxpay.checkout.sdk.databinding.FragmentBnplBottomSheetBinding
import com.boxpay.checkout.sdk.dataclasses.BnplDataClass
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random

internal class BNPLBottomSheet : BottomSheetDialogFragment() {

    private var shippingEnabled: Boolean = false
    private lateinit var binding: FragmentBnplBottomSheetBinding
    private var walletDetailsFiltered: ArrayList<BnplDataClass> = ArrayList()
    private lateinit var allWalletAdapter: BnplAdapters
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var checkedPosition: Int? = null
    private lateinit var Base_Session_API_URL: String
    private var transactionId: String? = null
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private var bnplDetailOriginal: ArrayList<BnplDataClass> = ArrayList()
    private var token: String? = null
    private var successScreenFullReferencePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBnplBottomSheetBinding.inflate(layoutInflater, container, false)
        val failureScreenSharedViewModelCallback =
            FailureScreenSharedViewModel(::failurePaymentFunction)
        FailureScreenCallBackSingletonClass.getInstance().callBackFunctions =
            failureScreenSharedViewModelCallback


        val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
        if (userAgentHeader.contains("Mobile", ignoreCase = true)) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        val screenHeight = requireContext().resources.displayMetrics.heightPixels
        val percentageOfScreenHeight = 0.45 // 70%
        val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()


        val layoutParams = binding.nestedScrollView.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.height = desiredHeight
        binding.nestedScrollView.layoutParams = layoutParams


        val layoutParamsLoading =
            binding.loadingRelativeLayout.layoutParams as ConstraintLayout.LayoutParams
        layoutParamsLoading.height = desiredHeight
        binding.loadingRelativeLayout.layoutParams = layoutParamsLoading

        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()


        val baseUrl = sharedPreferences.getString("baseUrl", "null")

        Base_Session_API_URL = "https://${baseUrl}/v0/checkout/sessions/"

        fetchTransactionDetailsFromSharedPreferences()
        bnplDetailOriginal = arrayListOf()

        allWalletAdapter = BnplAdapters(
            walletDetailsFiltered,
            binding.walletsRecyclerView,
            requireContext(),
            token.toString()
        )
        binding.walletsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.walletsRecyclerView.adapter = allWalletAdapter

        binding.boxPayLogoLottieAnimation.playAnimation()
        disableProceedButton()


        if (!shippingEnabled)
            fetchBnplDetails()
        else
            callPaymentMethodRules(requireContext())

        binding.backButton.setOnClickListener() {
            dismissAndMakeButtonsOfMainBottomSheetEnabled()
        }
        binding.proceedButton.isEnabled = false

        binding.checkingTextView.setOnClickListener() {
            var enabled = false
            if (!enabled)
                enableProceedButton()
            else
                disableProceedButton()

            enabled = !enabled
        }

        allWalletAdapter.checkPositionLiveData.observe(this, Observer { checkPositionObserved ->
            if (checkPositionObserved == null) {
                disableProceedButton()
            } else {
                enableProceedButton()
                checkedPosition = checkPositionObserved
            }
        })

        binding.proceedButton.setOnClickListener() {
            showLoadingInButton()
            var walletInstrumentTypeValue = ""
            walletInstrumentTypeValue =
                walletDetailsFiltered[checkedPosition!!].instrumentTypeValue
            callUIAnalytics(requireContext(),"PAYMENT_INITIATED",bnplDetailOriginal[checkedPosition!!].bnplBrand,"BNPL")


            postRequest(requireContext(), walletInstrumentTypeValue)
        }


        return binding.root
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
            val percentageOfScreenHeight = 0.9 // 90%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

            bottomSheetBehavior?.maxHeight = desiredHeight
            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.isHideable = false
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

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
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {

                }
            })
        }
        return dialog
    }

    companion object {
        fun newInstance(
            shippingEnabled: Boolean
        ): BNPLBottomSheet {
            val fragment = BNPLBottomSheet()
            fragment.shippingEnabled = shippingEnabled
            return fragment
        }
    }

    private fun updateTransactionIDInSharedPreferences(transactionIdArg: String) {
        editor.putString("transactionId", transactionIdArg)
        editor.putString("operationId", transactionIdArg)
        editor.apply()
    }

    fun failurePaymentFunction() {

        // Start a coroutine with a delay of 5 seconds
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000) // Delay for 1 seconds

            // Code inside this block will execute after the delay
            // Code inside this block will execute after the delay
            val bottomSheet = PaymentFailureScreen()
            bottomSheet.show(parentFragmentManager, "PaymentFailureScreen")
        }

    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences =
            requireContext().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token", "empty")
        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
    }

    private fun callUIAnalytics(
        context: Context,
        event: String,
        paymentSubType: String,
        paymentType: String
    ) {
        val baseUrl = sharedPreferences.getString("baseUrl", "null")

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


    private fun fetchBnplDetails() {
        val url = "${Base_Session_API_URL}${token}"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObjectAll = JsonObjectRequest(Request.Method.GET, url, null, { response ->

            try {

                // Get the payment methods array
                val paymentMethodsArray =
                    response.getJSONObject("configs").getJSONArray("paymentMethods")

                // Filter payment methods based on type equal to "Wallet"
                for (i in 0 until paymentMethodsArray.length()) {
                    val paymentMethod = paymentMethodsArray.getJSONObject(i)
                    if (paymentMethod.getString("type") == "BuyNowPayLater") {
                        val walletName = paymentMethod.getString("title")
                        var walletImage = paymentMethod.getString("logoUrl")
                        if (walletImage.startsWith("/assets")) {
                            walletImage =
                                "https://checkout.boxpay.in" + paymentMethod.getString("logoUrl")
                        }
                        val walletBrand = paymentMethod.getString("brand")
                        val walletInstrumentTypeValue =
                            paymentMethod.getString("instrumentTypeValue")
                        bnplDetailOriginal.add(
                            BnplDataClass(
                                walletName,
                                walletImage,
                                walletBrand,
                                walletInstrumentTypeValue
                            )
                        )
                    }
                }
                bnplDetailOriginal = ArrayList(bnplDetailOriginal.sortedBy { it.bnplBrand })

                // Print the filtered wallet payment methods
                showAllWallets()
                removeLoadingScreenState()

            } catch (e: Exception) {

            }

        }, { error ->

            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                hideLoadingInButton()
            }
        })
        queue.add(jsonObjectAll)
    }

    private fun callPaymentMethodRules(context: Context) {

        val requestQueue = Volley.newRequestQueue(context)

        val countryName = sharedPreferences.getString("countryCode", null)

        val jsonArrayRequest = object : JsonArrayRequest(
            Method.GET,
            Base_Session_API_URL + token + "/payment-methods?customerCountryCode=$countryName",
            null,
            Response.Listener { response ->
                for (i in 0 until response.length()) {
                    val paymentMethod = response.getJSONObject(i)
                    if (paymentMethod.getString("type") == "Wallet") {
                        val walletName = paymentMethod.getString("title")
                        var walletImage = paymentMethod.getString("logoUrl")
                        if (walletImage.startsWith("/assets")) {
                            walletImage =
                                "https://checkout.boxpay.in" + paymentMethod.getString("logoUrl")
                        }
                        val walletBrand = paymentMethod.getString("brand")
                        val walletInstrumentTypeValue =
                            paymentMethod.getString("instrumentTypeValue")
                        bnplDetailOriginal.add(
                            BnplDataClass(
                                walletName,
                                walletImage,
                                walletBrand,
                                walletInstrumentTypeValue
                            )
                        )
                    }
                }

                // Print the filtered wallet payment methods
                showAllWallets()
                removeLoadingScreenState()
            },
            Response.ErrorListener { _ ->

            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                return headers
            }
        }.apply {
            val timeoutMs = 100000
            val maxRetries = 0
            val backoffMultiplier = 1.0f
            retryPolicy = DefaultRetryPolicy(timeoutMs, maxRetries, backoffMultiplier)
        }

        requestQueue.add(jsonArrayRequest)
    }

    private fun postRequest(context: Context, instrumentTypeValue: String) {
        val requestQueue = Volley.newRequestQueue(context)

        val requestBody = JSONObject().apply {

            // Create the browserData JSON object
            val browserData = JSONObject().apply {

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
                put("packageId", requireActivity().packageName)
            }
            put("browserData", browserData)

            // Instrument Details
            val instrumentDetailsObject = JSONObject().apply {
                put("type", instrumentTypeValue)
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
                transactionId = response.getString("transactionId").toString()
                updateTransactionIDInSharedPreferences(transactionId!!)

                hideLoadingInButton()

                try {


                    val actionsArray = response.getJSONArray("actions")
                    val status = response.getJSONObject("status").getString("status")
                    var url = ""
                    // Loop through the actions array to find the URL
                    for (i in 0 until actionsArray.length()) {
                        val actionObject = actionsArray.getJSONObject(i)
                        url = actionObject.getString("url")
                        // Do something with the URL

                    }



                    if (status.equals("Approved")) {
                        val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                        bottomSheet.show(
                            parentFragmentManager,
                            "PaymentSuccessfulWithDetailsBottomSheet"
                        )
                        dismissAndMakeButtonsOfMainBottomSheetEnabled()
                    } else {

                        if (status.contains("RequiresAction", ignoreCase = true)) {
                            editor.putString("status", "RequiresAction")
                        }

//                        val intent = Intent(requireContext(), OTPScreenWebView::class.java)
//                        FailureScreenFunctionObject.failureScreenFunction = ::failurePaymentFunction
                        val intent = Intent(context, OTPScreenWebView::class.java)
                        intent.putExtra("url", url)

                        // Check if the context is not null before starting the activity
//                        context?.let { context ->
//                            intent.putExtra("url", url)
//                            val webViewActivity = OTPScreenWebView()
//                            webViewActivity.setWebViewCloseListener(requireContext()) // Pass the current BottomSheetDialogFragment as the listener
//                            context.startActivity(intent)
//                        } // Start the webViewActivity

                        startActivity(intent)
                        editor.apply()
////                        startActivity(intent)


//                        val bottomSheet = ForceTestPaymentBottomSheet()
//                        bottomSheet.show(parentFragmentManager,"ForceTestPaymentOpenByWallet")
                    }

                } catch (e: JSONException) {

                }

            },
            Response.ErrorListener { error ->
                // Handle error
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    hideLoadingInButton()
                    PaymentFailureScreen(
                        errorMessage = "Not configured for this merchant id"
                    ).show(parentFragmentManager, "FailureScreen")
                }
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
                headers["X-Client-Connector-Name"] =  "Android SDK"
                headers["X-Client-Connector-Version"] =  BuildConfig.SDK_VERSION
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

    private fun enableProceedButton() {
        binding.proceedButton.isEnabled = true
        binding.proceedButtonRelativeLayout.setBackgroundColor(
            Color.parseColor(
                sharedPreferences.getString(
                    "primaryButtonColor",
                    "#000000"
                )
            )
        )
        binding.proceedButton.setBackgroundResource(R.drawable.button_bg)
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

    fun showAllWallets() {
        walletDetailsFiltered.clear()
        for (bank in bnplDetailOriginal) {
            walletDetailsFiltered.add(bank)
        }
        allWalletAdapter.deselectSelectedItem()
        allWalletAdapter.notifyDataSetChanged()
    }

    private fun dismissAndMakeButtonsOfMainBottomSheetEnabled() {
        val mainBottomSheetFragment =
            parentFragmentManager.findFragmentByTag("MainBottomSheet") as? MainBottomSheet
        mainBottomSheetFragment?.enabledButtonsForAllPaymentMethods()
        dismiss()
    }

    fun generateRandomAlphanumericString(length: Int): String {
        val charPool : List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun removeLoadingScreenState() {
        binding.loadingRelativeLayout.visibility = View.GONE
        binding.walletsRecyclerView.visibility = View.VISIBLE
    }

}