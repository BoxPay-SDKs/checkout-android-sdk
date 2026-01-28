package com.boxpay.checkout.sdk

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Base64
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieDrawable
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.ViewModels.CallbackForDismissMainSheet
import com.boxpay.checkout.sdk.ViewModels.InstantOfferViewModel
import com.boxpay.checkout.sdk.ViewModels.OverlayViewModel
import com.boxpay.checkout.sdk.ViewModels.SingletonClassForLoadingState
import com.boxpay.checkout.sdk.ViewModels.SingletonForDismissMainSheet
import com.boxpay.checkout.sdk.adapters.OrderSummaryItemsAdapter
import com.boxpay.checkout.sdk.adapters.RecommendedItemsAdapter
import com.boxpay.checkout.sdk.adapters.SavedCardsItemsAdaptor
import com.boxpay.checkout.sdk.adapters.SavedUpiItemsAdaptor
import com.boxpay.checkout.sdk.composeScreens.components.ApplyCouponCard
import com.boxpay.checkout.sdk.composeScreens.model.defaultFontFamily
import com.boxpay.checkout.sdk.composeScreens.model.interFontFamily
import com.boxpay.checkout.sdk.composeScreens.screen.RecommendedScreen
import com.boxpay.checkout.sdk.databinding.FragmentMainBottomSheetBinding
import com.boxpay.checkout.sdk.dataclasses.FetchPaymentMethodPostOffer
import com.boxpay.checkout.sdk.dataclasses.SavedCard
import com.boxpay.checkout.sdk.dataclasses.SavedRecommended
import com.boxpay.checkout.sdk.dataclasses.SubscriptionDetails
import com.boxpay.checkout.sdk.constants.AnalyticsEvents
import com.boxpay.checkout.sdk.dataclasses.GetInstantOffersResponse
import com.boxpay.checkout.sdk.interfaces.UpdateMainBottomSheetInterface
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.boxpay.checkout.sdk.utils.callUIAnalytics
import com.boxpay.checkout.sdk.utils.generateRandomAlphanumericString
import com.boxpay.checkout.sdk.utils.getDOBAndPanEffectiveEntry
import com.boxpay.checkout.sdk.utils.getSessionApiUrl
import com.boxpay.checkout.sdk.utils.getSessionToken
import com.boxpay.checkout.sdk.utils.getShopperToken
import com.boxpay.checkout.sdk.utils.getValueAtIndexByKey
import com.boxpay.checkout.sdk.utils.showIf
import com.boxpay.checkout.sdk.utils.showWebOrTimerScreen
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.crossplatform.android.UPIAppDetectorAndroid
import com.crossplatform.sdk.UPIService
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.TimeZone


internal class MainBottomSheet : BottomSheetDialogFragment(), UpdateMainBottomSheetInterface {
    private var transactionId: String? = null
    private var isSuccessful = false
    private var qrCodeShown = false
    private var overlayViewMainBottomSheet: View? = null
    private val mContext: Context get() = requireContext()
    private lateinit var binding: FragmentMainBottomSheetBinding
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private val overlayViewModel: OverlayViewModel by activityViewModels()
    private var overlayViewCurrentBottomSheet: View? = null
    private var token: String? = null
    private var customerShopperToken: String? = null
    private var recommendedInstrumentationList = mutableListOf<SavedRecommended>()
    private var savedCardsInstrumentationList = mutableListOf<SavedCard>()
    private var savedUpiInstrumentationList = mutableListOf<SavedRecommended>()
    private var uniqueReference: String? = null

    private var offerType : String? = null
    private var offerMinAmount : Int? = null
    private var offerCurrencyCode : String? = null
    private var offerMaxAmount : Int? = null
    private lateinit var instantOfferViewModel  : InstantOfferViewModel
    private var successScreenFullReferencePath: String? = null
    private var job: Job? = null
    private var isTablet = false
    private var showName = false
    private var labelType: String? = null
    private var labelName: String? = null
    private var recommendedCheckedPosition: Int? = null
    private var savedUpiCheckedPosition : Int? = null
    private var savedCardsCheckedPosition :Int? = null
    private var showEmail = false
    private var moreOptionsClicked: Boolean? = null
    private var isPANEditable = true
    private var isDOBEditable = true
    private var showPAN = false
    private var showDOB = false
    private var railyatriAmount: String? = null
    private var showShipping = false
    private var showPhone = false
    var upiOptionsShown = false
    private var toLoadQrDirect: Boolean? = null
    private var priceBreakUpVisible = false
    private var upiAvailable = false
    private var upiCollectMethod = false
    private var upiIntentMethod = false
    private var upiQRMethod = false
    private var upiOtmAvailable = false
    private var upiOtmQRMethod = false
    private var upiOtmIntentMethod = false
    private var upiOtmCollectMethod = false
    private var cardsMethod = false
    private var isNameEditable = true
    private var isPhoneEditable = true
    private var isEmailEditable = true
    private var walletMethods = false
    private var emiMethod = false
    private var bnplMethod = false
    private var netBankingMethods = false
    private var overLayPresent = false
    private var items = mutableListOf<String>()
    private var itemQty = mutableListOf<String>()
    private var imagesUrls = mutableListOf<String>()
    private var prices = mutableListOf<String>()
    var queue: RequestQueue? = null
    var countdownTimer: CountDownTimer? = null
    var sessionTimer: CountDownTimer? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    var isGpayReturned = false
    var isOthersReturned = false
    var isPhonePe = false
    var isPaytmReturned = false
    private var shippingEnabled: Boolean = false
    private var dismissThroughAnotherBottomSheet: Boolean = false
    private lateinit var bottomSheet: DeliveryAddressBottomSheet
    private var firstLoad: Boolean = true
    private var productSummary: String? = null
    private var orderDetails: String? = null
    private var installedApps : List<String> = emptyList()
    private var instantOffersList : List<GetInstantOffersResponse> = emptyList()
    private var selectedCouponCode : String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroyView() {
        removeOverlayFromActivity()
        super.onDestroyView()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        removeOverlayFromActivity()
        sessionTimer?.cancel()
        dismiss()
    }

    override fun onStart() {
        super.onStart()
        if (firstLoad) {
            sharedPreferences =
                requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
            queue = Volley.newRequestQueue(mContext)
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
                val upiDetector = UPIAppDetectorAndroid(mContext)
                val upiService = UPIService(upiDetector)
                installedApps = upiService.getAvailableApps()
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

    private fun showLoadingState() {
        if (!binding.loadingRelativeLayout.isVisible) {
            binding.boxpayLogoLottie.apply {
                playAnimation()
                repeatCount = LottieDrawable.INFINITE // This makes the animation repeat infinitely
            }
            binding.loadingRelativeLayout.visibility = View.VISIBLE
            binding.recommendedProceedButton.visibility = View.GONE
        }
    }


    private fun removeLoadingState() {
        binding.loadingRelativeLayout.visibility = View.GONE
        binding.boxpayLogoLottie.cancelAnimation()
        if(binding.recommendedCardView.isVisible) {
            binding.recommendedProceedButton.visibility = View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        showLoadingState()
        initiateFetchStatusCall()
        if (requestCode == 121) {
            isGpayReturned = true
        } else if (requestCode == 122) {
            isPaytmReturned = true
        } else if (requestCode == 123) {
            isPhonePe = true
        } else {
            isOthersReturned = true
        }
    }

    private fun launchUPIIntent(url: String) {

        try {

            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.parse(url)
            intent.data = uri

            val resultCode = when {
                url.startsWith("tez") -> 121
                url.startsWith("paytm") -> 122
                else -> 123
            }

            initiateFetchStatusCall()
            startActivityForResult(intent, resultCode)

        } catch (e: Exception) {
            val eventName = if (e is ActivityNotFoundException) {
                AnalyticsEvents.UPI_APP_NOT_FOUND
            } else {
                AnalyticsEvents.FAILED_TO_LAUNCH_UPI_INTENT
            }

            callUIAnalytics(
                context = mContext,
                message = e.message ?: "",
                screenName = "Main Bottom Sheet in function launchUpiIntent",
                uiEvent = eventName
            )

            PaymentFailureScreen(errorMessage = "Please retry using other payment method or try again in sometime").show(
                parentFragmentManager,
                "FailureScreen"
            )
            removeLoadingState()
        }

    }


    private fun initiateFetchStatusCall() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(3000)
                fetchStatusAndReason("${getSessionApiUrl(mContext)}${token}/status")
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
                    if (status.equals("Pending", ignoreCase = true) && isGpayReturned) {
                        if (isAdded && isResumed && !isStateSaved) {
                            removeLoadingState()
                            job?.cancel()
                            isGpayReturned = false
                            editor.putString("status", "Failed")
                            editor.apply()
                            PaymentFailureScreen(
                                errorMessage = "Payment failed with GPay. Please retry payment with a different UPI app"
                            ).show(parentFragmentManager, "FailureScreen")
                        }
                    }
                    if (status.equals("Pending", ignoreCase = true) && isPhonePe) {
                        if (isAdded && isResumed && !isStateSaved) {
                            removeLoadingState()
                            job?.cancel()
                            isPhonePe = false
                            editor.putString("status", "Failed")
                            editor.apply()
                            PaymentFailureScreen(
                                errorMessage = "Payment failed with PhonePe. Please retry payment with a different UPI app"
                            ).show(parentFragmentManager, "FailureScreen")
                        }
                    }
                    if (status.equals("Pending", ignoreCase = true) && isOthersReturned) {
                        if (isAdded && isResumed && !isStateSaved) {
                            removeLoadingState()
                            job?.cancel()
                            isOthersReturned = false
                            editor.putString("status", "Failed")
                            editor.apply()
                            PaymentFailureScreen(
                                errorMessage = "Please retry using other payment method or try again in sometime"
                            ).show(parentFragmentManager, "FailureScreen")
                        }
                    }
                    if (status.equals("Pending", ignoreCase = true) && isPaytmReturned) {
                        if (isAdded && isResumed && !isStateSaved) {
                            removeLoadingState()
                            job?.cancel()
                            isPaytmReturned = false
                            editor.putString("status", "Failed")
                            editor.apply()
                            PaymentFailureScreen(
                                errorMessage = "Payment failed with Paytm. Please retry payment with a different UPI app"
                            ).show(parentFragmentManager, "FailureScreen")
                        }
                    }

                    if (status.equals("Rejected", ignoreCase = true) || status.equals(
                            "failed",
                            true
                        )
                    ) {
                        editor.putString("status", "Failed")
                        editor.apply()
                        if (isAdded && isResumed && !isStateSaved) {
                            job?.cancel()
                            var cleanedMessage = reason.substringAfter(":")
                            if (!reasonCode.startsWith("uf", true)) {
                                cleanedMessage =
                                    "Please retry using other payment method or try again in sometime"
                            }
                            PaymentFailureScreen(
                                function = {
                                    if (qrCodeShown) {
                                        countdownTimer?.cancel()
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

                            if (isAdded && isResumed && !isStateSaved) {
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
            Response.ErrorListener { error ->
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    val errorMessage = extractMessageFromErrorResponse(errorResponse)

                    if (errorMessage?.contains("expired", true) == true) {
                        SessionExpireScreen().show(parentFragmentManager, "SessionScreen")
                    } else {
                        PaymentFailureScreen(
                            errorMessage = "Please retry using other payment method or try again in sometime"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                }
                job?.cancel()
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

    @SuppressLint("NewApi")
    private fun getUrlForUPIIntent(appName: String) {
        showLoadingState()
        logMainBottomSheetUiEvents()
        callUIAnalytics(
            context = mContext,
            message = "",
            screenName = "Main Bottom Sheet in function getUrlForUpiIntent",
            uiEvent = AnalyticsEvents.PAYMENT_INITIATED
        )
        val requestQueue = Volley.newRequestQueue(mContext)
        val requestBody = JSONObject().apply {
            val browserData = JSONObject().apply {
                val userAgentHeader = WebSettings.getDefaultUserAgent(mContext)
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
                put("type", if(upiOtmAvailable) "upiotm/intent" else "upi/intent")

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
                getDOBAndPanEffectiveEntry(sharedPreferences).forEach { (key, value) ->
                    put(key, value)
                }

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

            val deviceDetails = JSONObject().apply {
                put("browser", Build.BRAND)
                put("platformVersion", Build.VERSION.RELEASE)
                put("deviceType", Build.MANUFACTURER)
                put("deviceName", Build.MANUFACTURER)
                put("deviceBrandName", Build.MODEL)
            }
            put("deviceDetails", deviceDetails)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, getSessionApiUrl(mContext) + token, requestBody,
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
                            cleanedMessage =
                                "Please retry using other payment method or try again in sometime"
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
                    callUIAnalytics(
                        context = mContext,
                        message = e.message ?: "",
                        screenName = "Main Bottom Sheet in function getUrlForUpiIntent in catch block",
                        uiEvent = AnalyticsEvents.ERROR_GETTING_UPI_URL
                    )
                    PaymentFailureScreen().show(parentFragmentManager, "FailureScreenFromUPIIntent")
                }
            },
            Response.ErrorListener { error ->

                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    val errorMessage = extractMessageFromErrorResponse(errorResponse)
                    callUIAnalytics(
                        context = mContext,
                        message = error.message ?: "",
                        screenName = "Main Bottom Sheet in function getAllInstalledApps in error response ",
                        uiEvent = AnalyticsEvents.ERROR_GETTING_UPI_URL
                    )

                    if (errorMessage?.contains("expired", true) == true) {
                        SessionExpireScreen().show(parentFragmentManager, "SessionScreen")
                    } else {
                        PaymentFailureScreen(
                            errorMessage = "Please retry using other payment method or try again in sometime"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                }
                removeLoadingState()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
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
        requestQueue.add(jsonObjectRequest)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        return try {
            binding = FragmentMainBottomSheetBinding.inflate(inflater, container, false)
            showLoadingState()
            instantOfferViewModel = InstantOfferViewModel(mContext)

            val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            binding.root.post {
                imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
            }
            queue = Volley.newRequestQueue(mContext)
            editor = sharedPreferences.edit()


            val userAgentHeader = WebSettings.getDefaultUserAgent(mContext)

            if (userAgentHeader.contains("Mobile", ignoreCase = true)) {
                isTablet = false

                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                isTablet = true
            }

            val callback = SingletonClassForLoadingState.getInstance().getYourObject()

            callback?.onBottomSheetOpened?.invoke()

            fetchTransactionDetailsFromSharedPreferences()
            overlayViewModel.showOverlay.observe(this, Observer { showOverlay ->
                if (showOverlay) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            addOverlayToActivity()
                        }
                    }
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
                OrderSummaryItemsAdapter(imagesUrls, items, prices, itemQty, mContext)
            binding.itemsInOrderRecyclerView.layoutManager = LinearLayoutManager(mContext)
            binding.itemsInOrderRecyclerView.adapter = orderSummaryAdapter

            val recommendedInstrumentsAdapter = RecommendedItemsAdapter(
                recommendedInstrumentationList, mContext
            )
            binding.recomendedRecyclerView.layoutManager = LinearLayoutManager(mContext)
            binding.recomendedRecyclerView.adapter = recommendedInstrumentsAdapter

            val savedCardsInstrumentAdaptor = SavedCardsItemsAdaptor(
                savedCardsInstrumentationList,mContext
            )
            binding.savedCardsRecyclerView.layoutManager = LinearLayoutManager(mContext)
            binding.savedCardsRecyclerView.adapter = savedCardsInstrumentAdaptor

            val savedUpiInstrumentAdaptor = SavedUpiItemsAdaptor(
                savedUpiInstrumentationList, mContext
            )
            binding.savedUpiRecyclerView.layoutManager = LinearLayoutManager(mContext)
            binding.savedUpiRecyclerView.adapter = savedUpiInstrumentAdaptor

            binding.orderSummaryConstraintLayout.setOnClickListener { // Toggle visibility of the price break-up card
                if (!binding.loadingRelativeLayout.isVisible) {
                    if (!priceBreakUpVisible) {
                        showPriceBreakUp()
                        priceBreakUpVisible = true
                    } else {
                        hidePriceBreakUp()
                        priceBreakUpVisible = false
                    }
                }
            }

            binding.recommendedLinearLayout.setOnClickListener {
                if (!binding.loadingRelativeLayout.isVisible) {
                    upiOptionsShown = false
                    hideUPIOptions()
                    savedCardsInstrumentAdaptor.checkPositionLiveData.value = RecyclerView.NO_POSITION
                    savedUpiInstrumentAdaptor.checkedPosition = RecyclerView.NO_POSITION
                    hideSavedCardOptions()
                    if (binding.recomendedOptionsLinearLayout.isVisible) {
                        hideRecommendedOptions()
                    } else {
                        showRecommendedOptions()
                    }
                }
            }

            recommendedInstrumentsAdapter.checkPositionLiveData.observe(viewLifecycleOwner) { checkedPositon ->
                if (!binding.loadingRelativeLayout.isVisible) {
                    recommendedCheckedPosition = checkedPositon
                    if (recommendedCheckedPosition != null && recommendedCheckedPosition != RecyclerView.NO_POSITION) {
                        logMainBottomSheetUiEvents()
                        enableRecommendedProceedButton()
                        binding.recommendedProceedButton.isEnabled = true
                    }
                }
            }

            savedCardsInstrumentAdaptor.checkPositionLiveData.observe(viewLifecycleOwner) { checkedPositon ->
                if (!binding.loadingRelativeLayout.isVisible) {
                    savedCardsCheckedPosition = checkedPositon
                    if (savedCardsCheckedPosition != null && savedCardsCheckedPosition != RecyclerView.NO_POSITION) {
                        logMainBottomSheetUiEvents()
                        enableRecommendedProceedButton()
                        binding.recommendedProceedButton.isEnabled = true
                    }
                }
            }

            savedUpiInstrumentAdaptor.checkPositionLiveData.observe(viewLifecycleOwner) { checkedPositon ->
                if (!binding.loadingRelativeLayout.isVisible) {
                    savedUpiCheckedPosition = checkedPositon
                    if (savedUpiCheckedPosition != null && savedUpiCheckedPosition != RecyclerView.NO_POSITION) {
                        logMainBottomSheetUiEvents()
                        enableRecommendedProceedButton()
                        binding.recommendedProceedButton.isEnabled = true
                    }
                }
            }

            binding.recommendedProceedButton.setOnClickListener {
                if (!binding.loadingRelativeLayout.isVisible) {
                    callUIAnalytics(
                        context = mContext,
                        message = "",
                        screenName = "Main Bottom Sheet in function click listener on recommendedproceedbutton",
                        uiEvent = AnalyticsEvents.PAYMENT_INITIATED
                    )
                    binding.recommendedProceedButton.visibility = View.GONE
                    if (binding.recomendedRecyclerView.isVisible) {
                        recommendedCheckedPosition = recommendedCheckedPosition ?: 0
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            postRecommendedInstruments(
                                if(recommendedInstrumentationList[recommendedCheckedPosition!!].type.equals("upi", true))"upi/collect" else "card/token",
                                recommendedInstrumentationList[recommendedCheckedPosition!!].instrumentationRef ?: "",
                                recommendedInstrumentationList[recommendedCheckedPosition!!].displayValue ?: ""
                            )
                        }
                    } else if (binding.savedUpiRecyclerView.isVisible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            postRecommendedInstruments(
                                "upi/collect",
                                savedUpiInstrumentationList[savedUpiCheckedPosition!!].instrumentationRef ?: "",
                                savedUpiInstrumentationList[savedUpiCheckedPosition!!].displayValue ?: ""
                            )
                        }
                    }else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            postRecommendedInstruments(
                                "card/token",
                                savedCardsInstrumentationList[savedCardsCheckedPosition!!].instrumentationRef ?: ""
                            )
                        }
                    }
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
            binding.upiLinearLayout.setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    recommendedInstrumentsAdapter.checkedPosition = RecyclerView.NO_POSITION
                    hideRecommendedOptions()
                    savedCardsInstrumentAdaptor.checkPositionLiveData.value = RecyclerView.NO_POSITION
                    hideSavedCardOptions()
                    if (!upiOptionsShown) {
                        upiOptionsShown = true
                        showUPIOptions()
                    } else {
                        upiOptionsShown = false
                        hideUPIOptions()
                    }
                }
            }

            binding.addNewUPIIDConstraint.setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    binding.addNewUPIIDConstraint.isEnabled = false
                    logMainBottomSheetUiEvents()
                    job?.cancel()
                    hideQRCode()
                    openAddUPIIDBottomSheet()
                }
            }

            binding.UPIQRConstraint.setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    if (qrCodeShown) {
                        qrCodeShown = false
                        binding.UPIQRConstraint.isEnabled = true
                        hideQRCode()
                    } else {
                        qrCodeShown = true
                        binding.textView21.visibility = View.GONE
                        binding.imageView10.visibility = View.GONE
                        showQRCode()
                    }
                }
            }

            binding.qrCodeOpenConstraint.setOnClickListener() {
                // for the sake that it does not open or closes the options
            }

            binding.addNewCardLinearLayout.setOnClickListener  {
                if (!binding.loadingRelativeLayout.isVisible) {
                    binding.cardConstraint.isEnabled = false
                    callUIAnalytics(mContext, "PAYMENT_CATEGORY_SELECTED", "", "Card")
                    callUIAnalytics(mContext, "PAYMENT_METHOD_SELECTED", "", "Card")
                    hideQRCode()
                    openAddCardBottomSheet()
                }
            }

            binding.cardConstraint.setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    recommendedInstrumentsAdapter.checkedPosition = RecyclerView.NO_POSITION
                    savedUpiInstrumentAdaptor.checkedPosition = RecyclerView.NO_POSITION
                    hideRecommendedOptions()
                    hideQRCode()
                    upiOptionsShown = false
                    hideUPIOptions()
                    if (savedCardsInstrumentationList.isEmpty()) {
                        binding.cardConstraint.isEnabled = false
                        logMainBottomSheetUiEvents()
                        openAddCardBottomSheet()
                    } else if(binding.savedCardsLinearLayout.isVisible) {
                        hideCardOptions()
                    } else {
                        showCardOptions()
                    }
                }
            }


            binding.walletConstraint.setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    recommendedInstrumentsAdapter.checkedPosition = RecyclerView.NO_POSITION
                    hideRecommendedOptions()
                    hideQRCode()
                    savedCardsInstrumentAdaptor.checkPositionLiveData.value = RecyclerView.NO_POSITION
                    savedUpiInstrumentAdaptor.checkedPosition = RecyclerView.NO_POSITION
                    hideSavedCardOptions()
                    binding.walletConstraint.isEnabled = false
                    logMainBottomSheetUiEvents()
                    openWalletBottomSheet()
                }
            }

            binding.emiConstraint.setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    recommendedInstrumentsAdapter.checkedPosition = RecyclerView.NO_POSITION
                    hideRecommendedOptions()
                    hideQRCode()
                    savedCardsInstrumentAdaptor.checkPositionLiveData.value = RecyclerView.NO_POSITION
                    savedUpiInstrumentAdaptor.checkedPosition = RecyclerView.NO_POSITION
                    hideSavedCardOptions()
                    binding.emiConstraint.isEnabled = false
                    logMainBottomSheetUiEvents()
                    openEmiBottomSheet()
                }
            }

            binding.bnplConstraint.setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    recommendedInstrumentsAdapter.checkedPosition = RecyclerView.NO_POSITION
                    hideRecommendedOptions()
                    hideQRCode()
                    savedCardsInstrumentAdaptor.checkPositionLiveData.value = RecyclerView.NO_POSITION
                    savedUpiInstrumentAdaptor.checkedPosition = RecyclerView.NO_POSITION
                    hideSavedCardOptions()
                    binding.bnplConstraint.isEnabled = false
                    logMainBottomSheetUiEvents()
                    openBNPLBottomSheet()
                }
            }


            binding.netBankingConstraint.setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    recommendedInstrumentsAdapter.checkedPosition = RecyclerView.NO_POSITION
                    savedUpiInstrumentAdaptor.checkedPosition = RecyclerView.NO_POSITION
                    hideRecommendedOptions()
                    hideQRCode()
                    savedCardsInstrumentAdaptor.checkPositionLiveData.value = RecyclerView.NO_POSITION
                    hideSavedCardOptions()
                    binding.netBankingConstraint.isEnabled = false
                    logMainBottomSheetUiEvents()
                    openNetBankingBottomSheet()
                }
            }

            binding.refreshButton.setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    showQRCode()
                }
            }

            binding.deliveryAddressConstraintLayout.setOnClickListener() {
                openSavedOrAddOrEditAddressScreen()
            }

            binding.addAddressButton.setOnClickListener() {
                openSavedOrAddOrEditAddressScreen()
            }

            binding.root
        } catch (e: Exception) {
            callUiAnalyticWithSdkCrashEvent(e.message ?: "")
            null
        }
    }

    private fun enableRecommendedProceedButton() {
        binding.recommendedProceedButton.visibility = View.VISIBLE
        binding.recommendedProceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.recommendedProceedButtonRelativeLayout.setBackgroundColor(
            Color.parseColor(
                sharedPreferences.getString(
                    "primaryButtonColor",
                    "#000000"
                )
            )
        )
        binding.proceedtext.setTextColor(
            Color.parseColor(
                sharedPreferences.getString(
                    "buttonTextColor",
                    "#ffffff"
                )
            )
        )
    }

    fun dismissMainSheet() {
        dismissThroughAnotherBottomSheet = true
        try {
            if (parentFragmentManager == null) {
                dismiss()
                return
            } else {
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    val cardBottomSheet =
                        parentFragmentManager.findFragmentByTag("AddCardBottomSheet") as? AddCardBottomSheet
                    if (cardBottomSheet?.isResumed == true) {
                        cardBottomSheet?.dismissCurrentBottomSheet()
                    }
                    val savedAddressBottomSheet =
                        parentFragmentManager.findFragmentByTag("SavedAddressBottomSheet") as SavedAddressBottomSheet?
                    savedAddressBottomSheet?.dismissCurrentBottomSheet()
                    val deliveryAddressBottomSheet =
                        parentFragmentManager.findFragmentByTag("DeliveryAddressBottomSheet") as DeliveryAddressBottomSheet?
                    deliveryAddressBottomSheet?.dismissCurrentBottomSheet()
                    val addUPIID =
                        parentFragmentManager.findFragmentByTag("AddUPIBottomSheet") as? AddUPIID
                    addUPIID?.dismissCurrentBottomSheet()
                    val walletBottomSheet =
                        parentFragmentManager.findFragmentByTag("WalletBottomSheet") as? WalletBottomSheet
                    walletBottomSheet?.dismissCurrentBottomSheet()
                    val netBankingBottomSheet =
                        parentFragmentManager.findFragmentByTag("NetBankingBottomSheet") as? NetBankingBottomSheet
                    netBankingBottomSheet?.dismissCurrentBottomSheet()
                    val emiBottomSheet =
                        parentFragmentManager.findFragmentByTag("EmiBottomSheet") as? EmiBottomSheet
                    emiBottomSheet?.dismissFunction()
                    sessionTimer?.cancel()

                    dismiss()
                }, 500)
            }
        } catch (e: Exception) {
            callUiAnalyticWithSdkCrashEvent(e.message ?: "")
        }
    }

    private fun showQRCode() {
        qrCodeShown = true
        binding.qrCodeOpenConstraint.visibility = View.VISIBLE
        showLoadingState()
        binding.refreshButton.visibility = View.GONE
        fetchQRCode()
    }

    private fun fetchQRCode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            postRequestForQRCode()
        }
    }

    private fun hideQRCode() {
        qrCodeShown = false
        countdownTimer?.cancel()
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
                callUIAnalytics(
                    context = mContext,
                    message = "",
                    screenName = "Main Bottom Sheet - QR timer finished",
                    uiEvent = AnalyticsEvents.PAYMENT_RESULT_SCREEN_DISPLAYED
                )
                blurImageView()
            }
        }
        countdownTimer?.start()
    }

    private fun blurImageView() {
        // Get the current Bitmap from the ImageView
        val bitmap = (binding.qrCodeImageView.drawable as BitmapDrawable).bitmap

        // Apply blur transformation using Glide and BlurTransformation
        Glide.with(mContext)
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

    private fun callPaymentMethodRules() {
        showLoadingState()
        val requestQueue = Volley.newRequestQueue(mContext)

        val countryName = sharedPreferences.getString("countryCode", null)

        val jsonArrayRequest = object : JsonArrayRequest(
            Method.GET,
            "${getSessionApiUrl(mContext)}$token/payment-methods?customerCountryCode=$countryName",
            null,
            Response.Listener { response ->

                val gson = Gson()
                val type = object : TypeToken<List<FetchPaymentMethodPostOffer>>() {}.type
                val paymentMethodsList: List<FetchPaymentMethodPostOffer> =
                    gson.fromJson(response.toString(), type)
                showPaymentMethods(paymentMethodsList)
                updateView()
                removeLoadingState()
            },
            Response.ErrorListener { /* no response handling */error ->
                removeLoadingState()
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    val errorMessage = extractMessageFromErrorResponse(errorResponse)

                    if (errorMessage?.contains("expired", true) == true) {
                        SessionExpireScreen().show(parentFragmentManager, "SessionScreen")
                    } else {
                        PaymentFailureScreen(
                            errorMessage = "Please retry using other payment method or try again in sometime"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                }
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


    @RequiresApi(Build.VERSION_CODES.O)
    private fun postRequestForQRCode() {

        val requestQueue = Volley.newRequestQueue(mContext)
        logMainBottomSheetUiEvents()
        callUIAnalytics(
            context = mContext,
            message = "",
            screenName = "Main Bottom Sheet in function postRequestForQRCode",
            uiEvent = AnalyticsEvents.PAYMENT_INITIATED
        )


        // Constructing the request body
        val requestBody = JSONObject().apply {


            // Create the browserData JSON object
            val browserData = JSONObject().apply {
                val userAgentHeader = WebSettings.getDefaultUserAgent(mContext)
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

            val instrumentDetailsObject = JSONObject().apply {
                put("type", if(upiOtmAvailable) "upiotm/qr" else "upi/qr")
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
                getDOBAndPanEffectiveEntry(sharedPreferences).forEach { (key, value) ->
                    put(key, value)
                }
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

            val deviceDetails = JSONObject().apply {
                put("browser", Build.BRAND)
                put("platformVersion", Build.VERSION.RELEASE)
                put("deviceType", Build.MANUFACTURER)
                put("deviceName", Build.MANUFACTURER)
                put("deviceBrandName", Build.MODEL)
            }
            put("deviceDetails", deviceDetails)
        }

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, getSessionApiUrl(mContext) + token, requestBody,
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
                initiateFetchStatusCall()
            },
            Response.ErrorListener { /* no response handling */error ->
                removeLoadingState()
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    val errorMessage = extractMessageFromErrorResponse(errorResponse)

                    if (errorMessage?.contains("expired", true) == true) {
                        SessionExpireScreen().show(parentFragmentManager, "SessionScreen")
                    } else {
                        PaymentFailureScreen(
                            errorMessage = "Please retry using other payment method or try again in sometime"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                }
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
                headers["X-Client-Connector-Name"] = "Android SDK"
                headers["X-Client-Connector-Version"] = BuildConfig.SDK_VERSION
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

    fun getRecommendedInstrumentation() {
        val requestQueue = Volley.newRequestQueue(mContext)
        uniqueReference = sharedPreferences.getString("uniqueReference", null)
        val jsonObjectRequest = object : JsonArrayRequest(
            Method.GET,
            getSessionApiUrl(mContext) + token + "/shoppers/$uniqueReference/recommended-instruments",
            null,
            Response.Listener { response ->
                try {
                    if (response != emptyArray<Objects>()) {
                        (0 until response.length()).map { index ->
                            val instrumentType = getValueAtIndexByKey(response, "type", index)
                            val instrumentationRef =
                                getValueAtIndexByKey(response, "instrumentRef", index)
                            val displayValue =
                                getValueAtIndexByKey(response, "displayValue", index)
                            val logoUrl = getValueAtIndexByKey(response, "logoUrl", index)
                            if (recommendedInstrumentationList.size < 2) {
                                recommendedInstrumentationList.add(
                                    SavedRecommended(
                                        instrumentationRef = instrumentationRef,
                                        displayValue = displayValue,
                                        logoUrl = logoUrl,
                                        type = instrumentType
                                    )
                                )
                            }
                            if (instrumentType.equals("upi", true)) {
                                savedUpiInstrumentationList.add(
                                    SavedRecommended(
                                        instrumentationRef = instrumentationRef,
                                        displayValue = displayValue,
                                        logoUrl = logoUrl,
                                        type = instrumentType
                                    )
                                )
                            }
                            if (instrumentType.equals("card", true)) {
                                val cardHolderName = getValueAtIndexByKey(response, "cardNickName", index)

                                savedCardsInstrumentationList.add(
                                    SavedCard(
                                    cardHolderName = cardHolderName.takeIf { !it.isNullOrEmpty() && it != "null" },
                                    cardNumber = displayValue,
                                    instrumentationRef = instrumentationRef,
                                    cardIcon = logoUrl
                                ))
                            }
                        }
                        if (recommendedInstrumentationList.isNotEmpty() && binding.upiLinearLayout.isVisible) {
                            binding.recommendedCardView.visibility = View.VISIBLE
                            binding.recommendedProceedButtonRelativeLayout.visibility = View.VISIBLE
                            binding.recommendedProceedButton.visibility = View.VISIBLE
                            binding.recommendedProceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
                            binding.recommendedProceedButtonRelativeLayout.setBackgroundColor(
                                Color.parseColor(
                                    sharedPreferences.getString(
                                        "primaryButtonColor",
                                        "#000000"
                                    )
                                )
                            )
                            binding.proceedtext.setTextColor(
                                Color.parseColor(
                                    sharedPreferences.getString(
                                        "buttonTextColor",
                                        "#ffffff"
                                    )
                                )
                            )
                            binding.recommendedProceedButton.isEnabled = true
                            recommendedCheckedPosition = 0
                            binding.swipeCtaScreen.visibility = View.VISIBLE
                            binding.linearLayoutMain.visibility = View.GONE
                            if (!binding.itemsInOrderRecyclerView.isVisible) {
                                swipeToPayContent()
                            }
                            showRecommendedOptions()
                        } else {
                            upiOptionsShown = true
                            showUPIOptions()
                        }
                        removeLoadingState()
                    }
                } catch (e: JSONException) {
                    removeLoadingState()
                }
            },
            Response.ErrorListener {
                removeLoadingState()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Session $customerShopperToken"
                return headers
            }
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
        binding.emiConstraint.isEnabled = true
        binding.recommendedProceedButton.isEnabled = true
    }

    private fun populatePopularUPIApps() {
        var i = 1
        if (installedApps.contains("phonepe")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setImageResource(R.drawable.phonepe_logo)
            imageView.setBackgroundResource(0)
            textView.text = "PhonePe"
            getPopularConstraintLayoutByNum(i).setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    overlayViewModel.setShowOverlay(false)
                    getUrlForUPIIntent("PhonePe")
                }
            }
            i++
        }

        if (installedApps.contains("gpay")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setImageResource(R.drawable.google_pay_seeklogo)
            imageView.setBackgroundResource(0)
            textView.text = "GPay"

            getPopularConstraintLayoutByNum(i).setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    overlayViewModel.setShowOverlay(false)
                    getUrlForUPIIntent("GPay")
                }
            }
            i++
        }

        if (installedApps.contains("paytm")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setImageResource(R.drawable.paytm_upi_logo)
            imageView.setBackgroundResource(0)
            textView.text = "Paytm"

            getPopularConstraintLayoutByNum(i).setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    overlayViewModel.setShowOverlay(false)
                    getUrlForUPIIntent("PayTm")
                }
            }
            i++
        }

        val imageView = getPopularImageViewByNum(i)
        val textView = getPopularTextViewByNum(i)
        imageView.setImageResource(R.drawable.ic_others)
        textView.text = "Others"

        getPopularConstraintLayoutByNum(i).setOnClickListener() {
            if (!binding.loadingRelativeLayout.isVisible) {
                showLoadingState()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getUrlForDefaultUPIIntent()
                }
            }
        }

        if (i == 1 || i < 1) {
            binding.popularUPIAppsConstraint.visibility = View.GONE
        }
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
        try {
            initiateFetchStatusCall()
            startActivityForResult(intent, 124)
        } catch (_: Exception) {
            removeLoadingState()
            Toast.makeText(mContext, "No other UPI options", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getUrlForDefaultUPIIntent() {
        logMainBottomSheetUiEvents()
        val requestQueue = Volley.newRequestQueue(mContext)
        // Constructing the request body
        val requestBody = JSONObject().apply {
            val browserData = JSONObject().apply {
                val userAgentHeader = WebSettings.getDefaultUserAgent(mContext)
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
                getDOBAndPanEffectiveEntry(sharedPreferences).forEach { (key, value) ->
                    put(key, value)
                }

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

            val deviceDetails = JSONObject().apply {
                put("browser", Build.BRAND)
                put("platformVersion", Build.VERSION.RELEASE)
                put("deviceType", Build.MANUFACTURER)
                put("deviceName", Build.MANUFACTURER)
                put("deviceBrandName", Build.MODEL)
            }
            put("deviceDetails", deviceDetails)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, getSessionApiUrl(mContext) + token, requestBody,
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
                            cleanedMessage =
                                "Please retry using other payment method or try again in sometime"
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
            Response.ErrorListener { /* no response handling */error ->
                removeLoadingState()
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    val errorMessage = extractMessageFromErrorResponse(errorResponse)

                    if (errorMessage?.contains("expired", true) == true) {
                        SessionExpireScreen().show(parentFragmentManager, "SessionScreen")
                    } else {
                        PaymentFailureScreen(
                            errorMessage = "Please retry using other payment method or try again in sometime"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                }
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
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
        requestQueue.add(jsonObjectRequest)
    }

    private fun addOverlayToActivity() {
        overLayPresent = true
        val activityContext = activity ?: return
        if (activityContext.isFinishing || activityContext.isDestroyed) return

        val windowManager = activityContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val decorView = activityContext.window.decorView

        if (overlayViewMainBottomSheet == null) {
            overlayViewMainBottomSheet = View(activityContext).apply {
                setBackgroundColor(Color.parseColor("#80000000"))
            }
        }

        // 2. CHECK TOKEN: If the user just pressed the power button, decorView.windowToken will be null
        val token = decorView.windowToken
        if (token == null) {
            // Window isn't ready yet. Try again in the next frame.
            decorView.post {
                if (isAdded && !isDetached) addOverlayToActivity()
            }
            return
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.token = token
        }

        activityContext.runOnUiThread {
            try {
                // 4. PREVENT MULTIPLE ADDS: Check if it's already on screen
                if (overlayViewMainBottomSheet?.parent == null) {
                    windowManager.addView(overlayViewMainBottomSheet, layoutParams)
                    overLayPresent = true
                }
            } catch (e: Exception) {
                callUIAnalytics(
                    context = mContext,
                    message = e.message ?: "${e.message}",
                    screenName = "Main Bottom Sheet in function addOverlayToActivity",
                    uiEvent = AnalyticsEvents.SDK_CRASH
                )
            }
        }
    }

    private fun removeOverlayFromActivity() {
        val activityContext = activity ?: return
        val view = overlayViewMainBottomSheet ?: return

        try {
            val windowManager = activityContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (view.parent != null) {
                windowManager.removeViewImmediate(view)
            }
        } catch (e: Exception) {
            callUIAnalytics(
                context = mContext,
                message = e.message ?: "${e.message}",
                screenName = "Main Bottom Sheet in function removeOverlayFromActivity",
                uiEvent = AnalyticsEvents.SDK_CRASH
            )
        } finally {
            // Crucial: Nullify the view so we don't hold a reference to a dead context
            overlayViewMainBottomSheet = null
            overLayPresent = false
            sessionTimer?.cancel()
        }
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

    private fun showRecommendedOptions() {
        if (binding.upiOptionsLinearLayout.isVisible) {
            upiOptionsShown = false
            hideUPIOptions()
        }
        binding.recomendedConstraint.setBackgroundColor(Color.parseColor("#E0F1FF"))
        binding.recomendedRecyclerView.visibility = View.VISIBLE
        binding.recomendedOptionsLinearLayout.visibility = View.VISIBLE
        binding.recomendedText.typeface =
            ResourcesCompat.getFont(mContext, R.font.poppins_semibold)
    }

    private fun hideRecommendedOptions() {
        binding.recomendedConstraint.setBackgroundColor(Color.parseColor("#FFFFFF"))
        binding.recomendedRecyclerView.visibility = View.GONE
        binding.recomendedText.typeface = ResourcesCompat.getFont(mContext, R.font.poppins)
        binding.recomendedOptionsLinearLayout.visibility = View.GONE
        recommendedCheckedPosition = null
        binding.recommendedProceedButton.visibility = View.GONE
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
            ResourcesCompat.getFont(mContext, R.font.poppins_semibold)

        if (installedApps.isNotEmpty() && (upiIntentMethod || upiOtmIntentMethod)) {
            binding.popularUPIAppsConstraint.visibility = View.VISIBLE
        }

        if (savedUpiInstrumentationList.isNotEmpty()) {
            binding.savedUpiRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showCardOptions() {
        binding.cardConstraint.setBackgroundColor(Color.parseColor("#E0F1FF"))
        binding.savedCardsLinearLayout.visibility = View.VISIBLE
        binding.textView29.typeface =
            ResourcesCompat.getFont(mContext, R.font.poppins_semibold)
        if (savedCardsCheckedPosition != RecyclerView.NO_POSITION && savedCardsCheckedPosition !=  null){
            binding.recommendedProceedButtonRelativeLayout.visibility = View.VISIBLE
            binding.recommendedProceedButton.visibility = View.VISIBLE
            binding.recommendedProceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
            binding.recommendedProceedButtonRelativeLayout.setBackgroundColor(
                Color.parseColor(
                    sharedPreferences.getString(
                        "primaryButtonColor",
                        "#000000"
                    )
                )
            )
            binding.proceedtext.setTextColor(
                Color.parseColor(
                    sharedPreferences.getString(
                        "buttonTextColor",
                        "#ffffff"
                    )
                )
            )
            binding.recommendedProceedButton.isEnabled = true
        }
    }

    private fun hideCardOptions() {
        binding.cardConstraint.setBackgroundColor(Color.parseColor("#FFFFFF"))
        binding.savedCardsLinearLayout.visibility = View.GONE
        binding.textView29.typeface =
            ResourcesCompat.getFont(mContext, R.font.poppins)
        binding.recommendedCardView.visibility = View.VISIBLE
    }


    private fun hideUPIOptions() {
        binding.upiConstraint.setBackgroundColor(Color.parseColor("#FFFFFF"))
        binding.upiOptionsLinearLayout.visibility = View.GONE
        binding.textView20.typeface = ResourcesCompat.getFont(mContext, R.font.poppins)
        binding.popularUPIAppsConstraint.visibility = View.GONE
        if (savedUpiInstrumentationList.isNotEmpty()) {
            binding.savedUpiRecyclerView.visibility = View.GONE
        }
        binding.imageView12.animate()
            .rotation(0f)
            .setDuration(500) // Set the duration of the animation in milliseconds
            .withEndAction {}
            .start()
        hideQRCode()
    }

    private fun hideSavedCardOptions() {
        binding.cardConstraint.setBackgroundColor(Color.parseColor("#FFFFFF"))
        binding.savedCardsLinearLayout.visibility = View.GONE
        binding.textView29.typeface = ResourcesCompat.getFont(mContext, R.font.poppins)
        savedCardsCheckedPosition = null
        binding.recommendedProceedButton.visibility = View.GONE
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


            val screenHeight = mContext.resources.displayMetrics.heightPixels
            val percentageOfScreenHeight = 0.95 // 70%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

            bottomSheetBehavior?.maxHeight = desiredHeight

            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.isHideable = false


            dialog.setCancelable(!binding.progressBar.isVisible)

            dialog.setCanceledOnTouchOutside(false)

            dialog.setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && binding.progressBar.isVisible) {
                    // Prevent dialog from being dismissed if loader is active
                    true
                } else {
                    // Allow dialog to be dismissed if loader is not active
                    false
                }
            }

            bottomSheetBehavior?.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    // Handle state changes
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            //Hidden
                            dismiss()
                            sessionTimer?.cancel()
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
        val bottomSheetFragment = AddUPIID.newInstance(shippingEnabled, upiOtmAvailable)
        parentFragmentManager.beginTransaction()
            .add(bottomSheetFragment, "AddUPIBottomSheet")
            .commitAllowingStateLoss()
    }

    private fun openAddCardBottomSheet() {
        val bottomSheetFragment =
            AddCardBottomSheet.newInstance(shippingEnabled)
        parentFragmentManager.beginTransaction()
            .add(bottomSheetFragment, "AddCardBottomSheet")
            .commitAllowingStateLoss()
    }

    private fun openNetBankingBottomSheet() {

        val bottomSheetFragment = NetBankingBottomSheet.newInstance(shippingEnabled)
        parentFragmentManager.beginTransaction()
            .add(bottomSheetFragment, "NetBankingBottomSheet")
            .commitAllowingStateLoss()
    }

    private fun openWalletBottomSheet() {

        val bottomSheetFragment = WalletBottomSheet.newInstance(shippingEnabled)
        parentFragmentManager.beginTransaction()
            .add(bottomSheetFragment, "WalletBottomSheet")
            .commitAllowingStateLoss()
    }

    private fun openEmiBottomSheet() {
        val bottomSheetFragment = EmiBottomSheet.newInstance(shippingEnabled)
        parentFragmentManager.beginTransaction()
            .add(bottomSheetFragment, "EmiBottomSheet")
            .commitAllowingStateLoss()
    }

    private fun openInstantOfferList() {
        val bottomSheetFragment = InstantOffersBottomSheet.newInstance(offersList = instantOffersList, onClickCode = {callApplyInstantOfferFunction(it)}, selectedCouponCode = selectedCouponCode, onRemoveCode = {callRemoveInstantOfferFunction()})
        parentFragmentManager.beginTransaction()
            .add(bottomSheetFragment, "InstantOfferBottomSheet")
            .commitAllowingStateLoss()
    }

    private fun openBNPLBottomSheet() {

        val bottomSheetFragment = BNPLBottomSheet.newInstance(shippingEnabled)
        parentFragmentManager.beginTransaction()
            .add(bottomSheetFragment, "BnplBottomSheet")
            .commitAllowingStateLoss()
    }

    private fun makeSessionDataCall() {

        val url = "${getSessionApiUrl(mContext)}${token}"
        val queue: RequestQueue = Volley.newRequestQueue(mContext)
        val jsonObjectAll = object : JsonObjectRequest(Method.GET, url, null, { response ->

            try {
                val status = response.getString("status")
                val transactionId = response.getString("lastTransactionId").toString()
                if (status.equals(
                        "Approved",
                        ignoreCase = true
                    ) || status.equals("paid", true)
                ) {
                    editor.putString("status", "Success")
                    editor.putString("transactionId", transactionId)
                    editor.apply()

                    if (isAdded && isResumed && !isStateSaved) {
                        val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                        bottomSheet.show(
                            parentFragmentManager,
                            "PaymentStatusBottomSheetWithDetails"
                        )
                    }
                }
                if (status.equals(
                        "expired",
                        ignoreCase = true
                    )
                ) {
                    editor.putString("status", "Expired")
                    editor.putString("transactionId", transactionId)
                    editor.apply()

                    if (isAdded && isResumed) {
                        SessionExpireScreen().show(parentFragmentManager, "SessionScreen")
                    }
                }
                val paymentDetailsObject = response.getJSONObject("paymentDetails")

                val totalAmount = paymentDetailsObject.getJSONObject("money").getInt("amount")
                offerMinAmount = totalAmount

                val formattedAmount =
                    paymentDetailsObject.getJSONObject("money").getString("amountLocaleFull")


                var orderObject: JSONObject? = null
                if (!paymentDetailsObject.isNull("order")) {
                    orderObject = paymentDetailsObject.getJSONObject("order")
                }
                if (orderObject == null) {
                    binding.itemsInOrderRecyclerView.visibility = View.GONE
                    binding.priceBreakUpDetailsLinearLayout.visibility = View.GONE
                } else {
                    binding.orderSummaryConstraintLayout.setPadding(0, 16, 0, 16)
                }

                val subscriptionDetails: JSONObject? =
                    paymentDetailsObject.optJSONObject("subscriptionDetails")
                val toShowSubscription =
                    subscriptionDetails != null && subscriptionDetails.optJSONObject("billingCycle")
                        ?.optString("billingTimeUnit")
                        .equals("AsPresented")

                if (toShowSubscription && railyatriAmount != null && railyatriAmount!!.isNotEmpty()) {
                    val amountValue =
                        railyatriAmount!!.toDoubleOrNull() // Convert the string to Double (or Int) safely
                    if (amountValue != null && amountValue >= 15000) {
                        binding.belowTextImage.visibility = View.VISIBLE
                    } else {
                        binding.belowTextImage.visibility = View.GONE
                    }
                } else {
                    binding.belowTextImage.visibility = View.GONE
                }

                @SuppressLint("SetTextI18n")
                if (subscriptionDetails != null && orderObject != null) {
                    val gson = Gson()
                    val subscriptionDetailsJson = subscriptionDetails.toString()
                    val subscriptionDetailsModel =
                        gson.fromJson(subscriptionDetailsJson, SubscriptionDetails::class.java)
                    binding.apply {
                        recurringDuration.text =
                            subscriptionDetailsModel.billingCycle!!.billingTimeUnit
                        recurringNextPay.text =
                            subscriptionDetailsModel.nextBillingDateLocale!!.substring(0, 10)
                        recurringPlanExpiry.text =
                            subscriptionDetailsModel.expiryDateLocale!!.substring(0, 10)
                        recurringTotal.text =
                            "₹" + paymentDetailsObject.getJSONObject("money").getDouble("amount")

                        val sourceString =
                            "· You will be charged ₹" + ("<b>$totalAmount").toString() + "</b> " + " on the next payment date"
                        recurringAmount.text = Html.fromHtml(sourceString)

                        if (orderObject.getString("originalAmount") != "null") {
                            recurringSubTotal.text = "₹" + orderObject.getString("originalAmount")
                        } else {
                            recurringLlSubTotal.visibility = View.GONE
                        }
                        if (orderObject.getString("taxAmount") != "" && orderObject.getString("taxAmount") != "null") {
                            recurringTax.text = "₹" + orderObject.getString("taxAmount")
                        } else {
                            recurringLlTax.visibility = View.GONE
                        }
                        if (orderObject.getString("totalDiscountedAmount") != "" && orderObject.getString(
                                "totalDiscountedAmount"
                            ) != "null"
                        ) {
                            recurringDiscount.text =
                                "-₹" + orderObject.getString("totalDiscountedAmount")
                        } else {
                            recurringLlDiscount.visibility = View.GONE
                        }

                        if (orderObject.getString("shippingAmount") != "" && orderObject.getString("shippingAmount") != "null") {
                            recurringShipping.text = "₹" + orderObject.getString("shippingAmount")
                        } else {
                            recurringLlShipping.visibility = View.GONE
                        }
                        recurringMainCard.visibility = View.VISIBLE
                        binding.arrowIconRecurring.animate()
                            .rotation(180f)
                            .setDuration(50) // Set the duration of the animation in milliseconds
                            .withEndAction {}
                            .start()
                    }
                } else {
                    binding.recurringMainCard.visibility = View.GONE
                }

                binding.arrowIconRecurring.setOnClickListener {
                    if (binding.recurringDetailsLinearLayout.visibility == View.VISIBLE) {
                        binding.recurringDetailsLinearLayout.visibility = View.GONE
                        binding.arrowIconRecurring.animate()
                            .rotation(0f)
                            .setDuration(250)
                            .withEndAction {}
                            .start()

                    } else {
                        binding.recurringDetailsLinearLayout.visibility = View.VISIBLE
                        binding.arrowIconRecurring.animate()
                            .rotation(180f)
                            .setDuration(250)
                            .withEndAction {}
                            .start()
                    }
                }

                val originalAmount = orderObject?.getString("originalAmount")

                val shippingCharges = orderObject?.getString("shippingAmount")


                val taxes = orderObject?.getString("taxAmount")

                val additionalDetails =
                    response.getJSONObject("configs").getJSONArray("additionalFieldSets")

                var orderSummaryEnable = false
                val moneyObject = paymentDetailsObject.getJSONObject("money")

                for (i in 0 until additionalDetails.length()) {
                    if (additionalDetails.get(i) == "ORDER_ITEM_DETAILS") {
                        orderSummaryEnable = true
                    }
                    if (additionalDetails.get(
                            i
                        ).equals("SHIPPING_ADDRESS")
                    ) {
                        showShipping = true
                    }
                }
                val enabledFields = response.getJSONObject("configs").getJSONArray("enabledFields")

                if (enabledFields.length() > 0) {
                    for (i in 0 until enabledFields.length()) {
                        val fieldObject = enabledFields.getJSONObject(i)
                        if (fieldObject.optString("field", "UNKNOWN").contains("phone", true)) {
                            showPhone = true
                            isPhoneEditable =
                                fieldObject.optBoolean("editable", false) || showShipping
                        }
                        if (fieldObject.optString("field", "UNKNOWN").contains("name", true)) {
                            showName = true
                            isNameEditable =
                                fieldObject.optBoolean("editable", false) || showShipping
                        }
                        if (fieldObject.optString("field", "UNKNOWN").contains("email", true)) {
                            showEmail = true
                            isEmailEditable =
                                fieldObject.optBoolean("editable", false) || showShipping
                        }
                    }
                }

                if (showEmail || showShipping || showPhone || showName) {
                    binding.cardView8.visibility = View.VISIBLE
                    binding.deliveryAddressText.visibility = View.VISIBLE
                } else {
                    binding.cardView8.visibility = View.GONE
                    binding.deliveryAddressText.visibility = View.GONE
                }
                binding.nameAndMobileTextViewMain.visibility =
                    if (showName || showPhone || showShipping) View.VISIBLE else View.GONE

                binding.rightArrow.visibility =
                    if (isEmailEditable || isPhoneEditable || isNameEditable || showShipping) {
                        View.VISIBLE
                    } else View.INVISIBLE

                if (orderDetails != null && productSummary != null) {
                    binding.orderSummaryConstraintLayout.visibility = View.GONE
                    binding.scrollCard.visibility = View.VISIBLE
                } else {
                    binding.orderSummaryConstraintLayout.visibility = View.VISIBLE
                    binding.scrollCard.visibility = View.GONE
                }
                productSummary?.let { parseAndRenderProductSummary(it) }

                var currencySymbol = moneyObject.getString("currencySymbol")
                val currencyCode = moneyObject.getString("currencyCode")
                offerCurrencyCode = currencyCode
                if (currencySymbol == "")
                    currencySymbol = "₹"

                var totalQuantity = 0
                editor.putString("currencySymbol", currencySymbol)
                editor.putString("currencyCode", currencyCode)
                editor.apply()

                updateTransactionAmountInSharedPreferences(
                    formattedAmount,
                    currencyCode ?: ""
                )

                val itemsArray =
                    if (orderObject?.optJSONArray("items") != null) orderObject.getJSONArray("items") else null
                var productName: String? = null

                if (itemsArray != null) {
                    for (i in 0 until itemsArray.length()) {
                        val itemObject = itemsArray.getJSONObject(i)

                        items.add(itemObject.getString("itemName"))
                        prices.add(itemObject.getString("amountWithoutTaxLocaleFull"))
                        val quantity = itemObject.getInt("quantity")
                        itemQty.add(quantity.toString())
                        productName = if (productName.isNullOrEmpty()) {
                            "${itemObject.getString("itemName")} X (x${itemObject.getInt("quantity")})"
                        } else {
                            "$productName\n${itemObject.getString("itemName")} X (x${
                                itemObject.getInt(
                                    "quantity"
                                )
                            })"
                        }
                        totalQuantity += quantity
                    }
                }
                editor.putString("orderDetails", productName)
                editor.putInt("orderDetailsLength", itemsArray?.length() ?: 0)
                editor.apply()

                val merchantDetailsObject = response.getJSONObject("merchantDetails")
                val checkoutThemeObject = merchantDetailsObject.getJSONObject("checkoutTheme")

                if (response.has("merchantDetails")) {
                    val merchantDetails = response.getJSONObject("merchantDetails")

                    // Check if "customFields" exists and is not null
                    if (merchantDetails.has("customFields") && !merchantDetails.isNull("customFields")) {
                        val customFields = merchantDetails.getJSONArray("customFields")
                        // Process the customFields array
                        if (customFields.length() > 0) {
                            for (i in 0 until customFields.length()) {
                                val fieldObject = customFields.getJSONObject(i)
                                if (fieldObject.getString("fieldName").contains("PAN", true)) {
                                    showPAN = true
                                }

                                if (fieldObject.getString("fieldName")
                                        .contains("DATE_OF_BIRTH", true)
                                ) {
                                    showDOB = true
                                }
                            }
                        }
                    }
                }
                val sharedPreferences = mContext.getSharedPreferences(
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

                if (totalQuantity == 0) {
                    binding.numberOfItems.visibility = View.GONE
                    binding.unopenedTotalValue.visibility = View.GONE
                    binding.textView18.visibility = View.VISIBLE
                    binding.ItemsPrice.visibility = View.VISIBLE
                } else if (totalQuantity == 1) {
                    binding.unopenedTotalValue.text = "${currencySymbol}${formattedAmount}"
                    binding.numberOfItems.text = "${totalQuantity} item"
                } else {
                    binding.numberOfItems.text = "${totalQuantity} items"
                    binding.unopenedTotalValue.text = "${currencySymbol}${formattedAmount}"
                }
                binding.ItemsPrice.text = "${currencySymbol}${formattedAmount}"

                if (originalAmount != null && originalAmount != "0" && originalAmount != "null") {
                    val originalAmountLocaleFull = paymentDetailsObject.getJSONObject("order")
                        .getString("originalAmountLocaleFull")
                    binding.subtotalTextView.text = "$currencySymbol$originalAmountLocaleFull"
                    binding.subTotalRelativeLayout.visibility = View.VISIBLE
                }

                if (showShipping) {
                    binding.textView6.text = "Continue to Add New Address"
                    binding.proceedButtonRelativeLayout.setBackgroundColor(
                        Color.parseColor(
                            sharedPreferences.getString(
                                "primaryButtonColor",
                                "#000000"
                            )
                        )
                    )
                    binding.textView6.setTextColor(
                        Color.parseColor(
                            sharedPreferences.getString(
                                "buttonTextColor",
                                "#ffffff"
                            )
                        )
                    )
                } else {
                    binding.textView6.text = "Continue to Add Personal Details"
                    binding.proceedButtonRelativeLayout.setBackgroundColor(
                        Color.parseColor(
                            sharedPreferences.getString(
                                "primaryButtonColor",
                                "#000000"
                            )
                        )
                    )
                    binding.textView6.setTextColor(
                        Color.parseColor(
                            sharedPreferences.getString(
                                "buttonTextColor",
                                "#ffffff"
                            )
                        )
                    )
                }

                if (taxes != null && taxes != "null" && taxes != "0") {
                    val taxAmountLocaleFull =
                        paymentDetailsObject.getJSONObject("order").getString("taxAmountLocaleFull")
                    binding.taxTextView.text = "$currencySymbol$taxAmountLocaleFull"
                    binding.taxesRelativeLayout.visibility = View.VISIBLE
                }

                if (shippingCharges != null && shippingCharges != "null" && shippingCharges != "0") {
                    val shippingAmountLocaleFull = paymentDetailsObject.getJSONObject("order")
                        .getString("shippingAmountLocaleFull")
                    binding.shippingChargesTextView.text =
                        "$currencySymbol$shippingAmountLocaleFull"
                    binding.shippingChargesRelativeLayout.visibility = View.VISIBLE
                }

                if ((originalAmount == null || originalAmount == "0" && originalAmount == "null") && (shippingCharges == null || shippingCharges == "null" || shippingCharges == "0") && (taxes == null || taxes == "null" && taxes == "0")) {
                    binding.arrowIcon.visibility = View.GONE
                    binding.orderSummaryConstraintLayout.setOnClickListener(null)
                }

                if (!binding.shippingChargesRelativeLayout.isVisible && !binding.taxesRelativeLayout.isVisible && !binding.subTotalRelativeLayout.isVisible) {
                    binding.blackLine.visibility = View.GONE
                }

                val shopperObject = paymentDetailsObject.getJSONObject("shopper")
                editor.putString("amount", formattedAmount)
                editor.putString("merchantId", response.getString("merchantId"))
                editor.putString(
                    "countryCode",
                    paymentDetailsObject.optJSONObject("context")?.optString("countryCode")
                )
                editor.putString(
                    "legalEntity",
                    paymentDetailsObject.optJSONObject("context")?.optJSONObject("legalEntity")?.optString("code")
                )
                editor.putString("uniqueReference", shopperObject.optString("uniqueReference"))

                if (shopperObject.isNull("deliveryAddress")) {
                    editor.putString("address1", null)
                    editor.putString("address2", null)
                    editor.putString("phoneCode", null)
                    editor.putString("city", null)
                    editor.putString("state", null)
                    editor.putString("postalCode", null)
                    editor.putString("labelType", null)
                    editor.putString("labelName", null)
                } else {
                    editor.putString(
                        "postalCode",
                        shopperObject.optJSONObject("deliveryAddress")?.optString("postalCode")
                    )
                    editor.putString(
                        "state",
                        shopperObject.optJSONObject("deliveryAddress")?.optString("state")
                    )
                    editor.putString(
                        "city",
                        shopperObject.optJSONObject("deliveryAddress")?.optString("city")
                    )
                    labelType =
                        shopperObject.optJSONObject("deliveryAddress")?.optString("labelType")
                    labelName =
                        shopperObject.optJSONObject("deliveryAddress")?.optString("labelName")
                    editor.putString(
                        "labelType",
                        shopperObject.optJSONObject("deliveryAddress")?.optString("labelType")
                    )
                    editor.putString(
                        "labelName",
                        shopperObject.optJSONObject("deliveryAddress")?.optString("labelName")
                    )
                    editor.putString(
                        "address2",
                        shopperObject.optJSONObject("deliveryAddress")?.optString("address2")
                    )
                    editor.putString(
                        "address1",
                        shopperObject.optJSONObject("deliveryAddress")?.optString("address1")
                    )
                }
                if (shopperObject.isNull("firstName")) {
                    editor.putString("firstName", null)
                } else {
                    editor.putString("firstName", shopperObject.getString("firstName"))
                }
                if (shopperObject.isNull("panNumber")) {
                    editor.putString("panNumber", null)
                } else {
                    editor.putString("panNumber", shopperObject.getString("panNumber"))
                }

                if (shopperObject.isNull("dateOfBirth")) {
                    editor.putString("dateOfBirth", null)
                } else {
                    editor.putString("dateOfBirth", shopperObject.getString("dateOfBirth"))
                }

                if (shopperObject.isNull("lastName")) {
                    editor.putString("lastName", null)
                } else {
                    editor.putString("lastName", shopperObject.getString("lastName"))
                }
                if (shopperObject.isNull("email")) {
                    editor.putString("email", null)
                } else {
                    editor.putString("email", shopperObject.getString("email"))
                }
                if (shopperObject.isNull("phoneNumber")) {
                    editor.putString("phoneNumber", null)
                } else {
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

                processShopper(
                    shopperObject,
                    orderDetails,
                    showShipping,
                    showName,
                    showEmail,
                    showPhone,
                    showPAN,
                    showDOB
                )

                if (paymentDetailsObject.isNull("order"))
                    orderSummaryEnable = false

                if (!orderSummaryEnable && !formattedAmount.isNullOrEmpty()) {
                    binding.textView9.text = "Payment Summary"
                }

                editor.apply()

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
                    callUIAnalytics(
                        context = context,
                        message = "$e",
                        screenName = "Main Bottom Sheet in makeSession data call line no 2534",
                        uiEvent = AnalyticsEvents.SDK_CRASH
                    )
                    binding.cardView3.visibility = View.GONE
                }

                binding.nameAndMobileTextViewMain.text =
                    if (showShipping && !shopperObject.isNull("deliveryAddress")) {
                        if (shopperObject.getJSONObject("deliveryAddress")
                                .getString("labelName") != "null" && !shopperObject.getJSONObject("deliveryAddress")
                                .getString("labelName").isNullOrEmpty()
                        ) {
                            "Deliver to ${
                                shopperObject.getJSONObject("deliveryAddress")
                                    .getString("labelName")
                            }"
                        } else {
                            "Deliver to ${
                                shopperObject.getJSONObject("deliveryAddress")
                                    .getString("labelType")
                            }"
                        }
                    } else if (showPhone && showName) {
                        sharedPreferences.getString(
                            "firstName",
                            ""
                        ) + " " + sharedPreferences.getString(
                            "lastName",
                            ""
                        ) + " " + "(${sharedPreferences.getString("phoneNumber", "")})"
                    } else if (showName) {
                        sharedPreferences.getString(
                            "firstName",
                            ""
                        ) + " " + sharedPreferences.getString(
                            "lastName",
                            ""
                        )
                    } else if (showPhone) {
                        "(${sharedPreferences.getString("phoneNumber", "")})"
                    } else {
                        "Deliver to"
                    }
                if (showShipping) {
                    binding.deliveryAddressText.text = "Address"
                    binding.addressTextViewMain.text =
                        if (!sharedPreferences.getString("address2", null).isNullOrEmpty()) {
                            "${sharedPreferences.getString("address1", null)}, " +
                                    "${sharedPreferences.getString("address2", null)}, " +
                                    "${sharedPreferences.getString("city", null)}" +
                                    ", ${sharedPreferences.getString("state", "null")}" +
                                    ", ${sharedPreferences.getString("postalCode", "null")}"
                        } else {
                            "${sharedPreferences.getString("address1", null)}, " +
                                    "${sharedPreferences.getString("city", null)}" +
                                    ", ${sharedPreferences.getString("state", "null")}" +
                                    ", ${sharedPreferences.getString("postalCode", "null")}"
                        }
                } else {
                    binding.deliveryAddressText.text = "Personal Details"
                    binding.homeIcon.setImageDrawable(
                        ContextCompat.getDrawable(
                            mContext,
                            R.drawable.ic_personal_details
                        )
                    )
                    binding.addressTextViewMain.text = sharedPreferences.getString("email", "")
                }
                fetchSurchargeDetails(totalAmount, currencyCode, currencySymbol)
                getInstantOffers(type = offerType ?: "", currencySymbol = currencySymbol)
                val gson = Gson()
                val type = object : TypeToken<List<FetchPaymentMethodPostOffer>>() {}.type
                val paymentMethodsList: List<FetchPaymentMethodPostOffer> =
                    gson.fromJson(paymentMethodsArray.toString(), type)
                showPaymentMethods(paymentMethodsList)
                updateView()
                val expireTiming = response.getString("sessionExpiryTimestamp")
                startCountdown(expireTiming)
            } catch (e: Exception) {
                callUiAnalyticWithSdkCrashEvent(e.message ?: "")
                Toast.makeText(
                    mContext,
                    "Invalid token/selected environment.\nPlease press back button and try again",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, Response.ErrorListener { error ->
            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                val errorResponse = String(error.networkResponse.data)
                val errorMessage = extractMessageFromErrorResponse(errorResponse)
                if (errorMessage?.contains("expired", true) == true) {
                    SessionExpireScreen().show(parentFragmentManager, "SessionScreen")
                } else {
                    Toast.makeText(
                        mContext,
                        "Invalid token/selected environment.\nPlease press back button and try again",
                        Toast.LENGTH_LONG
                    ).show()
                    dismiss()
                }
            }
        }) {
            // no op
        }
        queue.add(jsonObjectAll)
    }

    private fun fetchSurchargeDetails(amount: Int, currencyCode : String, currencySymbol : String) {
        val url = "${getSessionApiUrl(mContext)}${token}/surcharges/evaluate"
        val queue: RequestQueue = Volley.newRequestQueue(mContext)
        val requestBody = JSONObject().apply {


            // Create the browserData JSON object
            val discountedMoney = JSONObject().apply {
                put("amount", amount)
                put("currencyCode", currencyCode)
            }
            put("discountedMoney", discountedMoney)
        }
        val jsonObjectAll = object : JsonObjectRequest(Method.POST, url, requestBody, { response ->

            try {
                val appliedSurcharges = response.optJSONArray("appliedSurcharges")
                var surchargeDetails : List<Pair<String, String>> = emptyList()
                appliedSurcharges?.length()?.let {
                    surchargeDetails = (0 until appliedSurcharges.length()).map { index ->
                        val item = appliedSurcharges.getJSONObject(index)

                        val title = item
                            .getJSONObject("surchargeDetails")
                            .optString("title")

                        val calculatedFee = item
                            .opt("calculatedSurchargeFee")
                            ?.toString() ?: ""

                        title to calculatedFee
                    }
                }
                val finalAmount = response.getJSONObject("finalAmountAfterSurcharge").getDouble("amount")
                val indiaLocale = Locale("en", "IN")
                val formatter = NumberFormat.getNumberInstance(indiaLocale)
                formatter.minimumFractionDigits = 2
                formatter.maximumFractionDigits = 2
                val formattedAmount = formatter.format(finalAmount)
                updateTransactionAmountInSharedPreferences(
                    formattedAmount,
                    currencyCode
                )
                binding.surchargeComposeView.setContent {
                    Column {
                        surchargeDetails.map { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.first,
                                    fontFamily = defaultFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = androidx.compose.ui.graphics.Color(0xFF010102),
                                )
                                Text(
                                    text = item.second,
                                    fontFamily = interFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = androidx.compose.ui.graphics.Color(0xFF010102),
                                )
                            }
                        }
                    }
                }
                binding.priceBreakUpDetailsLinearLayout.visibility = View.VISIBLE
                binding.surchargeComposeView.visibility = View.VISIBLE
                binding.ItemsPrice.text = "$currencySymbol${formattedAmount}"
                binding.blackLine.visibility = View.VISIBLE
                getRecommendedOrRemoveLoading()
            }
            catch (e: Exception) {
                callUIAnalytics(
                    context = mContext,
                    message = "$e",
                    screenName = "Main Bottom Sheet",
                    uiEvent = AnalyticsEvents.SDK_CRASH
                )
                getRecommendedOrRemoveLoading()
            }
        }, Response.ErrorListener { error ->
            callUIAnalytics(
                context = mContext,
                message = "$error",
                screenName = "Main Bottom Sheet",
                uiEvent = AnalyticsEvents.SDK_CRASH
            )
            getRecommendedOrRemoveLoading()
        }) {
            // no op
        }
        queue.add(jsonObjectAll)
    }

    private fun getRecommendedOrRemoveLoading() {
        if (customerShopperToken != null && customerShopperToken != "") {
            getRecommendedInstrumentation()
        } else {
            upiOptionsShown = true
            showUPIOptions()
            if (toLoadQrDirect == false || toLoadQrDirect == null || !upiQRMethod || !upiOtmQRMethod) {
                removeLoadingState()
            } else {
                showQRCode()
            }
        }
    }

    private fun showPaymentMethods(paymentMethodsList : List<FetchPaymentMethodPostOffer>) {
        upiCollectMethod = false
        upiAvailable = false
        upiIntentMethod = false
        upiQRMethod = false
        cardsMethod = false
        walletMethods = false
        emiMethod = false
        upiOtmAvailable = false
        upiOtmQRMethod = false
        upiOtmIntentMethod = false
        upiOtmCollectMethod = false
        upiAvailable = false
        bnplMethod = false
        netBankingMethods = false
        binding.recommendedCardView.visibility = View.GONE

        paymentMethodsList.forEach { paymentMethod ->

            when (paymentMethod.type) {

                "Upi" -> {
                    upiAvailable = true
                    when (paymentMethod.brand) {
                        "UpiCollect" -> upiCollectMethod = true
                        "UpiIntent" -> upiIntentMethod = true
                        "UpiQr" -> {
                            val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
                            if (!userAgentHeader.contains("Mobile", ignoreCase = true)) {
                                upiQRMethod = true
                            }
                        }
                    }
                }

                "UpiOneTimeMandate" -> {
                    upiOtmAvailable = true
                    when (paymentMethod.brand) {
                        "UpiIntentOtm" -> upiOtmIntentMethod = true
                        "UpiQrOtm" -> {
                            val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())
                            if (!userAgentHeader.contains("Mobile", ignoreCase = true)) {
                                upiOtmQRMethod = true
                            }
                        }
                        "UpiCollectOtm" -> upiOtmCollectMethod = true
                    }
                }

                "Card" -> cardsMethod = true
                "Wallet" -> walletMethods = true
                "Emi" -> emiMethod = true
                "BuyNowPayLater" -> bnplMethod = true
                "NetBanking" -> netBankingMethods = true
            }
        }
    }

    private fun updateView() {
        // UPI Section
        binding.cardView4.showIf(upiAvailable || upiOtmAvailable)
        binding.upiLinearLayout.showIf(upiAvailable || upiOtmAvailable)

        binding.textView20.text = if(upiOtmAvailable) "UPI One Time Mandate" else "UPI"

        if (upiAvailable || upiOtmAvailable) {
            binding.addNewUPIIDConstraint.showIf(upiCollectMethod || upiOtmCollectMethod)

            if (upiQRMethod || upiOtmQRMethod) {
                val shouldHideHeader =((!upiIntentMethod && !upiCollectMethod) || (!upiOtmIntentMethod && !upiOtmCollectMethod)) && !cardsMethod && !walletMethods && !netBankingMethods && !bnplMethod && !emiMethod

                if (shouldHideHeader) {
                    binding.textView21.visibility = View.GONE
                    binding.imageView10.visibility = View.GONE
                    if (toLoadQrDirect == true) {
                        showQRCode()
                    }
                }

                binding.UPIQRConstraint.visibility = View.VISIBLE
            } else {
                binding.UPIQRConstraint.visibility = View.GONE
            }
        } else {
            binding.UPIQRConstraint.visibility = View.GONE
        }

// Other payment methods
        binding.cardView5.showIf(cardsMethod)
        binding.cardConstraint.showIf(cardsMethod)

        binding.cardView6.showIf(walletMethods)
        binding.walletConstraint.showIf(walletMethods)

        binding.emiCard.showIf(emiMethod)
        binding.emiConstraint.showIf(emiMethod)

        binding.cardView9.showIf(bnplMethod)
        binding.bnplConstraint.showIf(bnplMethod)

        binding.cardView7.showIf(netBankingMethods)
        binding.netBankingConstraint.showIf(netBankingMethods)

    }

    private fun callApplyInstantOfferFunction(
        selectedCode : String
    ) {
        selectedCouponCode = selectedCode
        showLoadingState()
        instantOfferViewModel.applyInstantOffer(
            currency = offerCurrencyCode ?: "",
            maxAmount = offerMaxAmount ?: 0,
            minAmount = offerMinAmount ?: 0,
            selectedCode = listOf(selectedCode)
        )
    }

    private fun callRemoveInstantOfferFunction() {
        showLoadingState()
        selectedCouponCode = null
        editor.putString("selectedOfferCode", null)
        editor.putString("amount", "$offerMinAmount")
        editor.apply()
        instantOfferViewModel.removeInstantOffer()
        instantOfferViewModel.updatePaymentMethods()
        instantOfferViewModel.isCodeApplied.value = false
        instantOfferViewModel.discountAmount.value = ""
        binding.subTotalRelativeLayout.visibility = View.GONE
        binding.offerAppliedLayout.visibility = View.GONE
        binding.priceBreakUpDetailsLinearLayout.visibility = View.GONE
        binding.ItemsPrice.text = "${sharedPreferences.getString("currencySymbol", "")}${sharedPreferences.getString("amount", "")}"
    }

    private fun getInstantOffers(
        type: String,
        currencySymbol : String
    ) {
        if(type.isNotEmpty()) {
            instantOfferViewModel.getInstantOffer(type, offerCurrencyCode ?: "", offerMinAmount ?: 0, offerMaxAmount ?: 0)
            lifecycleScope.launch {
                instantOfferViewModel.getInstantOfferList.collect { offers ->
                    instantOffersList = offers ?: emptyList()
                    if(!offers.isNullOrEmpty()) {
                        binding.instantOfferCard.visibility = View.VISIBLE
                        binding.offerCardComposeView.setContent {
                            val selectedCoupon = if(selectedCouponCode == null) {
                                offers[0]
                            } else {
                                offers.find { it.code == selectedCouponCode }
                            }
                            ApplyCouponCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                selectedColor = androidx.compose.ui.graphics.Color(
                                    Color.parseColor(
                                        sharedPreferences.getString(
                                            "primaryButtonColor",
                                            "#000000"
                                        )
                                    )
                                ),
                                code = selectedCoupon?.code ?: "",
                                description = selectedCoupon?.description ?: "",
                                onClickApply = { selectedCode ->
                                    callApplyInstantOfferFunction(
                                        selectedCode
                                    )
                                },
                                onClickViewAll = {
                                    openInstantOfferList()
                                },
                                isCodeApplied = instantOfferViewModel.isCodeApplied.value,
                                discountAmount = instantOfferViewModel.discountAmount.value,
                                currencySymbol = currencySymbol,
                                onClickRemove = {
                                    callRemoveInstantOfferFunction()
                                }
                            )
                        }
                    }
                }
            }
            lifecycleScope.launch {
                instantOfferViewModel.appliedInstantOffer.collect { selectedOffer ->
                    if(selectedOffer != null) {
                        editor.putString("selectedOfferCode", selectedOffer.evaluatedOffers?.get(0)?.code ?: "")
                        editor.putString("amount", "${selectedOffer.finalAmount}")
                        editor.apply()
                        instantOfferViewModel.updatePaymentMethods(code = selectedOffer.evaluatedOffers?.get(0)?.code ?: "", maxAmount = "${selectedOffer.finalAmount}")
                        instantOfferViewModel.isCodeApplied.value = true
                        instantOfferViewModel.discountAmount.value = "${selectedOffer.evaluatedOffers?.get(0)?.appliedDiscountAmount}"
                        binding.subTotalRelativeLayout.visibility = View.VISIBLE
                        binding.offerAppliedLayout.visibility = View.VISIBLE
                        binding.priceBreakUpDetailsLinearLayout.visibility = View.VISIBLE
                        binding.subtotalTextView.text = "$currencySymbol${selectedOffer.originalAmount}"
                        binding.offerCodeText.text = "Discount(${selectedOffer.evaluatedOffers?.get(0)?.code} applied)"
                        binding.offerAmountTextView.text = "-$currencySymbol${selectedOffer.evaluatedOffers?.get(0)?.appliedDiscountAmount}"
                        binding.ItemsPrice.text = "$currencySymbol${selectedOffer.finalAmount}"
                        binding.blackLine.visibility = View.VISIBLE
                    }
                }
            }

            lifecycleScope.launch {
                instantOfferViewModel.updatedPaymentMethods.collect { methods ->
                    if(!methods.isNullOrEmpty()) {
                        showPaymentMethods(methods)
                        updateView()
                        removeLoadingState()
                    }
                }
            }
        }
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        token = getSessionToken(mContext)
        customerShopperToken = getShopperToken(mContext)
        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
    }


    private fun updateTransactionAmountInSharedPreferences(
        transactionAmountArgs: String,
        currencyCode: String
    ) {
        val sharedPreferences: SharedPreferences =
            requireActivity().getSharedPreferences("NON_DCC_PREF", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("CURRENCY_TYPE", currencyCode)
        editor.putString(
            "AMOUNT",
            transactionAmountArgs
        )
        editor.apply()
    }

    override fun updateBottomSheet() {
        binding.orderSummaryConstraintLayout.setPadding(0, 16, 0, 16)
        labelName = sharedPreferences.getString("labelName", "")
        labelType = sharedPreferences.getString("labelType", "")
        binding.nameAndMobileTextViewMain.text =
            if (showShipping) {
                if (labelName != "null" && !labelName.isNullOrEmpty()) {
                    "Deliver to $labelName"
                } else {
                    "Deliver to $labelType"
                }
            } else if (showPhone && showName) {
                sharedPreferences.getString(
                    "firstName",
                    ""
                ) + " " + sharedPreferences.getString(
                    "lastName",
                    ""
                ) + " " + "(${sharedPreferences.getString("phoneNumber", "")})"
            } else if (showName) {
                sharedPreferences.getString(
                    "firstName",
                    ""
                ) + " " + sharedPreferences.getString(
                    "lastName",
                    ""
                )
            } else {
                "(${sharedPreferences.getString("phoneNumber", "")})"
            }
        if (showShipping) {
            binding.deliveryAddressText.text = "Address"
            binding.addressTextViewMain.text =
                if (!sharedPreferences.getString("address2", null).isNullOrEmpty()) {
                    "${sharedPreferences.getString("address1", null)}, " +
                            "${sharedPreferences.getString("address2", null)}, " +
                            "${sharedPreferences.getString("city", null)}" +
                            ", ${sharedPreferences.getString("state", "null")}" +
                            ", ${sharedPreferences.getString("postalCode", "null")}"
                } else {
                    "${sharedPreferences.getString("address1", null)}, " +
                            "${sharedPreferences.getString("city", null)}" +
                            ", ${sharedPreferences.getString("state", "null")}" +
                            ", ${sharedPreferences.getString("postalCode", "null")}"
                }
        } else {
            binding.deliveryAddressText.text = "Personal Details"
            binding.homeIcon.setImageDrawable(
                ContextCompat.getDrawable(
                    mContext,
                    R.drawable.ic_personal_details
                )
            )
            binding.addressTextViewMain.text = sharedPreferences.getString("email", "")
        }
        binding.cardView8.visibility = View.VISIBLE

        binding.cardView8.visibility = View.VISIBLE
        binding.deliveryAddressText.visibility = View.VISIBLE
        binding.textView111.text = "Payment Details"
        binding.addAddressButton.visibility = View.GONE
        priceBreakUpVisible = false
        hidePriceBreakUp()
        if (recommendedInstrumentationList.isNotEmpty() && binding.upiLinearLayout.isVisible) {
            binding.recommendedCardView.visibility = View.VISIBLE
            binding.recommendedLinearLayout.visibility = View.VISIBLE
            showRecommendedOptions()
        } else {
            upiOptionsShown = true
            showUPIOptions()
        }

        if (recommendedInstrumentationList.isNotEmpty() && (moreOptionsClicked == null || moreOptionsClicked == false)) {
            binding.swipeCtaScreen.visibility = View.VISIBLE
            binding.linearLayoutMain.visibility = View.GONE
            swipeToPayContent()
        }

        callPaymentMethodRules()
        binding.textView12.visibility = View.VISIBLE

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun postRecommendedInstruments(type: String, instrumentationRef: String, displayName: String? = null) {
        showLoadingState()
        val requestQueue = Volley.newRequestQueue(mContext)


        // Constructing the request body
        val requestBody = JSONObject().apply {


            // Create the browserData JSON object
            val browserData = JSONObject().apply {
                val userAgentHeader = WebSettings.getDefaultUserAgent(mContext)
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

            val instrumentDetailsObject = JSONObject().apply {
                put("type", type)

               if(type.contains("upi", true)) {
                   val upiObject = JSONObject().apply {
                       put("instrumentRef", instrumentationRef)
                   }
                   put("upi", upiObject)
               } else {
                   val savedCardObject = JSONObject().apply {
                       put("instrumentRef", instrumentationRef)
                   }
                   put("savedCard", savedCardObject)
               }
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
                getDOBAndPanEffectiveEntry(sharedPreferences).forEach { (key, value) ->
                    put(key, value)
                }
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

            val deviceDetails = JSONObject().apply {
                put("browser", Build.BRAND)
                put("platformVersion", Build.VERSION.RELEASE)
                put("deviceType", Build.MANUFACTURER)
                put("deviceName", Build.MANUFACTURER)
                put("deviceBrandName", Build.MODEL)
            }
            put("deviceDetails", deviceDetails)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, getSessionApiUrl(mContext) + token, requestBody,
            Response.Listener { response ->

                binding.swipeLoader.visibility = View.GONE
                binding.swipeScreenAnimation.cancelAnimation()
                val status = response.getJSONObject("status").getString("status")
                val reason = response.getJSONObject("status").getString("reason")
                val reasonCode = response.getJSONObject("status").getString("reasonCode")
                transactionId = response.getString("transactionId").toString()
                updateTransactionIDInSharedPreferences(transactionId!!)
                enabledButtonsForAllPaymentMethods()

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
                        showWebOrTimerScreen(this, response, displayName ?: "", {
                            initiateFetchStatusCall()
                        })
                    } else if (status.contains("Approved", ignoreCase = true)) {
                        editor.putString("status", "Success")
                        editor.apply()

                        val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                        bottomSheet.show(
                            parentFragmentManager,
                            "PaymentStatusBottomSheetWithDetails"
                        )
                    }
                }
                removeLoadingState()
            },
            Response.ErrorListener { error ->
                // Handle error
                removeLoadingState()
                binding.swipeLoader.visibility = View.GONE
                binding.swipeScreenAnimation.cancelAnimation()
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    val errorMessage = extractMessageFromErrorResponse(errorResponse)

                    if (errorMessage?.contains("expired", true) == true) {
                        SessionExpireScreen().show(parentFragmentManager, "SessionScreen")
                    } else {
                        PaymentFailureScreen(
                            errorMessage = "Please retry using other payment method or try again in sometime"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                }
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
                headers["Authorization"] = "Session $customerShopperToken"
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

    fun showLoadingInButton() {
        binding.proceedtext.visibility = View.INVISIBLE
        binding.progress.visibility = View.VISIBLE

        // Create the rotation animation
        val rotateAnimation = ObjectAnimator.ofFloat(binding.progress, "rotation", 0f, 360f)

        // Set the duration of one full rotation in milliseconds (e.g., 1000ms for 1 second)
        rotateAnimation.duration = 1000L // Set finite duration for each rotation

        // Set it to repeat indefinitely
        rotateAnimation.repeatCount = ValueAnimator.INFINITE
        rotateAnimation.repeatMode = ValueAnimator.RESTART // Restart rotation after each cycle

        // Disable the button during the loading state
        binding.recommendedProceedButton.isEnabled = false

        // Start the animation
        rotateAnimation.start()
    }

    private fun parseAndRenderProductSummary(jsonString: String) {
        val container = binding.container

        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val groupArray = jsonArray.getJSONArray(i)
                val horizontalLayout = LinearLayout(mContext).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(32, 8, 32, 8)
                }

                // Variable to track if the first element was "linegap"
                var skipInitialLineGap = false

                for (j in 0 until groupArray.length()) {
                    val item = groupArray.getJSONObject(j)

                    // Check for "linegap" at position 0
                    if (j == 0 && item.getString("type") == "linegap") {
                        skipInitialLineGap = true
                    }

                    // If "linegap" was found at position 0, reset j to 0 for the next item
                    val currentIndex = if (skipInitialLineGap && j != 0) {
                        skipInitialLineGap = false
                        0
                    } else {
                        j
                    }

                    when (item.getString("type")) {
                        "text" -> addTextView(horizontalLayout, item, currentIndex, i != 0)
                        "image" -> addImageView(horizontalLayout, item)
                        "divider" -> addDividerView(horizontalLayout, item)
                        "linegap" -> addLineGap(container, item)
                        "background" -> setBackground(horizontalLayout, item)
                        "accordion" -> addAccordionView(container, item)
                        else -> {
                            // no op
                        }
                    }

                    // After processing the first non-"linegap" element, no need to reset `j` again

                }
                // Add the horizontal layout to the container after processing the group
                container.addView(horizontalLayout)
            }

        } catch (_: Exception) {

        }
    }

    private fun addTextView(
        container: LinearLayout,
        item: JSONObject,
        i: Int,
        toAddWeight: Boolean = true
    ) {
        try {
            val textView = TextView(mContext).apply {
                layoutParams = LayoutParams(
                    if (toAddWeight) 0 else LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                ).apply {
                    if (toAddWeight) {
                        weight = 1.0f // All views have equal weight
                    }
                }
                gravity = if (i == 0) Gravity.START else Gravity.END
            }
            textView.text = item.optString("text", "Default Text")
            textView.textSize = item.optInt("textSize", 14).toFloat()
            textView.setTextColor(Color.parseColor(item.optString("color", "#000000")))

            // Handle font type if present
            if (item.has("fontType")) {
                when (item.getString("fontType")) {
                    "Bold" -> textView.setTypeface(
                        textView.typeface,
                        android.graphics.Typeface.BOLD
                    )

                    "SemiBold" -> textView.setTypeface(
                        textView.typeface,
                        android.graphics.Typeface.BOLD
                    )
                    // Handle other font types...
                }
            }

            // Apply background color if specified
            if (item.has("background")) {
                textView.setBackgroundColor(Color.parseColor(item.getString("background")))
            }

            // Apply padding if specified
            val padding = item.optInt("padding", 4)
            textView.setPadding(padding, padding, padding, padding)

            container.addView(textView)
        } catch (e: Exception) {

        }
    }


    private fun addImageView(container: LinearLayout, item: JSONObject) {
        try {
            val imageView = ImageView(mContext).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, // Width is 0, controlled by weight
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    weight = 1.0f // All views have equal weight
                }
            }
            val size = 100 // Default size if not present
            val params = LinearLayout.LayoutParams(size, size)
            imageView.layoutParams = params

            // Load image using Glide, with error handling
            val url = item.optString("url")
            Glide.with(this)
                .load(url)
                .into(imageView)

            // Apply background color if specified
            if (item.has("background")) {
                imageView.setBackgroundColor(Color.parseColor(item.getString("background")))
            }

            container.addView(imageView)
        } catch (e: Exception) {

        }
    }


    private fun addDividerView(container: LinearLayout, item: JSONObject) {
        try {
            val divider = View(mContext)
            val thickness = item.optInt("thickness", 1)
            val params = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, thickness)
            divider.layoutParams = params
            divider.setBackgroundColor(Color.parseColor(item.optString("color", "#000000")))

            // Apply background color if specified
            if (item.has("background")) {
                divider.setBackgroundColor(Color.parseColor(item.getString("background")))
            }

            container.addView(divider)
        } catch (e: Exception) {

        }
    }

    private fun addLineGap(container: LinearLayout, item: JSONObject) {
        try {
            val gap = View(mContext)
            val params =
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 20)
            gap.layoutParams = params

            // Apply background color if specified
            gap.setBackgroundColor(Color.parseColor(item.optString("background")))

            container.addView(gap)
        } catch (e: Exception) {

        }
    }

    private fun setBackground(container: LinearLayout, item: JSONObject) {
        val color = item.optString("color", "#FFFFFF") // Default to white
        container.setBackgroundColor(Color.parseColor(color))
    }

    private fun addSpace(container: LinearLayout, item: JSONObject) {
        val space = View(mContext)
        val width = item.optInt("width", 10)
        val weight = item.optInt("weight", 1)
        val params = LayoutParams(width, LayoutParams.MATCH_PARENT, weight.toFloat())
        space.layoutParams = params

        container.addView(space)
    }

    private fun addAccordionView(container: LinearLayout, item: JSONObject) {
        try {
            val accordionLayout = LinearLayout(mContext)
            accordionLayout.orientation = LinearLayout.VERTICAL
            accordionLayout.layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

            // Header of the Accordion
            val headerLayout = LinearLayout(mContext)
            headerLayout.orientation = LinearLayout.HORIZONTAL
            headerLayout.layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            headerLayout.setPadding(32, 32, 32, 16)

            // Adding header content (text and toggle icon)
            val headerContent = item.optJSONArray("content")?.optJSONArray(0)
            headerContent?.let {
                for (i in 0 until it.length()) {
                    val headerItem = it.getJSONObject(i)
                    when (headerItem.getString("type")) {
                        "text" -> addTextView(headerLayout, headerItem, i)
                        "toggleImage" -> addToggleImageView(
                            headerLayout,
                            headerItem,
                            accordionLayout
                        )
                    }
                }
            }

            accordionLayout.addView(headerLayout)

            // Content of the Accordion (hidden by default)
            val contentLayout = LinearLayout(mContext).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                visibility = View.VISIBLE // Initially visible
            }

            val contentArray = item.optJSONArray("content")
            contentArray?.let {
                for (i in 1 until contentArray.length()) {
                    val groupArray = contentArray.getJSONArray(i)

                    val horizontalLayout = LinearLayout(mContext).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setPadding(32, 8, 32, 8)
                    }

                    if (i == 1) {
                        // Add only first two items in the first row
                        val firstRowLayout = LinearLayout(mContext).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            setPadding(32, 8, 32, 8)
                        }

                        for (j in 0 until minOf(3, groupArray.length())) {
                            val item = groupArray.getJSONObject(j)
                            when (item.getString("type")) {
                                "text" -> addTextView(firstRowLayout, item, 0, false)
                                "image" -> addImageView(firstRowLayout, item)
                                "divider" -> addDividerView(firstRowLayout, item)
                                "background" -> setBackground(firstRowLayout, item)
                                else -> {
                                    // no op
                                }
                            }
                        }
                        contentLayout.addView(firstRowLayout)
                        val secondRowLayout = LinearLayout(mContext).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                LayoutParams.WRAP_CONTENT
                            )
                            setPadding(32, 16, 32, 16)
                        }
                        for (j in 3 until groupArray.length()) {
                            val item = groupArray.getJSONObject(j)
                            when (item.getString("type")) {
                                "text" -> addTextView(secondRowLayout, item, 0, false)
                                "image" -> addImageView(secondRowLayout, item)
                                "divider" -> addDividerView(secondRowLayout, item)
                                "background" -> setBackground(secondRowLayout, item)
                                else -> {
                                    // no op
                                }
                            }
                        }
                        contentLayout.addView(secondRowLayout)
                    } else {
                        // Add the remaining items in subsequent rows
                        for (j in 0 until groupArray.length()) {
                            val item = groupArray.getJSONObject(j)
                            when (item.getString("type")) {
                                "text" -> addTextView(horizontalLayout, item, j)
                                "image" -> addImageView(horizontalLayout, item)
                                "divider" -> addDividerView(horizontalLayout, item)
                                "background" -> setBackground(horizontalLayout, item)
                                else -> {
                                    // no op
                                }
                            }
                        }
                    }
                    if (i != 1) {
                        contentLayout.addView(horizontalLayout)
                    }
                }
            }

            headerLayout.setOnClickListener {
                if (contentLayout.visibility == View.VISIBLE) {
                    contentLayout.visibility = View.GONE
                } else {
                    contentLayout.visibility = View.VISIBLE
                }
            }


            accordionLayout.addView(contentLayout)
            container.addView(accordionLayout)
        } catch (e: Exception) {

        }
    }

    private fun addToggleImageView(
        headerLayout: LinearLayout,
        headerItem: JSONObject,
        accordionLayout: LinearLayout
    ) {
        try {
            val toggleImageView = ImageView(mContext)
            val openIconUrl = headerItem.optString("openIcon")
            val closeIconUrl = headerItem.optString("closeIcon")
            val size = 40
            Glide.with(this)
                .load(closeIconUrl)
                .into(toggleImageView)

            val params = LayoutParams(size, size)
            toggleImageView.layoutParams = params

            toggleImageView.setOnClickListener {
                val contentLayout =
                    accordionLayout.getChildAt(1) as LinearLayout // Content is the second child
                if (contentLayout.visibility == View.VISIBLE) {
                    // Collapse content
                    contentLayout.visibility = View.GONE
                    Glide.with(this)
                        .load(openIconUrl)
                        .into(toggleImageView)
                } else {
                    // Expand content
                    contentLayout.visibility = View.VISIBLE
                    Glide.with(this)
                        .load(closeIconUrl)
                        .into(toggleImageView)
                }
            }

            headerLayout.addView(toggleImageView)

        } catch (e: Exception) {

        }
    }

    fun setOrderDetails(orderDetails: String) {
        this.orderDetails = orderDetails
    }

    fun setProductSummary(productSummary: String) {
        this.productSummary = productSummary
    }

    fun setAmount(amount: String) {
        this.railyatriAmount = amount
    }

    fun extractMessageFromErrorResponse(response: String): String? {
        try {
            val jsonObject = JSONObject(response)
            return jsonObject.getString("message")
        } catch (e: Exception) {
            // no op
        }
        return null
    }

    fun startCountdown(endTime: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        try {
            val endDate = dateFormat.parse(endTime) ?: return
            val currentTime = Date().time
            val timeDifference = endDate.time - currentTime
            if (timeDifference > 0) {
                sessionTimer = object : CountDownTimer(timeDifference, 1000) {

                    override fun onTick(millisUntilFinished: Long) {
                        // no changes required
                    }

                    override fun onFinish() {
                        val callback = SingletonClass.getInstance().getYourObject()
                        val callbackForDismissing =
                            SingletonForDismissMainSheet.getInstance().getYourObject()
                        if (isAdded && isResumed && !isStateSaved) {
                            if (callbackForDismissing != null) {
                                callbackForDismissing.dismissFunction()
                            }
                            if (callback != null) {
                                callback.onPaymentResult(
                                    PaymentResultObject(
                                        "Expired",
                                        transactionId ?: "",
                                        transactionId ?: ""
                                    )
                                )
                            }
                            SessionExpireScreen().show(parentFragmentManager, "SessionScreen")
                        }
                    }
                }
                sessionTimer?.start()
            }
        } catch (_: Exception) {
            // no op
        }
    }

    fun loadQrDirect(toLoadQr: Boolean) {
        toLoadQrDirect = toLoadQr
    }

    private fun swipeToPayContent() {
        val address = buildString {
            if ((showPhone && showName) || showShipping) {
                append(sharedPreferences.getString("firstName", ""))
                append(" ")
                append(sharedPreferences.getString("lastName", ""))
                append(" (${sharedPreferences.getString("phoneNumber", "")})")
            } else if (showName) {
                append(sharedPreferences.getString("firstName", ""))
                append(" ")
                append(sharedPreferences.getString("lastName", ""))
            } else {
                append("(${sharedPreferences.getString("phoneNumber", "")})")
            }

            if (showEmail || showShipping) {
                append("\n")
                append(sharedPreferences.getString("email", ""))
            }

            // Add address
            val address1 = sharedPreferences.getString("address1", "")
            val address2 = sharedPreferences.getString("address2", null)
            val city = sharedPreferences.getString("city", "")
            val state = sharedPreferences.getString("state", "null")
            val postalCode = sharedPreferences.getString("postalCode", "null")
            if (showShipping) {
                append("\n")
                if (!address2.isNullOrEmpty()) {
                    append("$address1, $address2, $city, $state, $postalCode")
                } else {
                    append("$address1, $city, $state, $postalCode")
                }
            }
        }
        binding.composeView.setContent {
            RecommendedScreen(
                modifier = Modifier,
                buttonColor = androidx.compose.ui.graphics.Color(
                    Color.parseColor(
                        sharedPreferences.getString(
                            "primaryButtonColor",
                            "#000000"
                        )
                    )
                ),
                buttontextColor = androidx.compose.ui.graphics.Color(
                    Color.parseColor(
                        sharedPreferences.getString(
                            "buttonTextColor",
                            "#000000"
                        )
                    )
                ),
                amount = sharedPreferences.getString("amount", "empty") ?: "",
                currencySymbol = sharedPreferences.getString(
                    "currencySymbol",
                    "₹"
                ) ?: "",
                lastUsedUpi = recommendedInstrumentationList[0].displayValue ?: "",
                logoUrl = recommendedInstrumentationList[0].logoUrl ?: "",
                onClickMoreOptions = {
                    moreOptionsClicked = true
                    binding.linearLayoutMain.visibility = View.VISIBLE
                    binding.recommendedProceedButton.visibility = View.VISIBLE
                    binding.recommendedProceedButtonRelativeLayout.setBackgroundColor(
                        Color.parseColor(
                            sharedPreferences.getString(
                                "primaryButtonColor",
                                "#000000"
                            )
                        )
                    )
                    binding.proceedtext.setTextColor(
                        Color.parseColor(
                            sharedPreferences.getString(
                                "buttonTextColor",
                                "#ffffff"
                            )
                        )
                    )
                    binding.recommendedProceedButton.isEnabled = true
                    binding.swipeCtaScreen.visibility = View.GONE
                },
                onSwipeComplete = {
                    callUIAnalytics(
                        context = mContext,
                        message = "",
                        screenName = "Main Bottom Sheet",
                        uiEvent = AnalyticsEvents.PAYMENT_INITIATED
                    )
                    binding.swipeScreenAnimation.apply {
                        playAnimation()
                        repeatCount =
                            LottieDrawable.INFINITE // This makes the animation repeat infinitely
                    }
                    binding.swipeLoader.visibility = View.VISIBLE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        postRecommendedInstruments(
                            if(recommendedInstrumentationList[0].type.equals("upi", true))"upi/collect" else "card/token",
                            recommendedInstrumentationList[0].instrumentationRef ?: "",
                            recommendedInstrumentationList[0].displayValue ?: ""
                        )
                    }
                },
                address = address,
                onClickChangeAddress = {
                    openSavedOrAddOrEditAddressScreen()
                },
                toShowOnChangeAddressClick = isEmailEditable || isPhoneEditable || isNameEditable || showShipping,
                toShowAddress = showEmail || showShipping || showPhone || showName,
                toShowPersonal = !showShipping
            )
        }
    }

    private fun openSavedOrAddOrEditAddressScreen() {
        if ((!binding.loadingRelativeLayout.isVisible) && (isEmailEditable || isPhoneEditable || isNameEditable || showShipping)) {
            if (customerShopperToken != null && customerShopperToken != "" && showShipping) {
                val bottomSheet = SavedAddressBottomSheet()
                bottomSheet.setAddressViewAndEditSettings(
                    viewName = showName,
                    viewEmail = showEmail,
                    viewPhone = showPhone,
                    viewShipping = showShipping,
                    editPan = isPANEditable,
                    editDob = isDOBEditable,
                    editEmail = isEmailEditable,
                    editPhone = isPhoneEditable,
                    editName = isNameEditable,
                    viewDob = showDOB,
                    viewPan = showPAN
                )
                viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                    bottomSheet.show(parentFragmentManager, "SavedAddressBottomSheet")
                }
            } else {
                val bottomSheet = DeliveryAddressBottomSheet.newInstance(
                    this,
                    false,
                    showName,
                    showPhone,
                    showEmail,
                    showPAN,
                    showDOB,
                    showShipping,
                    isNameEditable,
                    isPhoneEditable,
                    isEmailEditable,
                    isPANEditable,
                    isDOBEditable
                )
                viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                    bottomSheet.show(parentFragmentManager, "DeliveryAddressBottomSheet")
                }
            }
        }
    }

    private fun showAddressRelatedDataOnScreen() {
        binding.cardView8.visibility = View.GONE
        binding.deliveryAddressText.visibility = View.GONE
        binding.textView12.visibility = View.GONE
        binding.upiLinearLayout.visibility = View.GONE
        binding.cardView5.visibility = View.GONE
        binding.cardView6.visibility = View.GONE
        binding.emiCard.visibility = View.GONE
        binding.cardView7.visibility = View.GONE
        binding.netBankingConstraint.visibility = View.GONE
        binding.bnplConstraint.visibility = View.GONE
        binding.cardConstraint.visibility = View.GONE
        binding.recommendedCardView.visibility = View.GONE
        binding.recommendedLinearLayout.visibility = View.GONE
        binding.walletConstraint.visibility = View.GONE
        binding.emiConstraint.visibility = View.GONE
        binding.linearLayout.visibility = View.GONE
        binding.textView111.text = "Order Details"
        binding.addAddressButton.visibility = View.VISIBLE
        priceBreakUpVisible = true
        bottomSheet = DeliveryAddressBottomSheet.newInstance(
            this,
            false,
            showName,
            showPhone,
            showEmail,
            showPAN,
            showDOB,
            showShipping,
            isNameEditable,
            isPhoneEditable,
            isEmailEditable,
            isPANEditable,
            isDOBEditable
        )
        showPriceBreakUp()
    }

    private fun processShopper(
        shopper: JSONObject,
        orderDetails: String?,
        showShipping: Boolean,
        showName: Boolean,
        showEmail: Boolean,
        showPhone: Boolean,
        showPAN: Boolean,
        showDOB: Boolean,
    ) {
        // 1. Define clear, self‑documenting flags
        val needsAddress = shopper.isNull("deliveryAddress") && showShipping && orderDetails == null
        val needsPersonalInfo = (
                shopper.isNull("firstName")
                        || shopper.isNull("phoneNumber")
                        || shopper.isNull("email")
                ) && (showName || showPhone || showEmail) && orderDetails == null
        val needsPan = showPAN && shopper.isNull("panNumber")
        val needsDob = showDOB && shopper.isNull("dateOfBirth")

        // 2. If *any* required data is missing, stop and show the address/name form
        if (listOf(needsAddress, needsPersonalInfo, needsPan, needsDob).any { it }) {
            showAddressRelatedDataOnScreen()
            return
        }

        // 3. Otherwise, switch UI to Payment Details
        binding.textView111.text = "Payment Details"
        binding.addAddressButton.visibility = View.GONE
    }

    private fun saveShopperToPrefs(
        shopper: JSONObject,
        editor: SharedPreferences.Editor
    ) = editor.apply {
        // Simple list of (JSON‑key to prefs‑key) mappings
        mapOf(
            "firstName" to "firstName",
            "lastName" to "lastName",
            "email" to "email",
            "gender" to "gender",
            "panNumber" to "panNumber",
            "dateOfBirth" to "dateOfBirth"
        ).forEach { (jsonKey, prefKey) ->
            shopper.optString(jsonKey)
                .takeIf { it.isNotBlank() }
                ?.let { putString(prefKey, it) }
        }

        // Phone needs a leading '+' if missing
        shopper.optString("phoneNumber").takeIf { it.isNotBlank() }?.let { raw ->
            val normalized = if (raw.startsWith("+")) raw else "+$raw"
            putString("phoneNumber", normalized)
        }

        // Delivery address block
        shopper.optJSONObject("deliveryAddress")?.let { addr ->
            mapOf(
                "address1" to "address1",
                "address2" to "address2",
                "city" to "city",
                "state" to "state",
                "postalCode" to "postalCode"
            ).forEach { (jsonKey, prefKey) ->
                addr.optString(jsonKey)
                    .takeIf { it.isNotBlank() }
                    ?.let { putString(prefKey, it) }
            }
        }

        apply()
    }

    private fun logMainBottomSheetUiEvents() {
        callUIAnalytics(
            context = mContext,
            message = "",
            screenName = "Main Bottom Sheet",
            uiEvent = AnalyticsEvents.PAYMENT_METHOD_SELECTED
        )
        callUIAnalytics(
            context = mContext,
            message = "",
            screenName = "Main Bottom Sheet",
            uiEvent = AnalyticsEvents.PAYMENT_CATEGORY_SELECTED
        )
        callUIAnalytics(
            context = mContext,
            message = "",
            screenName = "Main Bottom Sheet",
            uiEvent = AnalyticsEvents.PAYMENT_INSTRUMENT_PROVIDED
        )
    }

    private fun callUiAnalyticWithSdkCrashEvent(message: String) {
        callUIAnalytics(
            context = mContext,
            message = message,
            screenName = "Main Bottom Sheet in function dismissMainSheet",
            uiEvent = AnalyticsEvents.SDK_CRASH
        )
    }
}