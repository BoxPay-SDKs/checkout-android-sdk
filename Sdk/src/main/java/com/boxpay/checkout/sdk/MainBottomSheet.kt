package com.boxpay.checkout.sdk

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
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
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
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
import com.boxpay.checkout.sdk.ViewModels.CallBackFunctions
import com.boxpay.checkout.sdk.ViewModels.CallbackForDismissMainSheet
import com.boxpay.checkout.sdk.ViewModels.OverlayViewModel
import com.boxpay.checkout.sdk.ViewModels.SingletonClassForLoadingState
import com.boxpay.checkout.sdk.ViewModels.SingletonForDismissMainSheet
import com.boxpay.checkout.sdk.adapters.OrderSummaryItemsAdapter
import com.boxpay.checkout.sdk.adapters.RecommendedItemsAdapter
import com.boxpay.checkout.sdk.databinding.FragmentMainBottomSheetBinding
import com.boxpay.checkout.sdk.interfaces.UpdateMainBottomSheetInterface
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
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
import java.util.Locale
import java.util.Objects
import kotlin.random.Random


internal class MainBottomSheet : BottomSheetDialogFragment(), UpdateMainBottomSheetInterface {
    private var transactionId: String? = null
    private var isSuccessful = false
    private var qrCodeShown = false
    private var overlayViewMainBottomSheet: View? = null
    private lateinit var context: Context
    private lateinit var binding: FragmentMainBottomSheetBinding
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private val overlayViewModel: OverlayViewModel by activityViewModels()
    private var overlayViewCurrentBottomSheet: View? = null
    private var token: String? = null
    private var customerShopperToken: String? = null
    private var recommendedInstrumentationList = mutableListOf<Pair<String, String>>()
    private var uniqueReference: String? = null
    private var successScreenFullReferencePath: String? = null
    private var UPIAppsAndPackageMap: MutableMap<String, String> = mutableMapOf()
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private var job: Job? = null
    private var isTablet = false
    private var showName = false
    private var recommendedCheckedPosition: Int? = null
    private var showEmail = false
    private var railyatriAmount: String? = null
    private var showShipping = false
    private var showPhone = false
    var upiOptionsShown = false
    private var priceBreakUpVisible = false
    var countryCode: Pair<String, String>? = null
    private var transactionAmount: String? = null
    private var upiAvailable = false
    private var upiCollectMethod = false
    private var upiIntentMethod = false
    private var upiQRMethod = false
    private var cardsMethod = false
    private var isNameEditable = true
    private var isPhoneEditable = true
    private var isEmailEditable = true
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
    private var productSummary: String? = null
    private var orderDetails: String? = null


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

        showLoadingState("") // Show loading state before initiating tasks


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
        // List of known UPI-supported apps and their corresponding package names
        val upiAppPackages = setOf(
            "com.google.android.apps.nbu.paisa.user", // GPay
            "com.phonepe.app",                        // PhonePe
            "net.one97.paytm"                         // Paytm
        )
        var i = 0
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in apps) {
            val appName = packageManager.getApplicationLabel(app).toString()

            // Check if the app's package is in the known UPI apps list
            if (upiAppPackages.contains(app.packageName)) {
                i++
                UPIAppsAndPackageMap[appName] = app.packageName
            }
        }

        populatePopularUPIApps()
    }


    private fun showLoadingState(source: String) {
        binding.boxpayLogoLottie.apply {
            playAnimation()
            repeatCount = LottieDrawable.INFINITE // This makes the animation repeat infinitely
        }
        binding.loadingRelativeLayout.visibility = View.VISIBLE
    }


    private fun removeLoadingState() {
        binding.loadingRelativeLayout.visibility = View.GONE
        binding.boxpayLogoLottie.cancelAnimation()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        showLoadingState("")
        if (requestCode == 121) {
            if (resultCode == Activity.RESULT_OK) {
                val responseUri: Uri? = data?.data
                if (responseUri != null) {
                    val responseString = responseUri.toString()
                    if (responseString.contains("success", ignoreCase = true)) {
                        // Payment was successful
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
                    } else if (responseString.contains(
                            "fail",
                            ignoreCase = true
                        ) || responseString.contains("decline", ignoreCase = true)
                    ) {
                        // Payment was declined or failed
                        editor.putString("status", "Failed")
                        editor.apply()
                        if (isAdded && isResumed && !isStateSaved) {
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

                        if (isAdded && isResumed && !isStateSaved) {
                            val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                            bottomSheet.show(
                                parentFragmentManager,
                                "PaymentStatusBottomSheetWithDetails"
                            )
                            job?.cancel()
                        }
                    } else if (responseString.contains(
                            "fail",
                            ignoreCase = true
                        ) || responseString.contains("decline", ignoreCase = true)
                    ) {
                        // Payment was declined or failed
                        editor.putString("status", "Failed")
                        editor.apply()
                        if (isAdded && isResumed && !isStateSaved) {
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
        } else if (requestCode == 123) {
            if (resultCode == Activity.RESULT_OK) {
                val responseUri: Uri? = data?.data
                if (responseUri != null) {
                    val responseString = responseUri.toString()
                    if (responseString.contains("success", ignoreCase = true)) {
                        // Payment was successful
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
                    } else if (responseString.contains(
                            "fail",
                            ignoreCase = true
                        ) || responseString.contains("decline", ignoreCase = true)
                    ) {
                        // Payment was declined or failed
                        editor.putString("status", "Failed")
                        editor.apply()
                        if (isAdded && isResumed && !isStateSaved) {
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

                        if (isAdded && isResumed && !isStateSaved) {
                            val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                            bottomSheet.show(
                                parentFragmentManager,
                                "PaymentStatusBottomSheetWithDetails"
                            )
                            job?.cancel()
                        }
                    } else if (responseString.contains(
                            "fail",
                            ignoreCase = true
                        ) || responseString.contains("decline", ignoreCase = true)
                    ) {
                        // Payment was declined or failed
                        editor.putString("status", "Failed")
                        editor.apply()
                        if (isAdded && isResumed && !isStateSaved) {
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
            var resultCode: Int
            startFunctionCalls()
            if (url.startsWith("tez")) {
                resultCode = 121
            } else if (url.startsWith("paytm")) {
                resultCode = 122
            } else {
                resultCode = 123
            }

            startActivityForResult(intent, resultCode)
        } catch (_: ActivityNotFoundException) {
            removeLoadingState()
        }
    }

    private fun startFunctionCalls() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(3000)
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
                    if (status.equals("Pending", ignoreCase = true) && isGpayReturned) {
                        removeLoadingState()
                        job?.cancel()
                        isGpayReturned = false
                        editor.putString("status", "Failed")
                        editor.apply()
                        PaymentFailureScreen(
                            errorMessage = "Payment failed with GPay. Please retry payment with a different UPI app"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                    if (status.equals("Pending", ignoreCase = true) && isPhonePe) {
                        removeLoadingState()
                        job?.cancel()
                        isPhonePe = false
                        editor.putString("status", "Failed")
                        editor.apply()
                        PaymentFailureScreen(
                            errorMessage = "Payment failed with PhonePe. Please retry payment with a different UPI app"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                    if (status.equals("Pending", ignoreCase = true) && isOthersReturned) {
                        removeLoadingState()
                        job?.cancel()
                        isOthersReturned = false
                        editor.putString("status", "Failed")
                        editor.apply()
                        PaymentFailureScreen(
                            errorMessage = "Please retry using other payment method or try again in sometime"
                        ).show(parentFragmentManager, "FailureScreen")
                    }
                    if (status.equals("Pending", ignoreCase = true) && isPaytmReturned) {
                        removeLoadingState()
                        job?.cancel()
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
                    PaymentFailureScreen().show(parentFragmentManager, "FailureScreenFromUPIIntent")
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
            showShipping,
            isNameEditable,
            isPhoneEditable,
            isEmailEditable
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
        if (::context.isInitialized) {
            val config = ClarityConfig("o4josf35jv")
            Clarity.initialize(context, config)
            Clarity.setCustomTag("token", token)
        }

        hidePriceBreakUp()

        val callBackFunctionsForDismissing = CallbackForDismissMainSheet(::dismissMainSheet)
        SingletonForDismissMainSheet.getInstance().callBackFunctions =
            callBackFunctionsForDismissing

        val orderSummaryAdapter =
            OrderSummaryItemsAdapter(imagesUrls, items, prices, itemQty, requireContext())
        binding.itemsInOrderRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.itemsInOrderRecyclerView.adapter = orderSummaryAdapter

        val recommendedInstrumentsAdapter = RecommendedItemsAdapter(
            recommendedInstrumentationList, binding.recomendedRecyclerView, requireContext()
        )
        binding.recomendedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recomendedRecyclerView.adapter = recommendedInstrumentsAdapter
        var currencySymbol = sharedPreferences.getString("currencySymbol", "")
        updateTransactionAmountInSharedPreferences(currencySymbol + transactionAmount.toString())
        if (currencySymbol == "")
            currencySymbol = "₹"


        // Set click listeners

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
                if (binding.recomendedOptionsLinearLayout.isVisible) {
                    recommendedInstrumentsAdapter.checkPositionLiveData.value =
                        RecyclerView.NO_POSITION
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
                    binding.recommendedProceedButton.visibility = View.VISIBLE
                }
            }
        }

        binding.recommendedProceedButton.setOnClickListener {
            if (!binding.loadingRelativeLayout.isVisible) {
                postRecommendedInstruments(
                    "upi/collect",
                    recommendedInstrumentationList[recommendedCheckedPosition!!].first,
                    recommendedInstrumentationList[recommendedCheckedPosition!!].second
                )
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
                recommendedInstrumentsAdapter.checkPositionLiveData.value = RecyclerView.NO_POSITION
                hideRecommendedOptions()
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
                callUIAnalytics(
                    requireContext(),
                    "PAYMENT_INSTRUMENT_PROVIDED",
                    "UpiCollect",
                    "Upi"
                )
                callUIAnalytics(requireContext(), "PAYMENT_CATEGORY_SELECTED", "", "Upi")
                callUIAnalytics(requireContext(), "PAYMENT_METHOD_SELECTED", "UpiCollect", "Upi")
                job?.cancel()
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
                    showQRCode()
                }
            }
        }

        binding.qrCodeOpenConstraint.setOnClickListener() {
            // for the sake that it does not open or closes the options
        }

        binding.cardConstraint.setOnClickListener() {
            if (!binding.loadingRelativeLayout.isVisible) {
                recommendedInstrumentsAdapter.checkPositionLiveData.value = RecyclerView.NO_POSITION
                hideRecommendedOptions()
                binding.cardConstraint.isEnabled = false
                callUIAnalytics(requireContext(), "PAYMENT_CATEGORY_SELECTED", "", "Card")
                callUIAnalytics(requireContext(), "PAYMENT_METHOD_SELECTED", "", "Card")
                openAddCardBottomSheet()
            }
        }


        binding.walletConstraint.setOnClickListener() {
            if (!binding.loadingRelativeLayout.isVisible) {
                recommendedInstrumentsAdapter.checkPositionLiveData.value = RecyclerView.NO_POSITION
                hideRecommendedOptions()
                binding.walletConstraint.isEnabled = false
                callUIAnalytics(requireContext(), "PAYMENT_CATEGORY_SELECTED", "", "Wallet")
                openWalletBottomSheet()
            }
        }

        binding.bnplConstraint.setOnClickListener() {
            if (!binding.loadingRelativeLayout.isVisible) {
                recommendedInstrumentsAdapter.checkPositionLiveData.value = RecyclerView.NO_POSITION
                hideRecommendedOptions()
                binding.bnplConstraint.isEnabled = false
                callUIAnalytics(requireContext(), "PAYMENT_CATEGORY_SELECTED", "", "BuyNowPayLater")
                openBNPLBottomSheet()
            }
        }


        binding.netBankingConstraint.setOnClickListener() {
            if (!binding.loadingRelativeLayout.isVisible) {
                recommendedInstrumentsAdapter.checkPositionLiveData.value = RecyclerView.NO_POSITION
                hideRecommendedOptions()
                binding.netBankingConstraint.isEnabled = false
                callUIAnalytics(requireContext(), "PAYMENT_CATEGORY_SELECTED", "", "NetBanking")
                openNetBankingBottomSheet()
            }
        }

        binding.refreshButton.setOnClickListener() {
            if (!binding.loadingRelativeLayout.isVisible) {
                showQRCode()
            }
        }

        binding.deliveryAddressConstraintLayout.setOnClickListener() {
            if ((!binding.loadingRelativeLayout.isVisible) && (isEmailEditable || isPhoneEditable || isNameEditable || showShipping)) {
                if (!sharedPreferences.getString("phoneNumber", "").isNullOrEmpty()) {
                    val confirmPhoneNumber = sharedPreferences.getString("phoneNumber", "")
                        ?.removePrefix(countryCode?.second ?: "")
                    editor.putString("phoneNumber", confirmPhoneNumber)
                    editor.putString("phoneCode", countryCode?.second)
                    editor.putString("countryName", countryCode?.first)
                    editor.apply()
                }
                bottomSheet = DeliveryAddressBottomSheet.newInstance(
                    this,
                    false,
                    showName,
                    showPhone,
                    showEmail,
                    showShipping,
                    isNameEditable,
                    isPhoneEditable,
                    isEmailEditable
                )
                bottomSheet.show(parentFragmentManager, "DeliveryAddressBottomSheetOnClick")
            }
        }

        binding.proceedButton.setOnClickListener() {
            if (!binding.loadingRelativeLayout.isVisible) {
                if (!sharedPreferences.getString("phoneNumber", "").isNullOrEmpty()) {
                    val confirmPhoneNumber = sharedPreferences.getString("phoneNumber", "")
                        ?.removePrefix(countryCode?.second ?: "")
                    editor.putString("phoneNumber", confirmPhoneNumber)
                    editor.putString("countryName", countryCode?.first)
                    editor.putString("phoneCode", countryCode?.second)
                    editor.apply()
                }
                bottomSheet.show(parentFragmentManager, "DeliveryAddressBottomSheetOnClick")
            }
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
            Response.ErrorListener { /* no response handling */error ->
                removeLoadingState()
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
        val requestQueue = Volley.newRequestQueue(context)
        uniqueReference = sharedPreferences.getString("uniqueReference", null)
        val jsonObjectRequest = object : JsonArrayRequest(
            Method.GET,
            Base_Session_API_URL + token + "/shoppers/$uniqueReference/recommended-instruments",
            null,
            Response.Listener { response ->
                try {
                    val jsonArray = response

                    // Map each element in the JSONArray
                    if (jsonArray != emptyArray<Objects>()) {
                        val mappedList = (0 until minOf(2, jsonArray.length())).map { index ->
                            val instrumentationRef =
                                jsonArray.getJSONObject(index)
                                    .getString("instrumentRef")
                            val displayValue =
                                jsonArray.getJSONObject(index)
                                    .getString("displayValue")
                            val pair = Pair(
                                instrumentationRef, displayValue
                            )
                            recommendedInstrumentationList.add(pair)
                        }
                        if (recommendedInstrumentationList.isNotEmpty() && binding.upiLinearLayout.isVisible) {
                            binding.recommendedCardView.visibility = View.VISIBLE
                            binding.recommendedLinearLayout.visibility = View.VISIBLE
                            binding.recommendedProceedButton.visibility = View.VISIBLE
                            recommendedCheckedPosition = 0
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
                // no op
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
    }

    private fun populatePopularUPIApps() {
        var i = 1
        if (UPIAppsAndPackageMap.containsKey("PhonePe")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setImageResource(R.drawable.phonepe_logo)
            textView.text = "PhonePe"
            getPopularConstraintLayoutByNum(i).setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    overlayViewModel.setShowOverlay(false)
                    showLoadingState("fetchIntentURL")
                    getUrlForUPIIntent("PhonePe")
                    callUIAnalytics(
                        requireContext(),
                        "PAYMENT_INSTRUMENT_PROVIDED",
                        "UpiIntent",
                        "Upi"
                    )
                    callUIAnalytics(requireContext(), "PAYMENT_METHOD_SELECTED", "UpiIntent", "Upi")
                    callUIAnalytics(requireContext(), "PAYMENT_INITIATED", "UpiIntent", "Upi")
                }
            }

            i++
        }


        if (UPIAppsAndPackageMap.containsKey("GPay")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setImageResource(R.drawable.google_pay_seeklogo)
            textView.text = "GPay"

            getPopularConstraintLayoutByNum(i).setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    overlayViewModel.setShowOverlay(false)
                    showLoadingState("fetchIntentURL")
                    getUrlForUPIIntent("GPay")
                    callUIAnalytics(
                        requireContext(),
                        "PAYMENT_INSTRUMENT_PROVIDED",
                        "UpiIntent",
                        "Upi"
                    )
                    callUIAnalytics(requireContext(), "PAYMENT_METHOD_SELECTED", "UpiIntent", "Upi")
                    callUIAnalytics(requireContext(), "PAYMENT_INITIATED", "UpiIntent", "Upi")
                }
            }

            i++
        }


        if (UPIAppsAndPackageMap.containsKey("Paytm")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setImageResource(R.drawable.paytm_upi_logo)
            textView.text = "Paytm"

            getPopularConstraintLayoutByNum(i).setOnClickListener() {
                if (!binding.loadingRelativeLayout.isVisible) {
                    overlayViewModel.setShowOverlay(false)
                    showLoadingState("fetchIntentURL")
                    getUrlForUPIIntent("PayTm")
                    callUIAnalytics(
                        requireContext(),
                        "PAYMENT_INSTRUMENT_PROVIDED",
                        "UpiIntent",
                        "Upi"
                    )
                    callUIAnalytics(requireContext(), "PAYMENT_METHOD_SELECTED", "UpiIntent", "Upi")
                    callUIAnalytics(requireContext(), "PAYMENT_INITIATED", "UpiIntent", "Upi")
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
                showLoadingState("payUsingAnyUPIConstraint")
                getUrlForDefaultUPIIntent()
                callUIAnalytics(requireContext(), "PAYMENT_INSTRUMENT_PROVIDED", "UpiIntent", "Upi")
                callUIAnalytics(requireContext(), "PAYMENT_METHOD_SELECTED", "UpiIntent", "Upi")
                callUIAnalytics(requireContext(), "PAYMENT_INITIATED", "UpiIntent", "Upi")
            }
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
        try {
            startFunctionCalls()
            startActivityForResult(intent, 124)
        } catch (_: Exception) {
            removeLoadingState()
            Toast.makeText(context, "No other UPI options", Toast.LENGTH_SHORT).show()
        }
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
        if (context != null) {
            requireActivity().runOnUiThread {
                windowManager.addView(overlayViewMainBottomSheet, layoutParams)
            }
        }
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

    private fun showRecommendedOptions() {
        if (binding.upiOptionsLinearLayout.isVisible) {
            upiOptionsShown = false
            hideUPIOptions()
        }
        binding.recomendedConstraint.setBackgroundColor(Color.parseColor("#E0F1FF"))
        binding.recomendedRecyclerView.visibility = View.VISIBLE
        binding.recomendedOptionsLinearLayout.visibility = View.VISIBLE
        binding.recomendedText.typeface =
            ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
    }

    private fun hideRecommendedOptions() {
        binding.recomendedConstraint.setBackgroundColor(Color.parseColor("#FFFFFF"))
        binding.recomendedRecyclerView.visibility = View.GONE
        binding.recomendedText.typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins)
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
            ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)

        if (UPIAppsAndPackageMap.isNotEmpty()) {
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
            val percentageOfScreenHeight = 0.95 // 70%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

            bottomSheetBehavior?.maxHeight = desiredHeight

            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBehavior?.isDraggable = false
            bottomSheetBehavior?.isHideable = false


            dialog.setCancelable(!binding.progressBar.isVisible)

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

                val totalAmount = paymentDetailsObject.getJSONObject("money").getString("amount")
                val amount = totalAmount.toDouble()

// Format the amount using the NumberFormat class for locale-specific formatting
                val formattedAmount = NumberFormat.getNumberInstance(Locale.US).format(amount)


                var orderObject: JSONObject? = null
                if (!paymentDetailsObject.isNull("order")) {
                    orderObject = paymentDetailsObject.getJSONObject("order")
                }

                val subscriptionDetails = paymentDetailsObject.optJSONObject("subscriptionDetails")
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
                            isPhoneEditable = fieldObject.optBoolean("editable", false)
                        }
                        if (fieldObject.optString("field", "UNKNOWN").contains("name", true)) {
                            showName = true
                            isNameEditable = fieldObject.optBoolean("editable", false)
                        }
                        if (fieldObject.optString("field", "UNKNOWN").contains("email", true)) {
                            showEmail = true
                            isEmailEditable = fieldObject.optBoolean("editable", false)
                        }
                    }
                } else {
                    println("No enabled fields found")
                }
                if (showShipping) {
                    binding.textView6.text = "Continue to Add New Address"
                } else {
                    binding.textView6.text = "Continue to Add Personal Details"
                }

                if (showEmail || showShipping || showPhone || showName) {
                    binding.deliveryAddressConstraintLayout.visibility = View.VISIBLE
                } else {
                    binding.deliveryAddressConstraintLayout.visibility = View.GONE
                }
                binding.emailTextView.visibility =
                    if (showEmail || showShipping) View.VISIBLE else View.GONE
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
                if (currencySymbol == "")
                    currencySymbol = "₹"

                var totalQuantity = 0
                editor.putString("currencySymbol", currencySymbol)
                editor.apply()

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

                binding.unopenedTotalValue.text = "${currencySymbol}${formattedAmount}"
                if (totalQuantity == 0) {
                    binding.numberOfItems.text = "Total"
                } else if (totalQuantity == 1)
                    binding.numberOfItems.text = "${totalQuantity} item"
                else
                    binding.numberOfItems.text = "${totalQuantity} items"
                binding.ItemsPrice.text = "${currencySymbol}${formattedAmount}"

                if (originalAmount != null && originalAmount != "0" && originalAmount != "null") {
                    val doubleTypeOriginal =
                        NumberFormat.getNumberInstance(Locale.US).format(originalAmount.toDouble())
                    binding.subtotalTextView.text = "${currencySymbol}${doubleTypeOriginal}"
                    binding.subTotalRelativeLayout.visibility = View.VISIBLE
                }

                if (taxes != null && taxes != "null" && taxes != "0") {
                    val doubleTypeTax =
                        NumberFormat.getNumberInstance(Locale.US).format(taxes.toDouble())
                    binding.taxTextView.text = "${currencySymbol}${doubleTypeTax}"
                    binding.taxesRelativeLayout.visibility = View.VISIBLE
                }

                if (shippingCharges != null && shippingCharges != "null" && shippingCharges != "0") {
                    val doubleTypeshipping =
                        NumberFormat.getNumberInstance(Locale.US).format(shippingCharges.toDouble())
                    binding.shippingChargesTextView.text =
                        "${currencySymbol}$doubleTypeshipping"
                    binding.shippingChargesRelativeLayout.visibility = View.VISIBLE
                }

                if ((originalAmount == null || originalAmount == "0" && originalAmount == "null") && (shippingCharges == null || shippingCharges == "null" || shippingCharges == "0") && (taxes == null || taxes == "null" && taxes == "0")) {
                    binding.arrowIcon.visibility = View.GONE
                    binding.orderSummaryConstraintLayout.setOnClickListener(null)
                }

                if (!binding.shippingChargesRelativeLayout.isVisible && !binding.taxesRelativeLayout.isVisible && !binding.subTotalRelativeLayout.isVisible) {
                    binding.blackLine.visibility = View.GONE
                }

                val jsonString = readJsonFromAssets(requireContext(), "countryCodes.json")
                val countryCodeJson = JSONObject(jsonString)
                val shopperObject = paymentDetailsObject.getJSONObject("shopper")
                countryCode = getCountryName(
                    countryCodeJson,
                    if (shopperObject.getString("phoneNumber").contains('+')) {
                        shopperObject.getString("phoneNumber")
                    } else {
                        "+" + shopperObject.getString("phoneNumber")
                    }
                )
                editor.putString("countryName", countryCode?.first)
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

                if (!shopperObject.isNull("uniqueReference")) {
                    editor.putString("uniqueReference", shopperObject.getString("uniqueReference"))
                }
                if (shopperObject.isNull("deliveryAddress")) {
                    editor.putString("address1", null)
                    editor.putString("address2", null)
                    editor.putString("countryName", null)
                    editor.putString("indexCountryCodePhone", null)
                    editor.putString("phoneCode", null)
                    editor.putString("city", null)
                    editor.putString("state", null)
                    editor.putString("postalCode", null)
                } else {
                    editor.putString(
                        "postalCode",
                        shopperObject.getJSONObject("deliveryAddress").getString("postalCode")
                    )
                    editor.putString(
                        "state",
                        shopperObject.getJSONObject("deliveryAddress").getString("state")
                    )
                    editor.putString(
                        "city",
                        shopperObject.getJSONObject("deliveryAddress").getString("city")
                    )
                    editor.putString("indexCountryCodePhone", countryCode?.second)
                    editor.putString("phoneCode", countryCode?.second)
                    editor.putString(
                        "address2",
                        shopperObject.getJSONObject("deliveryAddress").getString("address2")
                    )
                    editor.putString(
                        "address1",
                        shopperObject.getJSONObject("deliveryAddress").getString("address1")
                    )
                }
                if (shopperObject.isNull("firstName")) {
                    editor.putString("firstName", null)
                } else {
                    editor.putString("firstName", shopperObject.getString("firstName"))
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
                if (shopperObject.isNull("deliveryAddress") && showShipping && orderDetails == null) {
                    binding.deliveryAddressConstraintLayout.visibility = View.GONE
                    binding.textView12.visibility = View.GONE
                    binding.upiLinearLayout.visibility = View.GONE
                    binding.cardView5.visibility = View.GONE
                    binding.cardView6.visibility = View.GONE
                    binding.cardView7.visibility = View.GONE
                    binding.netBankingConstraint.visibility = View.GONE
                    binding.bnplConstraint.visibility = View.GONE
                    binding.cardConstraint.visibility = View.GONE
                    binding.recommendedCardView.visibility = View.GONE
                    binding.recommendedLinearLayout.visibility = View.GONE
                    binding.walletConstraint.visibility = View.GONE
                    binding.linearLayout.visibility = View.GONE
                    binding.textView111.text = "Order Details"
                    binding.proceedButton.visibility = View.VISIBLE
                    priceBreakUpVisible = true
                    bottomSheet = DeliveryAddressBottomSheet.newInstance(
                        this,
                        false,
                        showName,
                        showPhone,
                        showEmail,
                        showShipping,
                        isNameEditable,
                        isPhoneEditable,
                        isEmailEditable
                    )
                    showPriceBreakUp()
                } else if ((shopperObject.isNull("firstName") || shopperObject.isNull("phoneNumber") || shopperObject.isNull(
                        "email"
                    )) && (showName || showEmail || showPhone) && orderDetails == null
                ) {
                    binding.deliveryAddressConstraintLayout.visibility = View.GONE
                    binding.textView12.visibility = View.GONE
                    binding.upiLinearLayout.visibility = View.GONE
                    binding.cardView5.visibility = View.GONE
                    binding.cardView6.visibility = View.GONE
                    binding.cardView7.visibility = View.GONE
                    binding.netBankingConstraint.visibility = View.GONE
                    binding.bnplConstraint.visibility = View.GONE
                    binding.cardConstraint.visibility = View.GONE
                    binding.walletConstraint.visibility = View.GONE
                    binding.linearLayout.visibility = View.GONE
                    binding.textView111.text = "Order Details"
                    binding.proceedButton.visibility = View.VISIBLE
                    binding.recommendedCardView.visibility = View.GONE
                    binding.recommendedLinearLayout.visibility = View.GONE
                    priceBreakUpVisible = true
                    bottomSheet = DeliveryAddressBottomSheet.newInstance(
                        this,
                        false,
                        showName,
                        showPhone,
                        showEmail,
                        showShipping,
                        isNameEditable,
                        isPhoneEditable,
                        isEmailEditable
                    )
                    showPriceBreakUp()
                } else {
                    binding.textView111.text = "Payment Details"
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
                    if (!shopperObject.isNull("deliveryAddress")) {
                        val deliveryAddress = shopperObject.getJSONObject("deliveryAddress")
                        if (!deliveryAddress.isNull("address1")) {
                            editor.putString("address1", deliveryAddress.getString("address1"))
                        }
                        if (!deliveryAddress.isNull("address2")) {
                            editor.putString("address2", deliveryAddress.getString("address2"))
                        }
                        if (!deliveryAddress.isNull("countryCode")) {
                            editor.putString("countryName", countryCode?.first)
                            editor.putString("indexCountryCodePhone", countryCode?.second)
                            editor.putString("phoneCode", countryCode?.second)
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

                binding.nameAndMobileTextViewMain.text =
                    if ((showPhone && showName) || showShipping) {
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
                binding.emailTextView.text = sharedPreferences.getString("email", "")
                if (showShipping) {
                    binding.textView2.text = "Delivery Address"
                    binding.addressTextViewMain.text =
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
                } else {
                    binding.textView2.text = "Personal details"
                    binding.homeIcon.visibility = View.GONE
                    binding.deliveryAddressObjectImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            context!!,
                            R.drawable.ic_personal_details
                        )
                    )
                    binding.addressTextViewMain.visibility = View.GONE
                }
                if (customerShopperToken != null && customerShopperToken != "") {
                    getRecommendedInstrumentation()
                } else {
                    upiOptionsShown = true
                    showUPIOptions()
                    removeLoadingState()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
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
                        requireContext(),
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

    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences =
            requireContext().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token", "empty")
        customerShopperToken = sharedPreferences.getString("shopperToken", "")
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
        binding.nameAndMobileTextViewMain.text = if ((showPhone && showName) || showShipping) {
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
        binding.emailTextView.text = sharedPreferences.getString("email", "")
        if (showShipping) {
            binding.textView2.text = "Delivery Address"
            binding.addressTextViewMain.text =
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
        } else {
            binding.textView2.text = "Personal details"
            binding.addressTextViewMain.visibility = View.GONE
        }
        binding.cardView8.visibility = View.VISIBLE
        countryCode = Pair(
            sharedPreferences.getString("countryName", "") ?: "",
            sharedPreferences.getString("phoneCode", null) ?: ""
        )

        binding.deliveryAddressConstraintLayout.visibility = View.VISIBLE
        binding.textView12.visibility = View.VISIBLE
        binding.upiLinearLayout.visibility = View.VISIBLE
        binding.cardView5.visibility = View.VISIBLE
        binding.cardView6.visibility = View.VISIBLE
        binding.cardView7.visibility = View.VISIBLE
        binding.walletConstraint.visibility = View.VISIBLE
        binding.netBankingConstraint.visibility = View.VISIBLE
        binding.cardConstraint.visibility = View.VISIBLE
        binding.bnplConstraint.visibility = View.VISIBLE
        binding.walletConstraint.visibility = View.VISIBLE
        binding.linearLayout.visibility = View.VISIBLE
        binding.textView111.text = "Payment Details"
        binding.proceedButton.visibility = View.GONE
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
        phoneNumber: String
    ): Pair<String, String> {
        var fullName = ""
        var code = ""
        countryCodeJson.keys().forEach { key ->
            val countryDetails = countryCodeJson.getJSONObject(key)
            if (phoneNumber.startsWith(countryDetails.getString("isdCode"))) {
                code = countryDetails.getString("isdCode")
                fullName = countryDetails.getString("fullName")
            }
        }
        return Pair(fullName, code)
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

    fun postRecommendedInstruments(type: String, instrumentationRef: String, displayName: String) {
        showLoadingInButton()
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
                put("type", type)

                val upiObject = JSONObject().apply {
                    put("instrumentRef", instrumentationRef)
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
                        val bottomSheetFragment = UPITimerBottomSheet.newInstance(displayName)
                        bottomSheetFragment.show(parentFragmentManager, "UPITimerBottomSheet")
                    } else if (status.contains("Approved", ignoreCase = true)) {
                        editor.putString("status", "Success")
                        editor.apply()

                        val bottomSheet = PaymentSuccessfulWithDetailsBottomSheet()
                        bottomSheet.show(
                            parentFragmentManager,
                            "PaymentStatusBottomSheetWithDetails"
                        )
                        enabledButtonsForAllPaymentMethods()
                    }
                }
                hideLoadingInButton()
            },
            Response.ErrorListener { error ->
                // Handle error
                hideLoadingInButton()
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


    fun hideLoadingInButton() {
        binding.progress.visibility = View.INVISIBLE
        binding.proceedtext.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
            )
        )
        binding.proceedtext.visibility = View.VISIBLE
        binding.recommendedProceedButtonRelativeLayout.setBackgroundColor(
            Color.parseColor(
                sharedPreferences.getString(
                    "primaryButtonColor",
                    "#000000"
                )
            )
        )
        binding.recommendedProceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.recommendedProceedButton.isEnabled = true
    }

    private fun parseAndRenderProductSummary(jsonString: String) {
        val container = binding.container

        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val groupArray = jsonArray.getJSONArray(i)
                val horizontalLayout = LinearLayout(container.context).apply {
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
                        else -> Log.w("JSONParsing", "Unknown type: ${item.getString("type")}")
                    }

                    // After processing the first non-"linegap" element, no need to reset `j` again

                }
                // Add the horizontal layout to the container after processing the group
                container.addView(horizontalLayout)
            }

        } catch (e: Exception) {
            Log.e("JSONParsingError", "Error parsing JSON", e)
        }
    }

    private fun addTextView(
        container: LinearLayout,
        item: JSONObject,
        i: Int,
        toAddWeight: Boolean = true
    ) {
        try {
            val textView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    if (toAddWeight) 0 else LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
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
            Log.e("AddTextViewError", "Error adding TextView", e)
        }
    }


    private fun addImageView(container: LinearLayout, item: JSONObject) {
        try {
            val imageView = ImageView(context).apply {
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
            Log.e("AddImageViewError", "Error adding ImageView", e)
        }
    }


    private fun addDividerView(container: LinearLayout, item: JSONObject) {
        try {
            val divider = View(context)
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
            Log.e("AddDividerViewError", "Error adding Divider", e)
        }
    }

    private fun addLineGap(container: LinearLayout, item: JSONObject) {
        try {
            val gap = View(context)
            val params =
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 20)
            gap.layoutParams = params

            // Apply background color if specified
            gap.setBackgroundColor(Color.parseColor(item.optString("background")))

            container.addView(gap)
        } catch (e: Exception) {
            Log.e("AddLineGapError", "Error adding LineGap", e)
        }
    }

    private fun setBackground(container: LinearLayout, item: JSONObject) {
        val color = item.optString("color", "#FFFFFF") // Default to white
        container.setBackgroundColor(Color.parseColor(color))
    }

    private fun addSpace(container: LinearLayout, item: JSONObject) {
        val space = View(context)
        val width = item.optInt("width", 10)
        val weight = item.optInt("weight", 1)
        val params = LayoutParams(width, LayoutParams.MATCH_PARENT, weight.toFloat())
        space.layoutParams = params

        container.addView(space)
    }

    private fun addAccordionView(container: LinearLayout, item: JSONObject) {
        try {
            val accordionLayout = LinearLayout(context)
            accordionLayout.orientation = LinearLayout.VERTICAL
            accordionLayout.layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

            // Header of the Accordion
            val headerLayout = LinearLayout(context)
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
            val contentLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                visibility = View.VISIBLE // Initially visible
            }

            val contentArray = item.optJSONArray("content")
            contentArray?.let {
                for (i in 1 until contentArray.length()) {
                    val groupArray = contentArray.getJSONArray(i)

                    val horizontalLayout = LinearLayout(container.context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setPadding(32, 8, 32, 8)
                    }

                    if (i == 1) {
                        // Add only first two items in the first row
                        val firstRowLayout = LinearLayout(container.context).apply {
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
                                else -> Log.w(
                                    "JSONParsing",
                                    "Unknown type: ${item.getString("type")}"
                                )
                            }
                        }
                        contentLayout.addView(firstRowLayout)
                        val secondRowLayout = LinearLayout(container.context).apply {
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
                                else -> Log.w(
                                    "JSONParsing",
                                    "Unknown type: ${item.getString("type")}"
                                )
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
                                else -> Log.w(
                                    "JSONParsing",
                                    "Unknown type: ${item.getString("type")}"
                                )
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
            Log.e("AddAccordionViewError", "Error adding Accordion View", e)
        }
    }

    private fun addToggleImageView(
        headerLayout: LinearLayout,
        headerItem: JSONObject,
        accordionLayout: LinearLayout
    ) {
        try {
            val toggleImageView = ImageView(context)
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
            Log.e("AddToggleImageViewError", "Error adding toggle image view", e)
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

    fun setContext(context: Context) {
        this.context = context
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
}