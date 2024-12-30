package com.boxpay.checkout.sdk

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieDrawable
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.databinding.FragmentUpiComponentAloneBinding
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.boxpay.checkout.sdk.util.CommonFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

class BoxPayUpiComponent(
    val token: String?,
    val sandboxEnabled: Boolean?,
    val onPaymentResult: ((PaymentResultObject) -> Unit)
) : Fragment() {
    private var UPIAppsAndPackageMap: MutableMap<String, String> = mutableMapOf()
    private var testEnvironment: Boolean = false
    private var BASE_URL = ""
    private lateinit var binding: FragmentUpiComponentAloneBinding
    private var selectedColor = ""
    private var showProceedButton = true
    private var selectedTextColor = ""
    private var totalAmount = ""
    private var email: String? = null
    private var firstName: String? = null
    private var lastName: String? = null
    private var gender: String? = null
    private var sessionTimer: CountDownTimer? = null
    private var phoneNumber: String? = null
    private var uniqueReference: String? = null
    private var dob: String? = null
    private var panNumber: String? = null
    private var address1: String? = null
    private var address2: String? = null
    private var city: String? = null
    private var state: String? = null
    private var countryCode: String? = null
    private var postalCode: String? = null
    private var countryName: String? = null
    private var context: Context? = null
    private var job: Job? = null
    var isGpayReturned = false
    var isOthersReturned = false
    var isPhonePe = false
    var selectedUpiIntent = ""
    var isPaytmReturned = false
    var upiCollectId: String? = null
    private lateinit var inputMethodManager: InputMethodManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun setContext(context: Context) {
        this.context = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUpiComponentAloneBinding.inflate(inflater, container, false)
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        var sessionUrl = ""
        if (sandboxEnabled == true) {
            sessionUrl = "sandbox-apis.boxpay.tech"
        } else if (testEnvironment) {
            sessionUrl = "test-apis.boxpay.tech"
        } else {
            sessionUrl = "apis.boxpay.in"
        }
        this.BASE_URL = "https://${sessionUrl}/v0/checkout/sessions/"
        makeSessionDataCall()
        coroutineScope.launch {
            val packageManager = context!!.packageManager
            getAllInstalledApps(packageManager)
        }
        inputMethodManager =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        binding.proceedButton.setOnClickListener {
            onProceedPayment()
        }

        binding.addNewUpiTextInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textNow = s.toString()
                if (textNow.isNotBlank() && textNow.matches(Regex("[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{3,64}"))) {
                    enableProceedButton()
                } else {
                    disableProceedButton()
                    if (textNow.contains('@') && (textNow.split('@').getOrNull(1)?.length
                            ?: 0) >= 2
                    ) {
                        binding.textView8.text = "Please enter a valid UPI Id"
                        binding.invalidCVV.visibility = View.VISIBLE // Show specific error
                    } else {
                        binding.invalidCVV.visibility =
                            View.GONE // Hide error if not matching condition
                    }
                }
                upiCollectId = textNow
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })

        binding.addUpiIdProceedButton.setOnClickListener {
            if (upiCollectId.isNullOrEmpty()) {
                binding.textView8.text = "Please enter your UPI ID first"
                binding.invalidCVV.visibility = View.VISIBLE
            } else if (!upiCollectId.isNullOrEmpty() && !upiCollectId!!.matches(Regex("[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{3,64}"))) {
                binding.textView8.text = "Please enter a valid UPI Id"
                binding.invalidCVV.visibility = View.VISIBLE
            } else {
                postRequest()
            }
        }

        binding.addNewUpiId.setOnClickListener {
            if (binding.addNewUpiTextInputLayout.isVisible) {
                disableAddUpiIdClick()
            } else {
                binding.addNewUpiId.setBackgroundResource(R.drawable.add_new_upi_id_enabled_background)
                binding.imageView13.rotation = 180f
                selectedUpiIntent = ""
                binding.dashedLine1.visibility = View.INVISIBLE
                binding.proceedButton.visibility = View.GONE
                resetClickToDefault()
                binding.addUpiIdTextView6.text =
                    "Verify & Pay $totalAmount"
                binding.addNewUpiTextInputLayout.visibility = View.VISIBLE
                binding.addUpiIdProceedButton.visibility = if (showProceedButton) View.VISIBLE else View.GONE
                binding.addUpiIdProceedButton.isEnabled = false
                binding.addNewUpiTextInput.requestFocus()
                inputMethodManager.showSoftInput(
                    binding.addNewUpiTextInput,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }
        }
        return binding.root
    }

    fun onProceedPayment() {
        getUrlForUPIIntent(selectedUpiIntent)
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

    private fun populatePopularUPIApps() {
        var i = 1
        if (UPIAppsAndPackageMap.containsKey("PhonePe")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setBackgroundResource(R.drawable.popular_item_unselected_bg)
            imageView.setImageResource(R.drawable.phonepe_logo)
            textView.text = "PhonePe"
            val layout = getPopularConstraintLayoutByNum(i)
            layout.visibility = View.VISIBLE
            layout.setOnClickListener() {
                onClickUpiIntent("PhonePe",imageView)
                if (binding.addNewUpiTextInputLayout.isVisible) {
                    disableAddUpiIdClick()
                }
            }

            i++
        }


        if (UPIAppsAndPackageMap.containsKey("GPay")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setBackgroundResource(R.drawable.popular_item_unselected_bg)
            imageView.setImageResource(R.drawable.google_pay_seeklogo)
            textView.text = "GPay"
            val layout = getPopularConstraintLayoutByNum(i)
            layout.visibility = View.VISIBLE
            layout.setOnClickListener() {
                onClickUpiIntent("GPay",imageView)
                if (binding.addNewUpiTextInputLayout.isVisible) {
                    disableAddUpiIdClick()
                }
            }

            i++
        }


        if (UPIAppsAndPackageMap.containsKey("Paytm")) {
            val imageView = getPopularImageViewByNum(i)
            val textView = getPopularTextViewByNum(i)
            imageView.setBackgroundResource(R.drawable.popular_item_unselected_bg)
            imageView.setImageResource(R.drawable.paytm_upi_logo)
            textView.text = "Paytm"
            val layout = getPopularConstraintLayoutByNum(i)
            layout.visibility = View.VISIBLE
            layout.setOnClickListener() {
                onClickUpiIntent("PayTm",imageView)
                if (binding.addNewUpiTextInputLayout.isVisible) {
                    disableAddUpiIdClick()
                }
            }

            i++
        }

        val imageView = getPopularImageViewByNum(i)
        val textView = getPopularTextViewByNum(i)
        imageView.setBackgroundResource(R.drawable.popular_item_unselected_bg)
        imageView.setImageResource(R.drawable.ic_other_intent)
        textView.text = "Others"
        val layout = getPopularConstraintLayoutByNum(i)
        layout.visibility = View.VISIBLE
        layout.setOnClickListener() {
            resetClickToDefault()
            binding.proceedButton.visibility = View.GONE
            if (binding.addNewUpiTextInputLayout.isVisible) {
                disableAddUpiIdClick()
            }
            getUrlForDefaultUPIIntent()
        }

        if (i == 1 || i < 1) {
            binding.popularUPIAppsConstraint.visibility = View.GONE
        }
    }

    fun onClickUpiIntent(selected: String,imageView: ImageView) {
        resetClickToDefault()
        selectedUpiIntent = selected
        imageView.setBackgroundResource(R.drawable.selected_popular_item_bg)
        val shapeDrawable = imageView.background as? GradientDrawable
        shapeDrawable?.setStroke(
            3, Color.parseColor(
                selectedColor
            )
        )
        binding.textView6.text =
            "Pay $totalAmount via $selectedUpiIntent"
        binding.proceedButton.visibility = if (showProceedButton) View.VISIBLE else View.GONE
    }

    private fun resetClickToDefault() {
        if (binding.PopularUPILinearLayout1.isVisible) {
            binding.popularUPIImageView1.setBackgroundResource(R.drawable.popular_item_unselected_bg)
        }
        if (binding.PopularUPILinearLayout2.isVisible) {
            binding.popularUPIImageView2.setBackgroundResource(R.drawable.popular_item_unselected_bg)
        }
        if (binding.PopularUPILinearLayout3.isVisible) {
            binding.popularUPIImageView3.setBackgroundResource(R.drawable.popular_item_unselected_bg)
        }
        if (binding.PopularUPILinearLayout4.isVisible) {
            binding.popularUPIImageView4.setBackgroundResource(R.drawable.popular_item_unselected_bg)
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

    private fun getPopularTextViewByNum(num: Int): TextView {
        return when (num) {
            1 -> binding.popularUPITextView1
            2 -> binding.popularUPITextView2
            3 -> binding.popularUPITextView3
            4 -> binding.popularUPITextView4
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

    private fun makeSessionDataCall() {
        showLoadingState()

        val url = "${BASE_URL}${token}"
        val queue: RequestQueue = Volley.newRequestQueue(context)
        val jsonObjectAll = @RequiresApi(Build.VERSION_CODES.O)
        object : JsonObjectRequest(Method.GET, url, null, { response ->

            try {
                val status = response.getString("status")
                val expireTiming = response.getString("sessionExpiryTimestamp")
                startCountdown(expireTiming)
                if (status.equals(
                        "Approved",
                        ignoreCase = true
                    ) || status.equals("paid", true)
                ) {
                    onPaymentResult?.let {
                        it(
                            PaymentResultObject(
                                resultFetched = "Success",
                                transactionIdFetched = "",
                                operationIdFetched = ""
                            )
                        )
                    }
                }
                if (status.equals(
                        "expired",
                        ignoreCase = true
                    )
                ) {
                    onPaymentResult?.let {
                        it(
                            PaymentResultObject(
                                resultFetched = "Expired",
                                transactionIdFetched = "",
                                operationIdFetched = ""
                            )
                        )
                    }
                }
                val jsonString = readJsonFromAssets(context!!, "countryCodes.json")
                val countryCodeJson = JSONObject(jsonString)
                val merchantDetailsObject = response.getJSONObject("merchantDetails")
                val checkoutThemeObject = merchantDetailsObject.getJSONObject("checkoutTheme")
                selectedColor = checkoutThemeObject.getString("headerColor")
                selectedTextColor = checkoutThemeObject.getString("buttonTextColor")
                binding.proceedButtonRelativeLayout.setBackgroundColor(
                    Color.parseColor(selectedColor)
                )
                binding.textView6.setTextColor(
                    Color.parseColor(
                        selectedTextColor
                    )
                )
                val paymentDetailsObject = response.getJSONObject("paymentDetails")
                val shopperObject = paymentDetailsObject.getJSONObject("shopper")
                email = shopperObject.optString("email")
                firstName = shopperObject.optString("firstName")
                lastName = shopperObject.optString("lastName")
                gender = shopperObject.optString("gender")
                phoneNumber = shopperObject.optString("phoneNumber")
                uniqueReference = shopperObject.optString("uniqueReference")
                val deliveryAddress = shopperObject.optJSONObject("deliveryAddress")
                if (deliveryAddress != null) {
                    address1 = deliveryAddress.optString("address1")
                    address2 = deliveryAddress.optString("address2")
                    city = deliveryAddress.optString("city")
                    state = deliveryAddress.optString("state")
                    countryCode = deliveryAddress.optString("countryCode")
                    countryName = getCountryName(countryCodeJson, countryCode ?: "IN")
                    postalCode = deliveryAddress.optString("postalCode")
                }
                panNumber = shopperObject.optString("panNumber")

                dob = if (shopperObject.getString("dateOfBirth") != null && shopperObject.getString("dateOfBirth") != "null") {
                    CommonFunctions.formatToISO8601WithCurrentTime(shopperObject.optString("dateOfBirth"))
                } else {
                    null
                }

                val money = paymentDetailsObject.getJSONObject("money").getString("amount")
                val amount = money.toDouble()
                var currencySymbol =
                    paymentDetailsObject.getJSONObject("money").getString("currencySymbol")
                totalAmount =
                    "$currencySymbol${NumberFormat.getNumberInstance(Locale.US).format(amount)}"
                binding.textView26.setTextColor(
                    Color.parseColor(selectedColor)
                )
                binding.imageView15.setColorFilter(
                    Color.parseColor(selectedColor)
                )
                val paymentMethodsArray =
                    response.getJSONObject("configs").getJSONArray("paymentMethods")
                var upiIntentVisible = false
                var upiCollectVisible = false
                for (i in 0 until paymentMethodsArray.length()) {
                    val paymentMethod = paymentMethodsArray.getJSONObject(i)
                    val paymentMethodName = paymentMethod.getString("type")
                    if (paymentMethodName == "Upi") {
                        val brand = paymentMethod.getString("brand")
                        if (brand == "UpiCollect") {
                            upiCollectVisible = true
                        }
                        if (brand == "UpiIntent") {
                            upiIntentVisible = true
                        }
                    }
                }

                if (!upiIntentVisible && !upiCollectVisible) {
                    binding.boxpayUpiAlone.visibility = View.GONE
                    binding.errorlayout.visibility = View.VISIBLE
                }

                if (upiIntentVisible) {
                    binding.popularUPIAppsConstraint.visibility = View.VISIBLE
                    binding.dashedLine1.visibility = View.VISIBLE
                }

                if (upiCollectVisible) {
                    binding.addNewUPIIDConstraint.visibility = View.VISIBLE
                }

                removeLoadingState()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Invalid token/selected environment.\nPlease press back button and try again",
                    Toast.LENGTH_LONG
                ).show()
            }

        }, Response.ErrorListener { error ->
            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                val errorResponse = String(error.networkResponse.data)

            }
        }) {
            // no op
        }
        queue.add(jsonObjectAll)
    }

    fun setTestEnv(testEnv: Boolean) {
        testEnvironment = testEnv
    }


    private fun showLoadingState() {
        binding.boxpayLogoLottie.apply {
            playAnimation()
            repeatCount = LottieDrawable.INFINITE // This makes the animation repeat infinitely
        }
        binding.boxpayUpiAlone.visibility = View.GONE
        binding.boxpayLoader.visibility = View.VISIBLE
    }

    private fun removeLoadingState() {
        binding.boxpayLoader.visibility = View.GONE
        binding.boxpayUpiAlone.visibility = View.VISIBLE
        binding.boxpayLogoLottie.cancelAnimation()
    }

    private fun getUrlForDefaultUPIIntent() {
        showLoadingState()

        val requestQueue = Volley.newRequestQueue(context)


        // Constructing the request body
        val requestBody = JSONObject().apply {
            val browserData = JSONObject().apply {
                val userAgentHeader = WebSettings.getDefaultUserAgent(context)
                val displayMetrics = resources.displayMetrics
                put("screenHeight", displayMetrics.heightPixels.toString())
                put("screenWidth", displayMetrics.widthPixels.toString())
                put("acceptHeader", "application/json")
                put("userAgentHeader", userAgentHeader)
                put("browserLanguage", Locale.getDefault().toString())
                put("javaEnabled", true) // Example value
                put("packageId", context!!.packageName)// Example value
            }
            put("browserData", browserData)

            val instrumentDetailsObject = JSONObject().apply {
                put("type", "upi/intent")
            }
            put("instrumentDetails", instrumentDetailsObject)

            val shopperObject = JSONObject().apply {
                put("email", email)
                put("firstName", firstName)
                put("gender", gender)
                put("lastName", lastName)
                put("phoneNumber", phoneNumber)
                put("uniqueReference", uniqueReference)
                put("dateOfBirth", dob)
                put("panNumber", panNumber)

                val deliveryAddressObject = JSONObject().apply {

                    put("address1", address1)
                    put("address2", address2)
                    put("city", city)
                    put("countryCode", countryCode)
                    put("postalCode", postalCode)
                    put("state", state)
                    put("city", city)
                    put("countryName", countryName)

                }
                put("deliveryAddress", deliveryAddressObject)
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
            Method.POST, BASE_URL + token, requestBody,
            Response.Listener { response ->

                // Handle response

                try {
                    val actionsArray = response.getJSONArray("actions")
                    val urlForIntent = actionsArray.getJSONObject(0).getString("url")

                    val urlInBase64 = urlToBase64(urlForIntent)
                    openDefaultUPIIntentBottomSheetFromAndroid(urlInBase64)

                } catch (e: JSONException) {

                    removeLoadingState()

                }
            },
            Response.ErrorListener { /* no response handling */error ->
                removeLoadingState()
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    onPaymentResult?.let {
                        it(
                            PaymentResultObject(
                                resultFetched = "Failed",
                                transactionIdFetched = "",
                                operationIdFetched = ""
                            )
                        )
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

    private fun getCountryName(
        countryCodeJson: JSONObject,
        countryCode: String
    ): String? {
        countryCodeJson.keys().forEach { key ->
            val countryDetails = countryCodeJson.getJSONObject(key)
            if (countryCode.equals(key, true)) {
                return countryDetails.getString("fullName")
            }
        }
        return null
    }

    private fun readJsonFromAssets(context: Context, fileName: String): String {
        val assetManager = context.assets
        val inputStream = assetManager.open(fileName)
        val bufferedReader = inputStream.bufferedReader()
        return bufferedReader.use { it.readText() }
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

    private fun openDefaultUPIIntentBottomSheetFromAndroid(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startFunctionCalls()
            startActivityForResult(intent, 124)
        } catch (_: Exception) {
            removeLoadingState()
            job?.cancel()
            Toast.makeText(context, "No other UPI options", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startFunctionCalls() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(3000)
                fetchStatusAndReason("${BASE_URL}${token}/status")
                // Delay for 4 seconds
            }
        }
    }

    fun generateRandomAlphanumericString(length: Int): String {
        val charPool: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun fetchStatusAndReason(url: String) {
        val requestQueue = Volley.newRequestQueue(context)
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.GET, url, null,
            Response.Listener { response ->
                try {
                    val status = response.getString("status")
                    val transactionId = response.getString("transactionId").toString()
                    if (status.equals("Pending", ignoreCase = true) && isGpayReturned) {
                        removeLoadingState()
                        job?.cancel()
                        isGpayReturned = false
                        onPaymentResult?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = "Failed",
                                    transactionIdFetched = transactionId,
                                    operationIdFetched = transactionId
                                )
                            )
                        }
                    }
                    if (status.equals("Pending", ignoreCase = true) && isPhonePe) {
                        removeLoadingState()
                        job?.cancel()
                        isPhonePe = false
                        onPaymentResult?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = "Failed",
                                    transactionIdFetched = transactionId,
                                    operationIdFetched = transactionId
                                )
                            )
                        }
                    }
                    if (status.equals("Pending", ignoreCase = true) && isOthersReturned) {
                        removeLoadingState()
                        job?.cancel()
                        isOthersReturned = false
                        onPaymentResult?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = "Failed",
                                    transactionIdFetched = transactionId,
                                    operationIdFetched = transactionId
                                )
                            )
                        }
                    }
                    if (status.equals("Pending", ignoreCase = true) && isPaytmReturned) {
                        removeLoadingState()
                        job?.cancel()
                        isPaytmReturned = false
                        onPaymentResult?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = "Failed",
                                    transactionIdFetched = transactionId,
                                    operationIdFetched = transactionId
                                )
                            )
                        }
                    }

                    if (status.equals("Rejected", ignoreCase = true) || status.equals(
                            "failed",
                            true
                        )
                    ) {
                        job?.cancel()
                        onPaymentResult?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = "Failed",
                                    transactionIdFetched = transactionId,
                                    operationIdFetched = transactionId
                                )
                            )
                        }
                    } else {
                        if (status.equals("RequiresAction", ignoreCase = true)) {
                            job?.cancel()
                            onPaymentResult?.let {
                                it(
                                    PaymentResultObject(
                                        resultFetched = status,
                                        transactionIdFetched = transactionId,
                                        operationIdFetched = transactionId
                                    )
                                )
                            }
                        } else if (status.equals(
                                "Approved",
                                ignoreCase = true
                            ) || status.equals("paid", true)
                        ) {
                            job?.cancel()
                            onPaymentResult?.let {
                                it(
                                    PaymentResultObject(
                                        resultFetched = "Success",
                                        transactionIdFetched = transactionId,
                                        operationIdFetched = transactionId
                                    )
                                )
                            }
                        }
                    }
                } catch (_: JSONException) {

                }
            },
            Response.ErrorListener { error ->
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    onPaymentResult?.let {
                        it(
                            PaymentResultObject(
                                resultFetched = "Failed",
                                transactionIdFetched = "",
                                operationIdFetched = ""
                            )
                        )
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
        requestQueue.add(jsonObjectRequest)
    }

    private fun getUrlForUPIIntent(appName: String) {
        showLoadingState()

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
                put("email", email)
                put("firstName", firstName)
                put("gender", gender)
                put("lastName", lastName)
                put("phoneNumber", phoneNumber)
                put("uniqueReference", uniqueReference)
                put("dateOfBirth", dob)
                put("panNumber", panNumber)
                val deliveryAddressObject = JSONObject().apply {

                    put("address1", address1)
                    put("address2", address2)
                    put("city", city)
                    put("countryCode", countryCode)
                    put("postalCode", postalCode)
                    put("state", state)
                    put("city", city)
                    put("countryName", countryName)

                }
                put("deliveryAddress", deliveryAddressObject)
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
            Method.POST, BASE_URL + token, requestBody,
            Response.Listener { response ->

                try {

                    val actionsArray = response.getJSONArray("actions")
                    val urlForIntent = actionsArray.getJSONObject(0).getString("url")
                    val status = response.getJSONObject("status").getString("status")
                    val reason = response.getJSONObject("status").getString("reason")
                    val reasonCode = response.getJSONObject("status").getString("reasonCode")

                    if (status.contains("rejected", ignoreCase = true)) {
                        removeLoadingState()

                    }
                    val urlInBase64 = urlToBase64(urlForIntent)
                    launchUPIIntent(urlInBase64)
                } catch (e: JSONException) {
                    removeLoadingState()

                }
            },
            Response.ErrorListener { error ->

                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    onPaymentResult?.let {
                        it(
                            PaymentResultObject(
                                resultFetched = "Failed",
                                transactionIdFetched = "",
                                operationIdFetched = ""
                            )
                        )
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

            startFunctionCalls()
            startActivityForResult(intent, resultCode)

        } catch (e: ActivityNotFoundException) {
            // Log specific error if the app is not found

            removeLoadingState()

        } catch (e: Exception) {
            // Log any other error that occurs

            removeLoadingState()

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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

    fun setProceedButtonVisibility(visible: Boolean) {
        showProceedButton = visible
    }

    fun onClickProceed() {
        if (selectedUpiIntent.isNotEmpty()) {
            getUrlForUPIIntent(selectedUpiIntent)
        } else {
            postRequest()
        }
    }

    private fun postRequest() {
        showLoadingState()
        val requestQueue = Volley.newRequestQueue(context)
        val requestBody = JSONObject().apply {
            val browserData = JSONObject().apply {
                val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())

                // Get the screen height and width
                val displayMetrics = resources.displayMetrics
                put("screenHeight", displayMetrics.heightPixels.toString())
                put("screenWidth", displayMetrics.widthPixels.toString())
                put("acceptHeader", "application/json")
                put("userAgentHeader", userAgentHeader)
                put("browserLanguage", Locale.getDefault().toString())
                put("javaEnabled", true) // Example value
                put("packageId", requireActivity().packageName)
            }
            put("browserData", browserData)

            val instrumentDetailsObject = JSONObject().apply {
                put("type", "upi/collect")

                val upiObject = JSONObject().apply {
                    put("shopperVpa", upiCollectId)
                }
                put("upi", upiObject)
            }
            put("instrumentDetails", instrumentDetailsObject)

            val shopperObject = JSONObject().apply {
                put("email", email)
                put("firstName", firstName)
                put("gender", gender)
                put("lastName", lastName)
                put("phoneNumber", phoneNumber)
                put("uniqueReference", uniqueReference)
                put("dateOfBirth", dob)
                put("panNumber", panNumber)

                val deliveryAddressObject = JSONObject().apply {

                    put("address1", address1)
                    put("address2", address2)
                    put("city", city)
                    put("countryCode", countryCode)
                    put("postalCode", postalCode)
                    put("state", state)
                    put("city", city)
                    put("countryName", countryName)

                }
                put("deliveryAddress", deliveryAddressObject)
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
            Method.POST, BASE_URL + token, requestBody,
            Response.Listener { response ->

                val status = response.getJSONObject("status").getString("status")
                val reason = response.getJSONObject("status").getString("reason")
                val reasonCode = response.getJSONObject("status").getString("reasonCode")
                val transactionId = response.getString("transactionId").toString()

                if (status.contains("Rejected", ignoreCase = true)) {
                    var cleanedMessage = reason.substringAfter(":")
                    if (cleanedMessage.contains("virtual address", true)) {
                        cleanedMessage = "Invalid UPI Id"
                    } else if (!reasonCode.startsWith("uf", true)) {
                        cleanedMessage =
                            "Please retry using other payment method or try again in sometime"
                    }
                    Toast.makeText(context, cleanedMessage, Toast.LENGTH_SHORT).show()
                    onPaymentResult?.let {
                        it(
                            PaymentResultObject(
                                resultFetched = "Failed",
                                transactionIdFetched = "",
                                operationIdFetched = ""
                            )
                        )
                    }
                    removeLoadingState()
                } else {
                    if (status.contains("RequiresAction", ignoreCase = true)) {
                        onPaymentResult?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = status,
                                    transactionIdFetched = "",
                                    operationIdFetched = ""
                                )
                            )
                        }
                        openUPITimerBottomSheet()
                    } else if (status.contains("Approved", ignoreCase = true)) {
                        onPaymentResult?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = "Success",
                                    transactionIdFetched = transactionId,
                                    operationIdFetched = transactionId
                                )
                            )
                        }
                        removeLoadingState()
                    }
                }
            },
            Response.ErrorListener { error ->
                // Handle error
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    onPaymentResult?.let {
                        it(
                            PaymentResultObject(
                                resultFetched = "Failed",
                                transactionIdFetched = "",
                                operationIdFetched = ""
                            )
                        )
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

        // Add the request to the RequestQueue.
        requestQueue.add(jsonObjectRequest)
    }


    private fun openUPITimerBottomSheet() {
        val bottomSheetFragment = OnlyUPITimerBottomSheet.newInstance(upiCollectId)
        bottomSheetFragment.setCallbackFunction(::onUpiTimerCallback, BASE_URL ?: "", token ?: "")
        parentFragmentManager.beginTransaction()
            .add(bottomSheetFragment, "OnlyUPITimerBottomSheet")
            .commitAllowingStateLoss()
    }

    private fun onUpiTimerCallback(result: PaymentResultObject) {
        removeLoadingState()
        onPaymentResult?.let {
            it(
                PaymentResultObject(
                    resultFetched = result.status ?: "",
                    transactionIdFetched = result.transactionId ?: "",
                    operationIdFetched = result.operationId ?: ""
                )
            )
        }
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

                    }

                    override fun onFinish() {
                        onPaymentResult?.let {
                            it(
                                PaymentResultObject(
                                    resultFetched = "Expired",
                                    transactionIdFetched = "",
                                    operationIdFetched = ""
                                )
                            )
                        }
                    }
                }
                sessionTimer?.start()
            }
        } catch (_: Exception) {
            // no op
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionTimer?.cancel()
    }

    private fun enableProceedButton() {
        binding.addNewUpiProceedButtonRelativeLayout.isEnabled = true
        binding.addUpiIdProceedButton.isEnabled = true
        binding.addNewUpiProceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.addNewUpiProceedButtonRelativeLayout.setBackgroundColor(
            Color.parseColor(
                selectedColor
            )
        )
        binding.invalidCVV.visibility = View.GONE
        binding.addUpiIdTextView6.setTextColor(Color.parseColor(
            selectedTextColor
        ))
    }

    private fun disableProceedButton() {
        binding.addUpiIdTextView6.visibility = View.VISIBLE
        binding.addUpiIdProceedButton.isEnabled = false
        binding.addNewUpiProceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
        binding.addUpiIdProceedButton.setBackgroundResource(R.drawable.disable_button)
        binding.addUpiIdTextView6.setTextColor(Color.parseColor("#ADACB0"))
    }

    fun disableAddUpiIdClick() {
        binding.addNewUpiId.setBackgroundResource(0)
        binding.imageView13.rotation = 0f
        binding.addNewUpiTextInputLayout.visibility = View.GONE
        binding.dashedLine1.visibility = View.VISIBLE
        binding.addUpiIdProceedButton.visibility = View.GONE
        inputMethodManager.hideSoftInputFromWindow(binding.addNewUpiTextInput.windowToken, 0)
    }
}
