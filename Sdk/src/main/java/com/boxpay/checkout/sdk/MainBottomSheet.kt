package com.boxpay.checkout.sdk

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.ViewModels.CallBackFunctions
import com.boxpay.checkout.sdk.ViewModels.CallbackForDismissMainSheet
import com.boxpay.checkout.sdk.ViewModels.OverlayViewModel
import com.boxpay.checkout.sdk.ViewModels.SingletonClassForLoadingState
import com.boxpay.checkout.sdk.ViewModels.SingletonForDismissMainSheet
import com.boxpay.checkout.sdk.adapters.OrderSummaryItemsAdapter
import com.boxpay.checkout.sdk.databinding.FragmentMainBottomSheetBinding
import com.boxpay.checkout.sdk.interfaces.UpdateMainBottomSheetInterface
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.random.Random


internal class MainBottomSheet : BottomSheetDialogFragment(), UpdateMainBottomSheetInterface {
    private var transactionId: String? = null
    private var isSuccessful = false
    private var qrCodeShown = false
    private var overlayViewMainBottomSheet: View? = null
    private lateinit var binding: FragmentMainBottomSheetBinding
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private val overlayViewModel: OverlayViewModel by activityViewModels()
    private var overlayViewCurrentBottomSheet: View? = null
    private var token: String? = null
    private var successScreenFullReferencePath: String? = null
    private var UPIAppsAndPackageMap: MutableMap<String, String> = mutableMapOf()
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private var job: Job? = null
    private var isTablet = false
    private var showName = false
    private var showEmail = false
    private var showShipping = false
    private var showPhone = false
    private var priceBreakUpVisible = false
    private var i = 1
    var countryCode: Pair<String, String>? = null
    private var transactionAmount: String? = null
    private var upiAvailable = false
    private var upiCollectMethod = false
    private var upiIntentMethod = false
    private var upiQRMethod = false
    private var cardsMethod = false
    private var walletMethods = false
    private var bnplMethod = false
    private var netBankingMethods = false
    private var overLayPresent = false
    private var items = mutableListOf<String>()
    private var itemQty = mutableListOf<String>()
    private var imagesUrls = mutableListOf<String>()
    private var prices = mutableListOf<String>()
    private lateinit var Base_Session_API_URL: String
    var queue: RequestQueue? = null
    private lateinit var countdownTimer: CountDownTimer
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    var isGpayReturned = false
    var isOthersReturned = false
    var isPhonePe = false
    var isPaytmReturned = false
    private var callBackFunctions: CallBackFunctions? = null
    private var shippingEnabled: Boolean = false
    private var dismissThroughAnotherBottomSheet: Boolean = false
    private lateinit var bottomSheet: DeliveryAddressBottomSheet
    private var firstLoad: Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        removeOverlayFromActivity()
        dismiss()
    }

    override fun onStart() {
        super.onStart()

        showLoadingState("onStart") // Show loading state before initiating tasks


        // Show loading state while executing time-consuming tasks
        if (firstLoad) {
            sharedPreferences =
                requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
            queue = Volley.newRequestQueue(requireContext())
            editor = sharedPreferences.edit()
            val coroutineScope = CoroutineScope(Dispatchers.Main)
            val coroutine = coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    editor.putString("status", "NoAction")
                    editor.putString("transactionId", "")
                    editor.putString("operationId", "")
                    editor.apply()
                    makeSessionDataCall()
                }

            }
            coroutine.invokeOnCompletion {
                val packageManager = requireContext().packageManager
                getAllInstalledApps(packageManager)
                populatePopularUPIApps()
            }
            firstLoad = false
        } else {
            removeLoadingState()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        overlayViewModel.setShowOverlay(false)
        val callback = SingletonClass.getInstance().getYourObject()
        if (!dismissThroughAnotherBottomSheet) {
            if (callback != null) {
                val statusFetched = sharedPreferences.getString("status", "")
                val transactionIdFetched = sharedPreferences.getString("transactionId", "")
                val operationIdFetched = sharedPreferences.getString("operationId", "")
                callback.onPaymentResult(
                    PaymentResultObject(
                        statusFetched.toString(),
                        transactionIdFetched.toString(),
                        operationIdFetched.toString()
                    )
                )
            }
        }
        super.onDismiss(dialog)
    }

    fun dismissTheSheetAfterSuccess() {
        isSuccessful = true
        dismiss()
    }

    private fun getAllInstalledApps(packageManager: PackageManager) {

        val apps = packageManager.getInstalledApplications(PackageManager.GET_GIDS)

        for (app in apps) {
            val appName = packageManager.getApplicationLabel(app).toString()


            // Check if the app supports UPI transactions
            val upiIntent = Intent(Intent.ACTION_VIEW)
            upiIntent.data = Uri.parse("upi://pay")
            upiIntent.setPackage(app.packageName)
            val upiApps = packageManager.queryIntentActivities(upiIntent, 0)

            if (appName == "PhonePe") {
                i++;
                UPIAppsAndPackageMap[appName] = app.packageName
            }

            // If the app can handle the UPI intent, it's a UPI app
            if (upiApps.isNotEmpty() || appName == "Paytm" || appName == "GPay" || appName == "PhonePe") {
                i++

                UPIAppsAndPackageMap[appName] = app.packageName
            }
        }
    }

    private fun fetchUPIIntentURL(context: Context, appName: String) {

        showLoadingState("fetchIntentURL")
        getUrlForUPIIntent(appName)
    }

    private fun showLoadingState(source: String) {

        binding.loadingRelativeLayout.visibility = View.VISIBLE
    }

    private fun removeLoadingState() {
        binding.loadingRelativeLayout.visibility = View.GONE
        binding.boxpayLogoLottie.cancelAnimation()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 121) {
            if (resultCode == Activity.RESULT_OK) {
                val responseUri: Uri? = data?.data
                if (responseUri != null) {
                    val responseString = responseUri.toString()
                    if (responseString.contains("success", ignoreCase = true)) {
                        // Payment was successful
                        editor.putString("status", "Success")
                        editor.apply()

                        if (isAdded && isResumed) {
                            val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                            bottomSheet.show(
                                parentFragmentManager,
                                "PaymentStatusBottomSheetWithDetails"
                            )
                            job?.cancel()
                        }
                    } else if (responseString.contains("fail", ignoreCase = true) || responseString.contains("decline", ignoreCase = true)) {
                        // Payment was declined or failed
                        editor.putString("status", "Failed")
                        editor.apply()
                        if (isAdded && isResumed) {
                            job?.cancel()
                            PaymentFailureScreen(
                                errorMessage = ""
                            ).show(parentFragmentManager, "FailureScreen")
                        }
                    } else {
                        // User returned without completing payment or other cases
                    }
                } else {
                    // User returned without completing payment or other cases
                }
            } else {
                // Payment was canceled by the user or some error occurred
                isGpayReturned = true
            }
        } else if (requestCode == 122) {
            if (resultCode == Activity.RESULT_OK) {
                val responseUri: Uri? = data?.data
                if (responseUri != null) {
                    val responseString = responseUri.toString()
                    if (responseString.contains("success", ignoreCase = true)) {
                        // Payment was successful
                        editor.putString("status", "Success")
                        editor.apply()

                        if (isAdded && isResumed) {
                            val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                            bottomSheet.show(
                                parentFragmentManager,
                                "PaymentStatusBottomSheetWithDetails"
                            )
                            job?.cancel()
                        }
                    } else if (responseString.contains("fail", ignoreCase = true) || responseString.contains("decline", ignoreCase = true)) {
                        // Payment was declined or failed
                        editor.putString("status", "Failed")
                        editor.apply()
                        if (isAdded && isResumed) {
                            job?.cancel()
                            PaymentFailureScreen(
                                errorMessage = ""
                            ).show(parentFragmentManager, "FailureScreen")
                        }
                    } else {
                        // User returned without completing payment or other cases
                    }
                } else {
                    // User returned without completing payment or other cases
                }
            } else {
                // Payment was canceled by the user or some error occurred
                isPaytmReturned = true
            }
        } else if (requestCode == 123){
            if (resultCode == Activity.RESULT_OK) {
                val responseUri: Uri? = data?.data
                if (responseUri != null) {
                    val responseString = responseUri.toString()
                    if (responseString.contains("success", ignoreCase = true)) {
                        // Payment was successful
                        editor.putString("status", "Success")
                        editor.apply()

                        if (isAdded && isResumed) {
                            val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                            bottomSheet.show(
                                parentFragmentManager,
                                "PaymentStatusBottomSheetWithDetails"
                            )
                            job?.cancel()
                        }
                    } else if (responseString.contains("fail", ignoreCase = true) || responseString.contains("decline", ignoreCase = true)) {
                        // Payment was declined or failed
                        editor.putString("status", "Failed")
                        editor.apply()
                        if (isAdded && isResumed) {
                            job?.cancel()
                            PaymentFailureScreen(
                                errorMessage = ""
                            ).show(parentFragmentManager, "FailureScreen")
                        }
                    } else {
                        // User returned without completing payment or other cases
                    }
                } else {
                    // User returned without completing payment or other cases
                }
            } else {
                // Payment was canceled by the user or some error occurred
               isPhonePe = true
            }
        } else {
            if (resultCode == Activity.RESULT_OK) {
                val responseUri: Uri? = data?.data
                if (responseUri != null) {
                    val responseString = responseUri.toString()
                    if (responseString.contains("success", ignoreCase = true)) {
                        // Payment was successful
                        editor.putString("status", "Success")
                        editor.apply()

                        if (isAdded && isResumed) {
                            val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                            bottomSheet.show(
                                parentFragmentManager,
                                "PaymentStatusBottomSheetWithDetails"
                            )
                            job?.cancel()
                        }
                    } else if (responseString.contains("fail", ignoreCase = true) || responseString.contains("decline", ignoreCase = true)) {
                        // Payment was declined or failed
                        editor.putString("status", "Failed")
                        editor.apply()
                        if (isAdded && isResumed) {
                            job?.cancel()
                            PaymentFailureScreen(
                                errorMessage = ""
                            ).show(parentFragmentManager, "FailureScreen")
                        }
                    } else {
                        // User returned without completing payment or other cases
                    }
                } else {
                    // User returned without completing payment or other cases
                }
            } else {
                // Payment was canceled by the user or some error occurred
                isOthersReturned = true
            }
        }
    }

    private fun launchUPIIntent(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)

        val uri = Uri.parse(url)
        intent.data = uri
        try {
            var resultCode : Int
            startFunctionCalls()
            if (url.startsWith("tez")) {
                resultCode = 121
            } else if (url.startsWith("paytm")) {
                resultCode = 122
            } else {
                resultCode = 123
            }

            startActivityForResult(intent, resultCode)

            removeLoadingState()
        } catch (_: ActivityNotFoundException) {
        }
    }

    private fun startFunctionCalls() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(4000)
                fetchStatusAndReason("${Base_Session_API_URL}${token}/status")
                // Delay for 4 seconds
            }
        }
    }


    fun urlToBase64(base64String: String): String {


        return try {
            // Decode Base64 string to byte array
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)

            // Convert byte array to string
            val decodedString = String(decodedBytes, StandardCharsets.UTF_8)

            // Decode URL
            URLDecoder.decode(decodedString, "UTF-8")
        } catch (e: Exception) {
            ""
        }
    }

    private fun fetchStatusAndReason(url: String) {
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.GET, url, null,
            Response.Listener { response ->
                try {
                    val status = response.getString("status")
                    val reason = response.getString("statusReason")
                    val reasonCode = response.getString("reasonCode")
                    transactionId = response.getString("transactionId").toString()
                    updateTransactionIDInSharedPreferences(transactionId!!)
                    if (status.equals("Pending",ignoreCase = true) && isGpayReturned) {
                        isGpayReturned = false
                        editor.putString("status", "Failed")
                        editor.apply()
                        PaymentFailureScreen(
                            errorMessage = "Payment failed with Gpay. Please retry payment with a different UPI app"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                    if (status.equals("Pending",ignoreCase = true) && isPhonePe) {
                        isPhonePe = false
                        editor.putString("status", "Failed")
                        editor.apply()
                        PaymentFailureScreen(
                            errorMessage = "Payment failed with PhonePe. Please retry payment with a different UPI app"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                    if (status.equals("Pending", ignoreCase = true) && isOthersReturned) {
                        isOthersReturned = false
                        editor.putString("status", "Failed")
                        editor.apply()
                        PaymentFailureScreen(
                            errorMessage = "Please retry using other payment method or try again in sometime"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                    if (status.equals("Pending",ignoreCase = true) && isPaytmReturned) {
                        isPaytmReturned = false
                        editor.putString("status", "Failed")
                        editor.apply()
                        PaymentFailureScreen(
                            errorMessage = "Payment failed with Paytm. Please retry payment with a different UPI app"
                        ).show(parentFragmentManager, "FailureScreen")
                    }

                    if (status.equals("Rejected", ignoreCase = true) || status.equals(
                            "failed",
                            true
                        )
                    ) {
                        editor.putString("status", "Failed")
                        editor.apply()
                        if (isAdded && isResumed) {
                            job?.cancel()
                            var cleanedMessage = reason.substringAfter(":")
                            if (!reasonCode.startsWith("uf", true)) {
                                cleanedMessage = "Please retry using other payment method or try again in sometime"
                            }
                            PaymentFailureScreen(
                                function = {
                                    if (qrCodeShown) {
                                        countdownTimer.cancel()
                                        showQRCode()
                                    }
                                },
                                errorMessage = cleanedMessage
                            ).show(parentFragmentManager, "FailureScreen")
                        }
                    } else {
                        if (status.equals("RequiresAction", ignoreCase = true)) {
                            editor.putString("status", "RequiresAction")
                            editor.apply()
                        } else if (status.equals(
                                "Approved",
                                ignoreCase = true
                            ) || status.equals("paid", true)
                        ) {
                            editor.putString("status", "Success")
                            editor.apply()

                            if (isAdded && isResumed) {
                                val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                                bottomSheet.show(
                                    parentFragmentManager,
                                    "PaymentStatusBottomSheetWithDetails"
                                )
                                job?.cancel()
                            }
                        }
                    }
                } catch (_: JSONException) {

                }
            },
            Response.ErrorListener {
                PaymentFailureScreen(
                    errorMessage = "Please retry using other payment method or try again in sometime"
                ).show(parentFragmentManager, "FailureScreen")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Trace-Id"] = generateRandomAlphanumericString(10)
                return headers
            }

        }
        // Add the request to the RequestQueue.
        queue?.add(jsonObjectRequest)
    }

    private fun getUrlForUPIIntent(appName: String) {

        val requestQueue = Volley.newRequestQueue(context)
        val requestBody = JSONObject().apply {
            val browserData = JSONObject().apply {
                val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
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
            val instrumentDetailsObject = JSONObject().apply {
                put("type", "upi/intent")

                val upiAppDetails = JSONObject().apply {
                    put("upiApp", appName)
                }
                put("upiAppDetails", upiAppDetails)
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

                try {

                    val actionsArray = response.getJSONArray("actions")
                    val urlForIntent = actionsArray.getJSONObject(0).getString("url")
                    val status = response.getJSONObject("status").getString("status")
                    val reason = response.getJSONObject("status").getString("reason")
                    val reasonCode = response.getJSONObject("status").getString("reasonCode")
                    transactionId = response.getString("transactionId").toString()
                    updateTransactionIDInSharedPreferences(transactionId!!)

                    if (status.contains("rejected", ignoreCase = true)) {
                        removeLoadingState()
                        var cleanedMessage = reason.substringAfter(":")
                        if (!reasonCode.startsWith("uf", true)) {
                            cleanedMessage = "Please retry using other payment method or try again in sometime"
                        }
                        PaymentFailureScreen(errorMessage = cleanedMessage).show(
                            parentFragmentManager,
                            "FailureScreenFromUPIIntent"
                        )
                    } else if (status.contains("RequiresAction", ignoreCase = true)) {
                        editor.putString("status", "RequiresAction")
                        editor.apply()
                    }
                    val urlInBase64 = urlToBase64(urlForIntent)
                    launchUPIIntent(urlInBase64)
                } catch (e: JSONException) {
                    removeLoadingState()
                    PaymentFailureScreen().show(parentFragmentManager, "FailureScreenFromUPIIntent")
                }
            },
            Response.ErrorListener { _ ->
                // Handle error
                PaymentFailureScreen(
                    errorMessage = "Please retry using other payment method or try again in sometime"
                ).show(parentFragmentManager, "FailureScreen")
                removeLoadingState()
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
        requestQueue.add(jsonObjectRequest)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentMainBottomSheetBinding.inflate(inflater, container, false)

        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
        binding.boxpayLogoLottie.playAnimation()


        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        queue = Volley.newRequestQueue(requireContext())
        editor = sharedPreferences.edit()


        val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
        bottomSheet = DeliveryAddressBottomSheet.newInstance(
            this,
            false,
            showName,
            showPhone,
            showEmail,
            showShipping
        )

        if (userAgentHeader.contains("Mobile", ignoreCase = true)) {
            isTablet = false

            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            isTablet = true
        }

        val callback = SingletonClassForLoadingState.getInstance().getYourObject()

        callback?.onBottomSheetOpened?.invoke()

        val baseUrlFetched = sharedPreferences.getString("baseUrl", "null")

        Base_Session_API_URL = "https://${baseUrlFetched}/v0/checkout/sessions/"

        fetchTransactionDetailsFromSharedPreferences()
        overlayViewModel.showOverlay.observe(this, Observer { showOverlay ->
            if (showOverlay) {
                addOverlayToActivity()
            } else {
                removeOverlayFromActivity()
            }
        })
        overlayViewModel.setShowOverlay(true)

        hidePriceBreakUp()

        val callBackFunctionsForDismissing = CallbackForDismissMainSheet(::dismissMainSheet)
        SingletonForDismissMainSheet.getInstance().callBackFunctions =
            callBackFunctionsForDismissing

        val orderSummaryAdapter =
            OrderSummaryItemsAdapter(imagesUrls, items, prices, itemQty, requireContext())
        binding.itemsInOrderRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.itemsInOrderRecyclerView.adapter = orderSummaryAdapter

        var currencySymbol = sharedPreferences.getString("currencySymbol", "")
        updateTransactionAmountInSharedPreferences(currencySymbol + transactionAmount.toString())
        if (currencySymbol == "")
            currencySymbol = "₹"

        showUPIOptions()


        // Set click listeners

        binding.orderSummaryConstraintLayout.setOnClickListener { // Toggle visibility of the price break-up card
            if (!priceBreakUpVisible) {
                showPriceBreakUp()
                priceBreakUpVisible = true
            } else {
                hidePriceBreakUp()
                priceBreakUpVisible = false
            }
        }
        binding.itemsInOrderRecyclerView.setOnClickListener() {
            //Just to preventing user from clicking here and closing the order summary
        }

        binding.totalValueRelativeLayout.setOnClickListener() {
            //Just to preventing user from clicking here and closing the order summary
        }

        binding.backButton.setOnClickListener() {
            removeOverlayFromActivity()
            dismiss()
        }

        var upiOptionsShown = true
        binding.upiLinearLayout.setOnClickListener() {
            if (!upiOptionsShown) {
                upiOptionsShown = true
                showUPIOptions()
            } else {
                upiOptionsShown = false
                hideUPIOptions()
            }
        }

        binding.addNewUPIIDConstraint.setOnClickListener() {
            binding.addNewUPIIDConstraint.isEnabled = false
            callUIAnalytics(requireContext(), "PAYMENT_INSTRUMENT_PROVIDED", "UpiCollect", "Upi")
            callUIAnalytics(requireContext(), "PAYMENT_CATEGORY_SELECTED", "", "Upi")
            callUIAnalytics(requireContext(), "PAYMENT_METHOD_SELECTED", "UpiCollect", "Upi")
            job?.cancel()
            openAddUPIIDBottomSheet()
        }

        binding.UPIQRConstraint.setOnClickListener() {
            if (qrCodeShown) {
                qrCodeShown = false
                binding.UPIQRConstraint.isEnabled = true
                hideQRCode()
            } else {
                qrCodeShown = true
                showQRCode()
            }
        }

        binding.qrCodeOpenConstraint.setOnClickListener() {
            // for the sake that it does not open or closes the options
        }

        binding.cardConstraint.setOnClickListener() {
            binding.cardConstraint.isEnabled = false
            callUIAnalytics(requireContext(), "PAYMENT_CATEGORY_SELECTED", "", "Card")
            callUIAnalytics(requireContext(), "PAYMENT_METHOD_SELECTED", "", "Card")
            openAddCardBottomSheet()
        }


        binding.walletConstraint.setOnClickListener() {
            binding.walletConstraint.isEnabled = false
            callUIAnalytics(requireContext(), "PAYMENT_CATEGORY_SELECTED", "", "Wallet")
            openWalletBottomSheet()
        }

        binding.bnplConstraint.setOnClickListener() {
            binding.bnplConstraint.isEnabled = false
            callUIAnalytics(requireContext(), "PAYMENT_CATEGORY_SELECTED", "", "BuyNowPayLater")
            openBNPLBottomSheet()
        }


        binding.netBankingConstraint.setOnClickListener() {
            binding.netBankingConstraint.isEnabled = false
            callUIAnalytics(requireContext(), "PAYMENT_CATEGORY_SELECTED", "", "NetBanking")
            openNetBankingBottomSheet()
        }

        binding.refreshButton.setOnClickListener() {
            showQRCode()
        }

        binding.deliveryAddressConstraintLayout.setOnClickListener() {
            if (!sharedPreferences.getString("phoneNumber", "").isNullOrEmpty()) {
                val confirmPhoneNumber = sharedPreferences.getString("phoneNumber", "")
                    ?.removePrefix(countryCode?.second ?: "")
                editor.putString("phoneNumber", confirmPhoneNumber)
                editor.apply()
            }
            bottomSheet = DeliveryAddressBottomSheet.newInstance(
                this,
                false,
                showName,
                showPhone,
                showEmail,
                showShipping
            )
            bottomSheet.show(parentFragmentManager, "DeliveryAddressBottomSheetOnClick")
        }

        binding.proceedButton.setOnClickListener() {
            if (!sharedPreferences.getString("phoneNumber", "").isNullOrEmpty()) {
                val confirmPhoneNumber = sharedPreferences.getString("phoneNumber", "")
                    ?.removePrefix(countryCode?.second ?: "")
                editor.putString("phoneNumber", confirmPhoneNumber)
                editor.apply()
            }
            bottomSheet.show(parentFragmentManager, "DeliveryAddressBottomSheetOnClick")
        }

        return binding.root
    }

    fun dismissMainSheet() {
        dismissThroughAnotherBottomSheet = true


        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val cardBottomSheet =
                parentFragmentManager.findFragmentByTag("AddCardBottomSheet") as? AddCardBottomSheet
            if (cardBottomSheet?.isResumed == true) {
                cardBottomSheet?.dismissCurrentBottomSheet()
            }
            val addUPIID =
                parentFragmentManager.findFragmentByTag("AddUPIBottomSheet") as? AddUPIID
            addUPIID?.dismissCurrentBottomSheet()
            val walletBottomSheet =
                parentFragmentManager.findFragmentByTag("WalletBottomSheet") as? WalletBottomSheet
            walletBottomSheet?.dismissCurrentBottomSheet()
            val netBankingBottomSheet =
                parentFragmentManager.findFragmentByTag("NetBankingBottomSheet") as? NetBankingBottomSheet
            netBankingBottomSheet?.dismissCurrentBottomSheet()

            dismiss()
        }, 500)
    }

    private fun showQRCode() {
        qrCodeShown = true
        binding.qrCodeOpenConstraint.visibility = View.VISIBLE
        showLoadingState("showQRCode")
        binding.refreshButton.visibility = View.GONE
        fetchQRCode()
    }

    private fun fetchQRCode() {
        postRequestForQRCode(requireContext())
    }

    private fun hideQRCode() {
        qrCodeShown = false
        countdownTimer.cancel()
        binding.qrCodeOpenConstraint.visibility = View.GONE
    }

    private fun startTimer() {
        countdownTimer = object : CountDownTimer(300000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                // Update TextView with the remaining time
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                val timeString = String.format("%02d:%02d", minutes, seconds)
                binding.qrCodeTimer.text = timeString + " min"
            }

            override fun onFinish() {
                // Handle onFinish event if needed
                binding.qrCodeTimer.text = "00:00"
                binding.refreshButton.visibility = View.VISIBLE
                job?.cancel()
                blurImageView()
            }
        }
        countdownTimer.start()
    }

    private fun blurImageView() {
        // Get the current Bitmap from the ImageView
        val bitmap = (binding.qrCodeImageView.drawable as BitmapDrawable).bitmap

        // Apply blur transformation using Glide and BlurTransformation
        Glide.with(requireContext())
            .asBitmap()
            .load(bitmap) // Load the bitmap directly
            .apply(
                RequestOptions.bitmapTransform(
                    BlurTransformation(
                        25,
                        3
                    )
                )
            ) // Apply blur transformation
            .into(binding.qrCodeImageView) // Set the blurred bitmap back to the ImageView
    }

    private fun callPaymentMethodRules(context: Context) {
        val requestQueue = Volley.newRequestQueue(context)

        val countryName = sharedPreferences.getString("countryCode", null)

        val jsonArrayRequest = object : JsonArrayRequest(
            Method.GET,
            "$Base_Session_API_URL$token/payment-methods?customerCountryCode=$countryName",
            null,
            Response.Listener { response ->
                for (i in 0 until response.length()) {
                    val paymentMethod = response.getJSONObject(i)
                    val paymentMethodName = paymentMethod.getString("type")
                    if (paymentMethodName == "Upi") {
                        val brand = paymentMethod.getString("brand")
                        if (brand == "UpiCollect") {
                            upiCollectMethod = true
                            upiAvailable = true
                        }


                        if (brand == "UpiIntent") {
                            upiIntentMethod = true
                            upiAvailable = true
                        }

                        if (brand == "UpiQr") {
                            val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())

                            if (!userAgentHeader.contains("Mobile", ignoreCase = true)) {
                                upiQRMethod = true
                            }
                            upiAvailable = true
                        }
                    }
                    if (paymentMethodName == "Card") {
                        cardsMethod = true
                    }
                    if (paymentMethodName == "Wallet") {
                        walletMethods = true
                    }
                    if (paymentMethodName == "BuyNowPayLater") {
                        bnplMethod = true
                    }
                    if (paymentMethodName == "NetBanking") {
                        netBankingMethods = true
                    }
                }

                if (upiAvailable) {
                    binding.cardView4.visibility = View.VISIBLE


                    if (upiCollectMethod) {
                        binding.addNewUPIIDConstraint.visibility = View.VISIBLE
                    }

                    if (upiQRMethod) {
                        if (!upiIntentMethod && !upiCollectMethod && !cardsMethod && !walletMethods && !netBankingMethods && !bnplMethod) {
                            showQRCode()
                        }
                        binding.UPIQRConstraint.visibility = View.VISIBLE
                    }

                } else {
                    binding.cardView4.visibility = View.GONE
                }

                if (cardsMethod) {
                    binding.cardView5.visibility = View.VISIBLE
                } else {
                    binding.cardView5.visibility = View.GONE
                }

                if (walletMethods) {
                    binding.cardView6.visibility = View.VISIBLE
                } else {
                    binding.cardView6.visibility = View.GONE
                }

                if (bnplMethod) {
                    binding.cardView9.visibility = View.VISIBLE
                } else {
                    binding.cardView9.visibility = View.GONE
                }

                if (netBankingMethods) {
                    binding.cardView7.visibility = View.VISIBLE
                } else {
                    binding.cardView7.visibility = View.GONE
                }
            },
            Response.ErrorListener { /* no response handling */
                removeLoadingState()
                removeLoadingState()
                PaymentFailureScreen(
                    errorMessage = "Please retry using other payment method or try again in sometime"
                ).show(parentFragmentManager, "FailureScreen")}) {
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


    private fun postRequestForQRCode(context: Context) {

        val requestQueue = Volley.newRequestQueue(context)


        // Constructing the request body
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
                put("timeZoneOffSet", 330)
                put("packageId", requireActivity().packageName)// Example value
            }
            put("browserData", browserData)

            // Instrument Details
            val instrumentDetailsObject = JSONObject().apply {
                put("type", "upi/qr")
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

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->

                transactionId = response.getString("transactionId").toString()
                updateTransactionIDInSharedPreferences(transactionId!!)
                val valuesObject = response.getJSONArray("actions").getJSONObject(0)
                val urlBase64 = valuesObject.getString("content")


                val decodedBytes: ByteArray = Base64.decode(urlBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                val imageView: ImageView = binding.qrCodeImageView
                imageView.setImageBitmap(bitmap)
                removeLoadingState()
                startTimer()
                startFunctionCalls()
            },
            Response.ErrorListener { /* no response handling */
                removeLoadingState()
                PaymentFailureScreen(
                    errorMessage = "Please retry using other payment method or try again in sometime"
                ).show(parentFragmentManager, "FailureScreen")}) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
                headers["X-Client-Connector-Name"] =  "Android SDK"
                headers["X-Client-Connector-Version"] =  BuildConfig.SDK_VERSION
                return headers
            }
        }.apply {
            val timeoutMs = 100000 // Timeout in milliseconds
            val maxRetries = 0 // Max retry attempts
            val backoffMultiplier = 1.0f // Backoff multiplier
            retryPolicy = DefaultRetryPolicy(timeoutMs, maxRetries, backoffMultiplier)
        }

        requestQueue.add(jsonObjectRequest)
    }

    private fun updateTransactionIDInSharedPreferences(transactionIdArg: String) {
        editor.putString("transactionId", transactionIdArg)
        editor.putString("operationId", transactionIdArg)
        editor.apply()
    }

    fun enabledButtonsForAllPaymentMethods() {
        binding.addNewUPIIDConstraint.isEnabled = true
        binding.cardConstraint.isEnabled = true
        binding.walletConstraint.isEnabled = true
        binding.netBankingConstraint.isEnabled = true
        binding.bnplConstraint.isEnabled = true
    }

    private fun populatePopularUPIApps() {
        var i = 1
        if (UPIAppsAndPackageMap.containsKey("PhonePe")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setImageResource(R.drawable.phonepe_logo)
            textView.text = "PhonePe"
            getPopularConstraintLayoutByNum(i).setOnClickListener() {
                overlayViewModel.setShowOverlay(false)
                fetchUPIIntentURL(requireContext(), "PhonePe")
                callUIAnalytics(requireContext(), "PAYMENT_INSTRUMENT_PROVIDED", "UpiIntent", "Upi")
                callUIAnalytics(requireContext(), "PAYMENT_METHOD_SELECTED", "UpiIntent", "Upi")
                callUIAnalytics(requireContext(), "PAYMENT_INITIATED", "UpiIntent", "Upi")
            }

            i++
        }


        if (UPIAppsAndPackageMap.containsKey("GPay")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setImageResource(R.drawable.google_pay_seeklogo)
            textView.text = "GPay"

            getPopularConstraintLayoutByNum(i).setOnClickListener() {
                overlayViewModel.setShowOverlay(false)
                fetchUPIIntentURL(requireContext(), "GPay")
                callUIAnalytics(requireContext(), "PAYMENT_INSTRUMENT_PROVIDED", "UpiIntent", "Upi")
                callUIAnalytics(requireContext(), "PAYMENT_METHOD_SELECTED", "UpiIntent", "Upi")
                callUIAnalytics(requireContext(), "PAYMENT_INITIATED", "UpiIntent", "Upi")
            }

            i++
        }


        if (UPIAppsAndPackageMap.containsKey("Paytm")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setImageResource(R.drawable.paytm_upi_logo)
            textView.text = "Paytm"

            getPopularConstraintLayoutByNum(i).setOnClickListener() {
                overlayViewModel.setShowOverlay(false)
                fetchUPIIntentURL(requireContext(), "PayTm")
                callUIAnalytics(requireContext(), "PAYMENT_INSTRUMENT_PROVIDED", "UpiIntent", "Upi")
                callUIAnalytics(requireContext(), "PAYMENT_METHOD_SELECTED", "UpiIntent", "Upi")
                callUIAnalytics(requireContext(), "PAYMENT_INITIATED", "UpiIntent", "Upi")
            }

            i++
        }

        val imageView = getPopularImageViewByNum(i)
        val textView = getPopularTextViewByNum(i)
        imageView.setImageResource(R.drawable.ic_others)
        textView.text = "Others"

        getPopularConstraintLayoutByNum(i).setOnClickListener() {
            showLoadingState("payUsingAnyUPIConstraint")
            getUrlForDefaultUPIIntent()
            callUIAnalytics(requireContext(), "PAYMENT_INSTRUMENT_PROVIDED", "UpiIntent", "Upi")
            callUIAnalytics(requireContext(), "PAYMENT_METHOD_SELECTED", "UpiIntent", "Upi")
            callUIAnalytics(requireContext(), "PAYMENT_INITIATED", "UpiIntent", "Upi")
        }

        if (i == 1 || i < 1) {
            binding.popularUPIAppsConstraint.visibility = View.GONE
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
        val userAgentHeader = WebSettings.getDefaultUserAgent(context)
        val browserLanguage = Locale.getDefault().toString()

        // Constructing the request body
        val requestBody = JSONObject().apply {
            put("callerToken", token)
            put("uiEvent", event)

            // Create eventAttrs JSON object
            val eventAttrs = JSONObject().apply {
                put("paymentType", paymentType)

                if (paymentSubType.isBlank())
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

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, "https://${baseUrl}/v0/ui-analytics", requestBody,
            Response.Listener { /*no response handling */ },
            Response.ErrorListener { /*no response handling */ }) {}.apply {
            val timeoutMs = 100000 // Timeout in milliseconds
            val maxRetries = 0 // Max retry attempts
            val backoffMultiplier = 1.0f // Backoff multiplier
            retryPolicy = DefaultRetryPolicy(timeoutMs, maxRetries, backoffMultiplier)
        }
        requestQueue.add(jsonObjectRequest)
    }

    private fun getPopularImageViewByNum(num: Int): ImageView {
        return when (num) {
            1 -> binding.popularUPIImageView1
            2 -> binding.popularUPIImageView2
            3 -> binding.popularUPIImageView3
            4 -> binding.popularUPIImageView4
            else -> throw IllegalArgumentException("Invalid number: $num")
        }
    }

    private fun getPopularConstraintLayoutByNum(num: Int): LinearLayout {
        return when (num) {
            1 -> binding.PopularUPILinearLayout1
            2 -> binding.PopularUPILinearLayout2
            3 -> binding.PopularUPILinearLayout3
            4 -> binding.PopularUPILinearLayout4
            else -> throw IllegalArgumentException("Invalid number: $num")
        }
    }

    private fun getPopularTextViewByNum(num: Int): TextView {
        return when (num) {
            1 -> binding.popularUPITextView1
            2 -> binding.popularUPITextView2
            3 -> binding.popularUPITextView3
            4 -> binding.popularUPITextView4
            else -> throw IllegalArgumentException("Invalid number: $num")
        }
    }

    private fun openDefaultUPIIntentBottomSheetFromAndroid(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startFunctionCalls()
        startActivityForResult(intent, 124)
        removeLoadingState()
    }

    private fun getUrlForDefaultUPIIntent() {

        val requestQueue = Volley.newRequestQueue(context)


        // Constructing the request body
        val requestBody = JSONObject().apply {
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
                put("packageId", requireActivity().packageName)// Example value
            }
            put("browserData", browserData)
            val instrumentDetailsObject = JSONObject().apply {
                put("type", "upi/intent")
            }

            // Instrument Details
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

                // Handle response

                try {
                    val actionsArray = response.getJSONArray("actions")
                    val urlForIntent = actionsArray.getJSONObject(0).getString("url")

                    val status = response.getJSONObject("status").getString("status")
                    val reason = response.getJSONObject("status").getString("reason")
                    val reasonCode = response.getJSONObject("status").getString("reasonCode")
                    transactionId = response.getString("transactionId").toString()
                    updateTransactionIDInSharedPreferences(transactionId!!)

                    if (status.contains("rejected", ignoreCase = true)) {
                        removeLoadingState()
                        var cleanedMessage = reason.substringAfter(":")
                        if (!reasonCode.startsWith("uf", true)) {
                            cleanedMessage = "Please retry using other payment method or try again in sometime"
                        }
                        PaymentFailureScreen(errorMessage = cleanedMessage).show(
                            parentFragmentManager,
                            "FailureScreenFromUPIIntent"
                        )
                    } else if (status.contains("RequiresAction", ignoreCase = true)) {
                        editor.putString("status", "RequiresAction")
                        editor.apply()
                    }
                    val urlInBase64 = urlToBase64(urlForIntent)
                    openDefaultUPIIntentBottomSheetFromAndroid(urlInBase64)

                } catch (e: JSONException) {

                    removeLoadingState()
                    PaymentFailureScreen().show(
                        parentFragmentManager,
                        "FailureScreenFromDefaultUPIIntent"
                    )
                }
            },
            Response.ErrorListener { /* no response handling */
                removeLoadingState()
                PaymentFailureScreen(
                    errorMessage = "Please retry using other payment method or try again in sometime"
                ).show(parentFragmentManager, "FailureScreen")
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
        requestQueue.add(jsonObjectRequest)
    }

    private fun addOverlayToActivity() {
        overLayPresent = true
        overlayViewMainBottomSheet = View(requireContext())
        overlayViewMainBottomSheet?.setBackgroundColor(Color.parseColor("#80000000")) // Adjust the color and transparency as needed

        val windowManager =
            requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(overlayViewMainBottomSheet, layoutParams)
    }

    private fun removeOverlayFromActivity() {
        overlayViewMainBottomSheet?.let {
            val windowManager =
                requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.removeView(it)
        }
        overlayViewMainBottomSheet = null
    }

    fun removeOverlayFromCurrentBottomSheet() {
        overlayViewCurrentBottomSheet?.let {
            binding.root.removeView(it)
        }
    }

    private fun showPriceBreakUp() {
        binding.itemsInOrderRecyclerView.visibility = View.VISIBLE
        binding.textView18.visibility = View.VISIBLE
        binding.ItemsPrice.visibility = View.VISIBLE
        binding.priceBreakUpDetailsLinearLayout.visibility = View.VISIBLE
        binding.arrowIcon.animate()
            .rotation(180f)
            .setDuration(250) // Set the duration of the animation in milliseconds
            .withEndAction {}
            .start()
    }

    private fun hidePriceBreakUp() {
        binding.itemsInOrderRecyclerView.visibility = View.GONE
        binding.textView18.visibility = View.GONE
        binding.ItemsPrice.visibility = View.GONE
        binding.priceBreakUpDetailsLinearLayout.visibility = View.GONE
        binding.arrowIcon.animate()
            .rotation(0f)
            .setDuration(250) // Set the duration of the animation in milliseconds
            .withEndAction {}
            .start()
    }

    private fun showUPIOptions() {
        binding.upiConstraint.setBackgroundColor(Color.parseColor("#E0F1FF"))
        binding.upiOptionsLinearLayout.visibility = View.VISIBLE
        binding.textView20.typeface =
            ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)

        if (i > 1) {
            binding.popularUPIAppsConstraint.visibility = View.VISIBLE
        }
    }


    private fun hideUPIOptions() {
        binding.upiConstraint.setBackgroundColor(Color.parseColor("#FFFFFF"))
        binding.upiOptionsLinearLayout.visibility = View.GONE
        binding.textView20.typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins)
        binding.popularUPIAppsConstraint.visibility = View.GONE
        binding.imageView12.animate()
            .rotation(0f)
            .setDuration(500) // Set the duration of the animation in milliseconds
            .withEndAction {}
            .start()
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
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            }


            val screenHeight = requireContext().resources.displayMetrics.heightPixels
            val percentageOfScreenHeight = 0.7 // 70%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

            bottomSheetBehavior?.maxHeight = desiredHeight

            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.isHideable = false


            dialog.setCancelable(false)

            bottomSheetBehavior?.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    // Handle state changes
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            //Hidden
                            dismiss()
                            val callback = SingletonClass.getInstance().getYourObject()
                            if (callback != null) {
                                val status = sharedPreferences.getString("status", "")
                                val transactionIdFetched =
                                    sharedPreferences.getString("transactionId", "")
                                val operationIdFetched =
                                    sharedPreferences.getString("operationId", "")
                                callback.onPaymentResult(
                                    PaymentResultObject(
                                        status.toString(),
                                        transactionIdFetched.toString(),
                                        operationIdFetched.toString()
                                    )
                                )
                            }
                        }

                        else -> {
                            // no op
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {

                }
            })
        }
        return dialog
    }

    private fun openAddUPIIDBottomSheet() {
        val bottomSheetFragment = AddUPIID.newInstance(shippingEnabled)
        bottomSheetFragment.show(parentFragmentManager, "AddUPIBottomSheet")
    }

    private fun openAddCardBottomSheet() {
        val bottomSheetFragment =
            AddCardBottomSheet.newInstance(shippingEnabled)
        bottomSheetFragment.show(parentFragmentManager, "AddCardBottomSheet")
    }

    private fun openNetBankingBottomSheet() {

        val bottomSheetFragment = NetBankingBottomSheet.newInstance(shippingEnabled)
        bottomSheetFragment.show(parentFragmentManager, "NetBankingBottomSheet")
    }

    private fun openWalletBottomSheet() {

        val bottomSheetFragment = WalletBottomSheet.newInstance(shippingEnabled)
        bottomSheetFragment.show(parentFragmentManager, "WalletBottomSheet")
    }

    private fun openBNPLBottomSheet() {

        val bottomSheetFragment = BNPLBottomSheet.newInstance(shippingEnabled)
        bottomSheetFragment.show(parentFragmentManager, "BnplBottomSheet")
    }

    private fun makeSessionDataCall() {

        val url = "${Base_Session_API_URL}${token}"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObjectAll = JsonObjectRequest(Request.Method.GET, url, null, { response ->

            try {
                val paymentDetailsObject = response.getJSONObject("paymentDetails")

                val totalAmount = paymentDetailsObject.getJSONObject("money").getString("amount")


                var orderObject: JSONObject? = null
                if (!paymentDetailsObject.isNull("order")) {
                    orderObject = paymentDetailsObject.getJSONObject("order")
                }


                val originalAmount = orderObject?.getString("originalAmount")

                val shippingCharges = orderObject?.getString("shippingAmount") ?: "0"


                val taxes = orderObject?.getString("taxAmount") ?: "0"

                val additionalDetails =
                    response.getJSONObject("configs").getJSONArray("additionalFieldSets")

                var orderSummaryEnable = false

                for (i in 0 until additionalDetails.length()) {
                    if (additionalDetails.get(i) == "ORDER_ITEM_DETAILS") {
                        orderSummaryEnable = true
                    }
                    if (additionalDetails.get(i).equals("BILLING_ADDRESS") || additionalDetails.get(
                            i
                        ).equals("SHIPPING_ADDRESS")
                    ) {
                        showShipping = true
                    }
                    if (additionalDetails.get(i).equals("SHOPPER_EMAIL")) {
                        showEmail = true
                    }
                    if (additionalDetails.get(i).equals("SHOPPER_NAME")) {
                        showName = true
                    }
                    if (additionalDetails.get(i).equals("SHOPPER_PHONE")) {
                        showPhone = true
                    }
                }
                if (showShipping) {
                    binding.textView6.text = "Add New Address"
                } else {
                    binding.textView6.text = "Add Personal Details"
                }

                var currencySymbol = sharedPreferences.getString("currencySymbol", "")
                if (currencySymbol == "")
                    currencySymbol = "₹"

                var totalQuantity = 0

                transactionAmount = totalAmount


                val itemsArray =
                    if (orderObject?.optJSONArray("items") != null) orderObject.getJSONArray("items") else null

                if (itemsArray != null) {
                    for (i in 0 until itemsArray.length()) {
                        val itemObject = itemsArray.getJSONObject(i)

                        items.add(itemObject.getString("itemName"))
                        prices.add(itemObject.getString("amountWithoutTaxLocale"))
                        val quantity = itemObject.getInt("quantity")
                        itemQty.add(quantity.toString())
                        totalQuantity += quantity
                    }
                }

                val merchantDetailsObject = response.getJSONObject("merchantDetails")
                val checkoutThemeObject = merchantDetailsObject.getJSONObject("checkoutTheme")

                val sharedPreferences = requireContext().getSharedPreferences(
                    "TransactionDetails",
                    Context.MODE_PRIVATE
                )
                val editor = sharedPreferences.edit()

                editor.putString("headerColor", checkoutThemeObject.getString("headerColor"))
                editor.putString(
                    "primaryButtonColor",
                    checkoutThemeObject.getString("primaryButtonColor")
                )
                editor.putString(
                    "buttonTextColor",
                    checkoutThemeObject.getString("buttonTextColor")
                )
                editor.apply()

                transactionAmount = totalAmount.toString()

                binding.unopenedTotalValue.text = "${currencySymbol}${totalAmount}"
                if (totalQuantity == 0) {
                    binding.numberOfItems.text = "Total"
                } else if (totalQuantity == 1)
                    binding.numberOfItems.text = "${totalQuantity} item"
                else
                    binding.numberOfItems.text = "${totalQuantity} items"
                binding.ItemsPrice.text = "${currencySymbol}${totalAmount}"

                if (originalAmount != totalAmount) {
                    if (originalAmount == "null" || originalAmount == "0") {
                        binding.subTotalRelativeLayout.visibility = View.GONE
                    } else {
                        binding.subtotalTextView.text = "${currencySymbol}${originalAmount}"
                        binding.subTotalRelativeLayout.visibility = View.VISIBLE
                    }
                }

                if (taxes != "null") {
                    if (taxes == "0") {
                        binding.taxesRelativeLayout.visibility = View.GONE
                    } else {
                        binding.taxTextView.text = "${currencySymbol}${taxes}"
                        binding.taxesRelativeLayout.visibility = View.VISIBLE
                    }

                }

                if (shippingCharges != "null") {
                    if (shippingCharges == "0") {
                        binding.shippingChargesRelativeLayout.visibility = View.GONE
                    } else {
                        binding.shippingChargesTextView.text = "${currencySymbol}${shippingCharges}"
                        binding.shippingChargesRelativeLayout.visibility = View.VISIBLE
                    }
                }

                if (originalAmount == "0" && shippingCharges == "0" && taxes == "0") {
                    binding.arrowIcon.visibility = View.GONE
                    binding.orderSummaryConstraintLayout.setOnClickListener(null)
                }

                val moneyObject = paymentDetailsObject.getJSONObject("money")
                editor.putString("amount", moneyObject.getString("amount"))
                editor.putString("merchantId", response.getString("merchantId"))
                editor.putString(
                    "countryCode",
                    paymentDetailsObject.getJSONObject("context").getString("countryCode")
                )
                editor.putString(
                    "legalEntity",
                    paymentDetailsObject.getJSONObject("context").getJSONObject("legalEntity")
                        .getString("code")
                )

                val shopperObject = paymentDetailsObject.getJSONObject("shopper")
                if (!shopperObject.isNull("uniqueReference")) {
                    editor.putString("uniqueReference", shopperObject.getString("uniqueReference"))
                }
                if (shopperObject.isNull("deliveryAddress") && showShipping) {
                    binding.deliveryAddressConstraintLayout.visibility = View.GONE
                    binding.textView12.visibility = View.GONE
                    binding.upiLinearLayout.visibility = View.GONE
                    binding.cardView5.visibility = View.GONE
                    binding.cardView7.visibility = View.GONE
                    binding.netBankingConstraint.visibility = View.GONE
                    binding.bnplConstraint.visibility = View.GONE
                    binding.cardConstraint.visibility = View.GONE
                    binding.walletConstraint.visibility = View.GONE
                    binding.linearLayout.visibility = View.GONE
                    binding.proceedButton.visibility = View.VISIBLE
                    priceBreakUpVisible = true
                    bottomSheet = DeliveryAddressBottomSheet.newInstance(
                        this,
                        true,
                        showName,
                        showPhone,
                        showEmail,
                        showShipping
                    )
                    showPriceBreakUp()
                } else if (shopperObject.isNull("firstName") || shopperObject.isNull("phoneNumber")) {
                    binding.deliveryAddressConstraintLayout.visibility = View.GONE
                    binding.textView12.visibility = View.GONE
                    binding.upiLinearLayout.visibility = View.GONE
                    binding.cardView5.visibility = View.GONE
                    binding.cardView7.visibility = View.GONE
                    binding.netBankingConstraint.visibility = View.GONE
                    binding.bnplConstraint.visibility = View.GONE
                    binding.cardConstraint.visibility = View.GONE
                    binding.walletConstraint.visibility = View.GONE
                    binding.linearLayout.visibility = View.GONE
                    binding.proceedButton.visibility = View.VISIBLE
                    priceBreakUpVisible = true
                    bottomSheet = DeliveryAddressBottomSheet.newInstance(
                        this,
                        true,
                        showName,
                        showPhone,
                        showEmail,
                        showShipping
                    )
                    showPriceBreakUp()
                } else {
                    binding.proceedButton.visibility = View.GONE
                    if (!shopperObject.isNull("firstName")) {
                        editor.putString("firstName", shopperObject.getString("firstName"))
                    }
                    if (!shopperObject.isNull("lastName")) {
                        editor.putString("lastName", shopperObject.getString("lastName"))
                    }
                    if (!shopperObject.isNull("gender")) {
                        editor.putString("gender", shopperObject.getString("gender"))
                    }
                    if (!shopperObject.isNull("email")) {
                        editor.putString("email", shopperObject.getString("email"))
                    }
                    if (!shopperObject.isNull("phoneNumber")) {
                        if (shopperObject.getString("phoneNumber").contains('+')) {
                            editor.putString(
                                "phoneNumber",
                                shopperObject.getString("phoneNumber")
                            )
                        } else {
                            editor.putString(
                                "phoneNumber",
                                "+" + shopperObject.getString("phoneNumber")
                            )
                        }
                    }
                    val jsonString = readJsonFromAssets(requireContext(), "countryCodes.json")
                    val countryCodeJson = JSONObject(jsonString)
                    val countryCodesArray = loadCountryCodes(countryCodeJson)
                    countryCode = getCountryCode(
                        countryCodesArray,
                        sharedPreferences.getString("phoneNumber", null) ?: "+91"
                    )
                    if (!shopperObject.isNull("deliveryAddress")) {
                        val deliveryAddress = shopperObject.getJSONObject("deliveryAddress")
                        if (!deliveryAddress.isNull("address1")) {
                            editor.putString("address1", deliveryAddress.getString("address1"))
                        }
                        if (!deliveryAddress.isNull("address2")) {
                            editor.putString("address2", deliveryAddress.getString("address2"))
                        }
                        if (!deliveryAddress.isNull("countryCode")) {
                            countryCode = getCountryName(
                                countryCodeJson,
                                deliveryAddress.getString("countryCode")
                            )
                            editor.putString("countryName", countryCode?.first)
                            editor.putString("indexCountryCodePhone", countryCode?.second)
                            editor.putString("countryCodePhoneNum", countryCode?.second)
                        }
                        if (!deliveryAddress.isNull("city")) {
                            editor.putString("city", deliveryAddress.getString("city"))
                        }
                        if (!deliveryAddress.isNull("state")) {
                            editor.putString("state", deliveryAddress.getString("state"))
                        }
                        if (!deliveryAddress.isNull("postalCode")) {
                            editor.putString("postalCode", deliveryAddress.getString("postalCode"))
                        }
                    }
                }


                if (paymentDetailsObject.isNull("order"))
                    orderSummaryEnable = false

                if (!orderSummaryEnable && !totalAmount.isNullOrEmpty()) {
                    binding.textView9.text = "Payment Summary"
                    binding.numberOfItems.text = "Total"
                }

                if (!shippingEnabled) {
                    val paymentMethodsArray =
                        response.getJSONObject("configs").getJSONArray("paymentMethods")

                    try {
                        if (!paymentDetailsObject.isNull("order")) {
                            val itemsArray = if (paymentDetailsObject.getJSONObject("order")
                                    .optJSONArray("items") != null
                            ) paymentDetailsObject.getJSONObject("order")
                                .optJSONArray("items") else null
                            if (itemsArray != null) {
                                for (i in 0 until itemsArray.length()) {
                                    val imageURL = itemsArray.getJSONObject(i).getString("imageUrl")
                                    imagesUrls.add(imageURL)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        binding.cardView3.visibility = View.GONE
                    }

                    for (i in 0 until paymentMethodsArray.length()) {
                        val paymentMethod = paymentMethodsArray.getJSONObject(i)
                        val paymentMethodName = paymentMethod.getString("type")
                        if (paymentMethodName == "Upi") {
                            val brand = paymentMethod.getString("brand")
                            if (brand == "UpiCollect") {
                                upiCollectMethod = true
                                upiAvailable = true
                            }
                            if (brand == "UpiIntent") {
                                upiIntentMethod = true
                                upiAvailable = true
                            }
                            if (brand == "UpiQr") {
                                val userAgentHeader =
                                    WebSettings.getDefaultUserAgent(requireContext())
                                if (!userAgentHeader.contains("Mobile", ignoreCase = true)) {
                                    upiQRMethod = true
                                }
                                upiAvailable = true
                            }
                        }
                        if (paymentMethodName == "Card") {
                            cardsMethod = true
                        }
                        if (paymentMethodName == "Wallet") {
                            walletMethods = true
                        }
                        if (paymentMethodName == "BuyNowPayLater") {
                            bnplMethod = true
                        }
                        if (paymentMethodName == "NetBanking") {
                            netBankingMethods = true
                        }
                    }

                    if (upiAvailable) {
                        binding.cardView4.visibility = View.VISIBLE

                        if (upiCollectMethod) {
                            binding.addNewUPIIDConstraint.visibility = View.VISIBLE
                        }
                        if (upiQRMethod) {
                            if (!upiIntentMethod && !upiCollectMethod && !cardsMethod && !walletMethods && !netBankingMethods && !bnplMethod) {
                                showQRCode()
                            }
                            binding.UPIQRConstraint.visibility = View.VISIBLE
                        }
                    } else {
                        binding.cardView4.visibility = View.GONE
                    }

                    if (cardsMethod) {
                        binding.cardView5.visibility = View.VISIBLE
                    } else {
                        binding.cardView5.visibility = View.GONE
                    }
                    if (walletMethods) {
                        binding.cardView6.visibility = View.VISIBLE
                    } else {
                        binding.cardView6.visibility = View.GONE
                    }
                    if (bnplMethod) {
                        binding.cardView9.visibility = View.VISIBLE
                    } else {
                        binding.cardView9.visibility = View.GONE
                    }
                    if (netBankingMethods) {
                        binding.cardView7.visibility = View.VISIBLE
                    } else {
                        binding.cardView7.visibility = View.GONE
                    }
                }

                editor.apply()

                binding.nameTextView.text = sharedPreferences.getString(
                    "firstName",
                    ""
                ) + " " + sharedPreferences.getString("lastName", "")
                binding.mobileNumberTextViewMain.text =
                    "(${sharedPreferences.getString("phoneNumber", "")})"
                binding.addressTextViewMain.text =
                    if (!showShipping) {
                        binding.textView2.text = "Personal details"
                        binding.homeIcon.visibility = View.GONE
                        binding.deliveryAddressObjectImage.setImageDrawable(
                            ContextCompat.getDrawable(
                                context!!,
                                R.drawable.ic_personal_details
                            )
                        )
                        sharedPreferences.getString("email", null)
                    } else {
                        binding.textView2.text = "Delivery Address"
                        binding.homeIcon.visibility = View.VISIBLE
                        binding.deliveryAddressObjectImage.setImageDrawable(
                            ContextCompat.getDrawable(
                                context!!,
                                R.drawable.truck
                            )
                        )
                        if (!sharedPreferences.getString("address2", null).isNullOrEmpty()) {
                            "${sharedPreferences.getString("address1", null)}\n" +
                                    "${sharedPreferences.getString("address2", null)}\n" +
                                    "${sharedPreferences.getString("city", null)}" +
                                    ", ${sharedPreferences.getString("state", "null")}" +
                                    ", ${sharedPreferences.getString("postalCode", "null")}"
                        } else {
                            "${sharedPreferences.getString("address1", null)}\n" +
                                    "${sharedPreferences.getString("city", null)}" +
                                    ", ${sharedPreferences.getString("state", "null")}" +
                                    ", ${sharedPreferences.getString("postalCode", "null")}"
                        }
                    }
                removeLoadingState()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Invalid token/selected environment.\nPlease press back button and try again",
                    Toast.LENGTH_LONG
                ).show()
            }

        }, { _ ->
            Toast.makeText(
                requireContext(),
                "Invalid token/selected environment.\nPlease press back button and try again",
                Toast.LENGTH_LONG
            ).show()
            dismiss()
        })
        queue.add(jsonObjectAll)
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences =
            requireContext().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token", "empty")
        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
    }


    private fun updateTransactionAmountInSharedPreferences(transactionAmountArgs: String) {
        val sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()


        editor.putString("transactionAmount", transactionAmountArgs)
        editor.apply()
    }

    override fun updateBottomSheet() {
        binding.nameTextView.text = sharedPreferences.getString(
            "firstName",
            ""
        ) + " " + sharedPreferences.getString("lastName", "")
        binding.mobileNumberTextViewMain.text =
            "(${sharedPreferences.getString("phoneNumber", "")})"
        binding.addressTextViewMain.text =
            if (!showShipping) {
                binding.textView2.text = "Personal details"
                sharedPreferences.getString("email", null)
            } else {
                binding.textView2.text = "Delivery Address"
                if (!sharedPreferences.getString("address2", null).isNullOrEmpty()) {
                    "${sharedPreferences.getString("address1", null)}\n" +
                            "${sharedPreferences.getString("address2", null)}\n" +
                            "${sharedPreferences.getString("city", null)}" +
                            ", ${sharedPreferences.getString("state", "null")}" +
                            ", ${sharedPreferences.getString("postalCode", "null")}"
                } else {
                    "${sharedPreferences.getString("address1", null)}\n" +
                            "${sharedPreferences.getString("city", null)}" +
                            ", ${sharedPreferences.getString("state", "null")}" +
                            ", ${sharedPreferences.getString("postalCode", "null")}"
                }
            }
        binding.cardView8.visibility = View.VISIBLE
        countryCode = Pair(
            sharedPreferences.getString("countryName", null) ?: "",
            sharedPreferences.getString("indexCountryCodePhone", null) ?: ""
        )

//        binding.deliveryAddressConstraintLayout.visibility = View.VISIBLE
        binding.textView12.visibility = View.VISIBLE
        binding.upiLinearLayout.visibility = View.VISIBLE
        binding.cardView5.visibility = View.VISIBLE
        binding.cardView7.visibility = View.VISIBLE
        binding.netBankingConstraint.visibility = View.VISIBLE
        binding.cardConstraint.visibility = View.VISIBLE
        binding.linearLayout.visibility = View.VISIBLE
        binding.proceedButton.visibility = View.GONE
        priceBreakUpVisible = true
        hidePriceBreakUp()

        callPaymentMethodRules(requireContext())

    }

    private fun readJsonFromAssets(context: Context, fileName: String): String {
        val assetManager = context.assets
        val inputStream = assetManager.open(fileName)
        val bufferedReader = inputStream.bufferedReader()
        return bufferedReader.use { it.readText() }
    }

    private fun getCountryName(
        countryCodeJson: JSONObject,
        countryCode: String
    ): Pair<String, String>? {
        countryCodeJson.keys().forEach { key ->
            if (key.equals(countryCode)) {
                val countryDetails = countryCodeJson.getJSONObject(key)
                val code = countryDetails.getString("fullName")
                val isd = countryDetails.getString("isdCode")
                return Pair(code, isd)
            }
        }
        return null
    }

    private fun getCountryCode(
        countryCodeJson: Array<String>,
        phoneNumber: String
    ): Pair<String, String>? {
        countryCodeJson.forEach { key ->
            if (phoneNumber.startsWith(key)) {
                return Pair("", key)
            }
        }
        return null
    }


    fun generateRandomAlphanumericString(length: Int): String {
        val charPool: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    fun loadCountryCodes(countryCodeJson: JSONObject): Array<String> {
        val isdCodes = mutableSetOf<String>()

        // Iterate through each country and extract the isdCode
        countryCodeJson.keys().forEach { key ->
            val countryDetails = countryCodeJson.getJSONObject(key)
            val isdCode = countryDetails.getString("isdCode")
            isdCodes.add(isdCode)
        }

        // Sort ISD codes in ascending order
        val sorted = isdCodes.sorted()

        return sorted.toTypedArray()
    }
}