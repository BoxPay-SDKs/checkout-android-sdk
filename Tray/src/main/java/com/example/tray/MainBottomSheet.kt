package com.example.tray


import SingletonClass
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.ViewModels.CallBackFunctions
import com.example.tray.ViewModels.OverlayViewModel
import com.example.tray.adapters.OrderSummaryItemsAdapter
import com.example.tray.databinding.FragmentMainBottomSheetBinding
import com.example.tray.dataclasses.WalletDataClass
import com.example.tray.paymentResult.PaymentResultObject
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

internal class MainBottomSheet : BottomSheetDialogFragment() {
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
    private var i = 1
    private var transactionAmount: String? = null
    private var upiAvailable = false
    private var upiCollectMethod = false
    private var upiIntentMethod = false
    private var cardsMethod = false
    private var walletMethods = false
    private var netBankingMethods = false
    private var overLayPresent = false
    private var items = mutableListOf<String>()
    private var imagesUrls = mutableListOf<String>()
    private var prices = mutableListOf<String>()
    private lateinit var Base_Session_API_URL : String
    var queue: RequestQueue? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var callBackFunctions: CallBackFunctions? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        arguments?.let {
//            token = it.getString("token")
//            successScreenFullReferencePath = it.getString("successScreenFullReferencePath")
//        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // Handle the back button press here
        // Dismiss the dialog when the back button is pressed
        removeOverlayFromActivity()
        callFunctionInActivity()
        dismiss()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Notify ViewModel to hide the overlay when dismissed
        Log.d("Overlay", "Bottom sheet dismissed")
        overlayViewModel.setShowOverlay(false)
        super.onDismiss(dialog)
    }

    private fun getAllInstalledApps(packageManager: PackageManager) {
        Log.d("getAllInstalledApps", "here")
        val apps = packageManager.getInstalledApplications(PackageManager.GET_GIDS)

        for (app in apps) {
            val appName = packageManager.getApplicationLabel(app).toString()
            Log.d("all apps", "allApps $appName")

            // Check if the app supports UPI transactions
            val upiIntent = Intent(Intent.ACTION_VIEW)
            upiIntent.data = Uri.parse("upi://pay")
            upiIntent.setPackage(app.packageName)
            val upiApps = packageManager.queryIntentActivities(upiIntent, 0)

            if (appName == "PhonePe") {
                i++;
                Log.d("UPI App", appName)
                Log.d("UPI App Package Name", app.packageName)

                UPIAppsAndPackageMap[appName] = app.packageName
            }

            // If the app can handle the UPI intent, it's a UPI app
            if (!upiApps.isEmpty()) {
                i++;
                Log.d("UPI App", appName)
                Log.d("UPI App Package Name", app.packageName)

                UPIAppsAndPackageMap[appName] = app.packageName
            }

            if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                // apps with launcher intent
                if (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) {
                    // updated system apps
                } else if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                    // system apps
                } else {
                    // user installed apps
                }
            }
        }
    }
    private fun fetchUPIIntentURL(context: Context, appName: String) {
        Log.d(" upiIntent Details launch UPI Payment", appName)
        showLoadingState()
        getUrlForUPIIntent(appName)
    }
    private fun showLoadingState(){
        binding.loadingRelativeLayout.visibility = View.VISIBLE
        binding.boxpayLogoLottie.playAnimation()
    }
    private fun removeLoadingState(){
        binding.loadingRelativeLayout.visibility = View.GONE
        binding.boxpayLogoLottie.cancelAnimation()
    }

    private fun launchUPIIntent(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        Log.d("upiIntent",url)
        val uri = Uri.parse(url)
        intent.data = uri

        try {
            startFunctionCalls()

            startActivity(intent)

            removeLoadingState()
        } catch (e: ActivityNotFoundException) {
            // Handle the case where no activity is found to handle the intent
            Log.d("upiIntent Details Activity Not found",e.toString())
        }
    }
    private fun startFunctionCalls() {
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                fetchStatusAndReason("https://test-apis.boxpay.tech/v0/checkout/sessions/${token}/status")
                // Delay for 5 seconds
                delay(4000)
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
            e.printStackTrace()
            ""
        }
    }
    private fun fetchStatusAndReason(url: String) {
        Log.d("fetching function called correctly", "Fine")
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val status = response.getString("status")
                    val statusReason = response.getString("statusReason")

                    // Do something with status and statusReason
                    // For example, log them
                    Log.d("MainBottomSheet Status", status)
                    Log.d("Status Reason", statusReason)

                    // Check if status is success, if yes, dismiss the bottom sheet
                    if (status.contains(
                            "Approved",
                            ignoreCase = true
                        ) || statusReason.contains(
                            "Received by BoxPay for processing",
                            ignoreCase = true
                        ) || statusReason.contains(
                            "Approved by PSP",
                            ignoreCase = true
                        ) || status.contains("PAID", ignoreCase = true)
                    ) {
                        job?.cancel()
                        binding.payUsingAnyUPIConstraint.isEnabled = true

                        val callback =  SingletonClass.getInstance().getYourObject()
                        if(callback == null){
                            Log.d("call back is null","Success")
                        }else{
                            callback.onPaymentResult(PaymentResultObject("Success"))
                        }
                    } else if (status.contains("PENDING", ignoreCase = true)) {
//                        val bottomSheet = PaymentFailureScreen()
//                        bottomSheet.show(supportFragmentManager,"PaymentFailureBottomSheet")
//                        finish()
                    } else if (status.contains("EXPIRED", ignoreCase = true)) {
                        job?.cancel()
                        binding.payUsingAnyUPIConstraint.isEnabled = true
                    } else if (status.contains("PROCESSING", ignoreCase = true)) {

                    } else if (status.contains("FAILED", ignoreCase = true)) {
                        job?.cancel()
                        binding.payUsingAnyUPIConstraint.isEnabled = true
                        val callback = FailureScreenCallBackSingletonClass.getInstance().getYourObject()
                        if(callback == null){
                            Log.d("callback is null","PaymentFailedWithDetailsSheet")
                        }else{
                            callback.openFailureScreen()
                        }
//                        val bottomSheet = PaymentFailureScreen()
//                        bottomSheet.show(parentFragmentManager,"PaymentFailureBottomSheet")
                    }else{
                        job?.cancel()
                        binding.payUsingAnyUPIConstraint.isEnabled = true

//                        val bottomSheet = PaymentFailureScreen()
//                        bottomSheet.show(parentFragmentManager,"PaymentFailureBottomSheet")
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }) { error ->
            Log.e("Error", "Error occurred: ${error.message}")
            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                val errorResponse = String(error.networkResponse.data)
                Log.e("Error", "Detailed error response: $errorResponse")
            }
            // Handle errors here
        }
        // Add the request to the RequestQueue.
        queue?.add(jsonObjectRequest)
    }

    private fun getUrlForUPIIntent(appName : String) {
        Log.d("postRequestCalled", System.currentTimeMillis().toString())
        val requestQueue = Volley.newRequestQueue(context)


        // Constructing the request body
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

            // Browser Data

            // Get the IP address

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
            }
            put("browserData", browserData)
            val instrumentDetailsObject = JSONObject().apply {
                put("type", "upi/intent")

                val upiAppDetails = JSONObject().apply {
                    put("upiApp", appName)
                    //


                    // Replace with the actual shopper VPA value
                }
                put("upiAppDetails", upiAppDetails)
            }

            // Instrument Details
            put("instrumentDetails", instrumentDetailsObject)
            // Shopper
            val shopperObject = JSONObject().apply {
                val deliveryAddressObject = JSONObject().apply {

                    put("address1", sharedPreferences.getString("address1", "null"))
                    put("address2", sharedPreferences.getString("address2", "null"))
                    put("address3", sharedPreferences.getString("address3", "null"))
                    put("city", sharedPreferences.getString("city", "null"))
                    put("countryCode", sharedPreferences.getString("countryCode", "null"))
                    put("countryName", sharedPreferences.getString("countryName", "null"))
                    put("postalCode", sharedPreferences.getString("postalCode", "null"))
                    put("state", sharedPreferences.getString("state", "null"))

                }


                put("deliveryAddress", deliveryAddressObject)
                put("email", sharedPreferences.getString("email", "null"))
                put("firstName", sharedPreferences.getString("firstName", "null"))
                if (sharedPreferences.getString("gender", "null") == "null")
                    put("gender", JSONObject.NULL)
                else
                    put("gender", sharedPreferences.getString("gender", "null"))
                put("lastName", sharedPreferences.getString("lastName", "null"))
                put("phoneNumber", sharedPreferences.getString("phoneNumber", "null"))
                put("uniqueReference", sharedPreferences.getString("uniqueReference", "null"))
            }
            put("shopper", shopperObject)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->

                // Handle response

                try {
                    logJsonObjectUPIIntent(response)

                    val actionsArray = response.getJSONArray("actions")
                    val urlForIntent = actionsArray.getJSONObject(0).getString("url")

                   val urlInBase64 = urlToBase64(urlForIntent)
                    Log.d("upiIntent Details inside upi Intent call", urlInBase64)
                    launchUPIIntent(urlInBase64)

                } catch (e: JSONException) {
                    Log.d("upiIntent Details status check error", e.toString())
                }

            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
//                    val errorResponse = String(error.networkResponse.data)
//                    Log.e("Error", "Detailed error response: $errorResponse")
//                    binding.ll1InvalidCardNumber.visibility = View.VISIBLE
//                    binding.textView4.text = extractMessageFromErrorResponse(errorResponse)
//                    getMessageForFieldErrorItems(errorResponse)
//                    hideLoadingInButton()
//                    val errorMessage = extractMessageFromErrorResponse(errorResponse).toString()
//                    Log.d("Error message", errorMessage)
//                    if (errorMessage.contains("Session is no longer accepting the payment as payment is already completed",ignoreCase = true)){
//                        binding.textView4.text = "Payment is already done"
//                    }
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
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = FragmentMainBottomSheetBinding.inflate(inflater, container, false)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        queue = Volley.newRequestQueue(requireContext())

        val environmentFetched = sharedPreferences.getString("environment","null")
        Log.d("environment is $environmentFetched","Add UPI ID")
        Base_Session_API_URL = "https://${environmentFetched}-apis.boxpay.tech/v0/checkout/sessions/"


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
        getAndSetOrderDetails()



        fetchAllPaymentMethods()
        val packageManager = requireContext().packageManager
        getAllInstalledApps(packageManager)


        val orderSummaryAdapter = OrderSummaryItemsAdapter(imagesUrls, items, prices)
        binding.itemsInOrderRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.itemsInOrderRecyclerView.adapter = orderSummaryAdapter




        updateTransactionAmountInSharedPreferences("₹" + transactionAmount.toString())

        showUPIOptions()



        // Set click listeners
        var priceBreakUpVisible = false
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
            callFunctionInActivity()
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

        binding.payUsingAnyUPIConstraint.setOnClickListener {

            binding.payUsingAnyUPIConstraint.isEnabled = false
            showLoadingState()
            getUrlForDefaultUPIIntent()
        }


        binding.addNewUPIIDConstraint.setOnClickListener() {
            binding.addNewUPIIDConstraint.isEnabled = false
            openAddUPIIDBottomSheet()
        }

        binding.cardConstraint.setOnClickListener() {
            binding.cardConstraint.isEnabled = false
            openAddCardBottomSheet()
        }
        binding.walletConstraint.setOnClickListener() {
            binding.walletConstraint.isEnabled = false
            openWalletBottomSheet()
        }

        binding.netBankingConstraint.setOnClickListener() {
            binding.netBankingConstraint.isEnabled = false
            openNetBankingBottomSheet()
        }

        populatePopularUPIApps()

        binding.popularUPIAppsConstraint.setOnClickListener {
            // Do nothing , Just for the sake that it doesnt close the UPI options
        }


        return binding.root
    }

    private fun openActivity(activityPath: String, context: Context) {
        if (context is AppCompatActivity) {
            try {
                // Get the class object for the activity using reflection
                val activityClass = Class.forName(activityPath)
                // Create an instance of the activity using Kotlin reflection
                val activityInstance =
                    activityClass.getDeclaredConstructor().newInstance() as AppCompatActivity

                // Check if the activity is a subclass of AppCompatActivity
                if (activityInstance is AppCompatActivity) {
                    // Start the activity
                    context.startActivity(Intent(context, activityClass))
                } else {
                    // Log an error or handle the case where the activity is not a subclass of AppCompatActivity
                }
            } catch (e: ClassNotFoundException) {
                // Log an error or handle the case where the activity class cannot be found
            }
        } else {
            // Log an error or handle the case where the context is not an AppCompatActivity
        }
    }


    private fun fetchAllPaymentMethods() {
        val url = "${Base_Session_API_URL}${token}"

        val jsonObjectAll = JsonObjectRequest(Request.Method.GET, url, null, { response ->

            try {
                logJsonObject(response)

                // Get the payment methods array
                val paymentMethodsArray =
                    response.getJSONObject("configs").getJSONArray("paymentMethods")

                val paymentDetailsObject = response.getJSONObject("paymentDetails")
                val itemsArray = paymentDetailsObject.getJSONObject("order").getJSONArray("items")

                for (i in 0 until itemsArray.length()) {
                    val imageURL = itemsArray.getJSONObject(i).getString("imageUrl")
                    Log.d("imageURL",imageURL)
                    imagesUrls.add(imageURL)
                }

                // Filter payment methods based on type equal to "Wallet"
                for (i in 0 until paymentMethodsArray.length()) {
                    val paymentMethod = paymentMethodsArray.getJSONObject(i)
                    val paymentMethodName = paymentMethod.getString("type")
                    Log.d("paymentMethodName", paymentMethodName)
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
                    }
                    if (paymentMethodName == "Card") {
                        cardsMethod = true
                    }
                    if (paymentMethodName == "Wallet") {
                        walletMethods = true
                    }
                    if (paymentMethodName == "NetBanking") {
                        netBankingMethods = true
                    }
                }
                Log.d("paymentMethods : ", upiAvailable.toString() + cardsMethod.toString())


                if (upiAvailable) {
                    binding.cardView4.visibility = View.VISIBLE
                    if (upiIntentMethod) {
                        binding.payUsingAnyUPIConstraint.visibility = View.VISIBLE
                    }
                    if (upiCollectMethod) {
                        binding.addNewUPIIDConstraint.visibility = View.VISIBLE
                    }
                }
                if (cardsMethod) {
                    binding.cardView5.visibility = View.VISIBLE
                }
                if (walletMethods) {
                    binding.cardView6.visibility = View.VISIBLE
                }

                if (netBankingMethods) {
                    binding.cardView7.visibility = View.VISIBLE
                }


            } catch (e: Exception) {
                Log.d("Error Occurred", e.toString())
                e.printStackTrace()
            }

        }, { error ->

            Log.e("Error", "Error occurred: ${error.message}")
            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                val errorResponse = String(error.networkResponse.data)
                Log.e("Error", " fetching MainBottomSheet error response: $errorResponse")
//                binding.errorField.visibility = View.VISIBLE
//                binding.textView4.text = extractMessageFromErrorResponse(errorResponse)
//                hideLoadingInButton()
            }
        })
        queue?.add(jsonObjectAll)
    }

    fun enabledButtonsForAllPaymentMethods() {
        binding.payUsingAnyUPIConstraint.isEnabled = true
        binding.addNewUPIIDConstraint.isEnabled = true
        binding.cardConstraint.isEnabled = true
        binding.walletConstraint.isEnabled = true
        binding.netBankingConstraint.isEnabled = true
    }


    private fun putTransactionDetailsInSharedPreferences() {
        val sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("token", token)
        Log.d("token added to sharedPreferences", token.toString())
        editor.putString("successScreenFullReferencePath", successScreenFullReferencePath)
        Log.d(
            "success Screen added to sharedPreferences",
            successScreenFullReferencePath.toString()
        )
        editor.apply()
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
            }
            Log.d("i and app inside if statement", "$i and app = PhonePe")
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
            }
            Log.d("i and app inside if statement", "$i and app = GPay")
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
            }
            Log.d("i and app inside if statement", "$i and app = Paytm")
            i++
        }

        if (i == 1) {
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
    private fun openDefaultUPIIntentBottomSheetFromAndroid(url : String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startFunctionCalls()



        removeLoadingState()
        startActivity(intent)
        binding.payUsingAnyUPIConstraint.isEnabled = true
    }
    private fun getUrlForDefaultUPIIntent(){
        Log.d("postRequestCalled", System.currentTimeMillis().toString())
        val requestQueue = Volley.newRequestQueue(context)


        // Constructing the request body
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

            // Browser Data

            // Get the IP address

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
            }
            put("browserData", browserData)
            val instrumentDetailsObject = JSONObject().apply {
                put("type", "upi/intent")

//                val upiAppDetails = JSONObject().apply {
//                    put("upiApp", "PayTm")
//                    /
//                    // Replace with the actual shopper VPA value
//                }
//                put("upiAppDetails", upiAppDetails)
            }

            // Instrument Details
            put("instrumentDetails", instrumentDetailsObject)
            // Shopper
            val shopperObject = JSONObject().apply {
                val deliveryAddressObject = JSONObject().apply {

                    put("address1", sharedPreferences.getString("address1", "null"))
                    put("address2", sharedPreferences.getString("address2", "null"))
                    put("address3", sharedPreferences.getString("address3", "null"))
                    put("city", sharedPreferences.getString("city", "null"))
                    put("countryCode", sharedPreferences.getString("countryCode", "null"))
                    put("countryName", sharedPreferences.getString("countryName", "null"))
                    put("postalCode", sharedPreferences.getString("postalCode", "null"))
                    put("state", sharedPreferences.getString("state", "null"))

                }


                put("deliveryAddress", deliveryAddressObject)
                put("email", sharedPreferences.getString("email", "null"))
                put("firstName", sharedPreferences.getString("firstName", "null"))
                if (sharedPreferences.getString("gender", "null") == "null")
                    put("gender", JSONObject.NULL)
                else
                    put("gender", sharedPreferences.getString("gender", "null"))
                put("lastName", sharedPreferences.getString("lastName", "null"))
                put("phoneNumber", sharedPreferences.getString("phoneNumber", "null"))
                put("uniqueReference", sharedPreferences.getString("uniqueReference", "null"))
            }
            put("shopper", shopperObject)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->

                // Handle response

                try {
                    logJsonObjectUPIIntent(response)

                    val actionsArray = response.getJSONArray("actions")
                    val urlForIntent = actionsArray.getJSONObject(0).getString("url")

                    val urlInBase64 = urlToBase64(urlForIntent)
                    Log.d("upiIntent Details inside upi Intent call", urlInBase64)
                    openDefaultUPIIntentBottomSheetFromAndroid(urlInBase64)

                } catch (e: JSONException) {
                    Log.d("upiIntent Details status check error", e.toString())
                }
            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
//                    val errorResponse = String(error.networkResponse.data)
//                    Log.e("Error", "Detailed error response: $errorResponse")
//                    binding.ll1InvalidCardNumber.visibility = View.VISIBLE
//                    binding.textView4.text = extractMessageFromErrorResponse(errorResponse)
//                    getMessageForFieldErrorItems(errorResponse)
//                    hideLoadingInButton()
//                    val errorMessage = extractMessageFromErrorResponse(errorResponse).toString()
//                    Log.d("Error message", errorMessage)
//                    if (errorMessage.contains("Session is no longer accepting the payment as payment is already completed",ignoreCase = true)){
//                        binding.textView4.text = "Payment is already done"
//                    }
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

    private fun addOverlayToActivity() {
        overLayPresent = true
        Log.d("Overlay", "overlay added......")
        // Create a translucent overlay view
        overlayViewMainBottomSheet = View(requireContext())
        overlayViewMainBottomSheet?.setBackgroundColor(Color.parseColor("#80000000")) // Adjust the color and transparency as needed

        // Get WindowManager from the parent activity's context
        val windowManager =
            requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Set layout parameters for the overlay view
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // Add overlay view to the WindowManager
        windowManager.addView(overlayViewMainBottomSheet, layoutParams)
    }

    private fun removeOverlayFromActivity() {
        // Remove the overlay view from the parent activity
        overlayViewMainBottomSheet?.let {
            val windowManager =
                requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.removeView(it)
        }
        overlayViewMainBottomSheet = null
    }

    // Method to show overlay in the first BottomSheet
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

    // Method to remove overlay from the first BottomSheet
    public fun removeOverlayFromCurrentBottomSheet() {
        overlayViewCurrentBottomSheet?.let {
            // Remove the overlay view directly from the root view
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
            .setDuration(500) // Set the duration of the animation in milliseconds
            .withEndAction {
                // Code to be executed when the animation ends
            }
            .start()
    }

    private fun hidePriceBreakUp() {
        binding.itemsInOrderRecyclerView.visibility = View.GONE
        binding.textView18.visibility = View.GONE
        binding.ItemsPrice.visibility = View.GONE
        binding.priceBreakUpDetailsLinearLayout.visibility = View.GONE
        binding.arrowIcon.animate()
            .rotation(0f)
            .setDuration(500) // Set the duration of the animation in milliseconds
            .withEndAction {
                // Code to be executed when the animation ends
            }
            .start()
    }

    private fun showUPIOptions() {
        binding.upiConstraint.setBackgroundColor(Color.parseColor("#E0F1FF"))
        binding.upiOptionsLinearLayout.visibility = View.VISIBLE
        binding.textView20.typeface =
            ResourcesCompat.getFont(requireContext(), R.font.poppins_semibold)
        Log.d("made visible", i.toString())

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
            .withEndAction {
                // Code to be executed when the animation ends
            }
            .start()
    }

    fun extractSum(prices: MutableList<String>): String {
        var finalSum = 0
        for (price in prices) {

            val numericPart = price.replace("[^0-9]".toRegex(), "")
            if (numericPart.isEmpty()) {
                return 0.toString()
            } else {
                finalSum += numericPart.toInt()
            }
        }

        val formattedSum = String.format("₹%.2f", finalSum / 100.0)
        return formattedSum

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

            if (bottomSheetBehavior == null)
                Log.d("bottomSheetBehavior is null", "check here")

            val screenHeight = requireContext().resources.displayMetrics.heightPixels
            val percentageOfScreenHeight = 0.7 // 70%
            val desiredHeight = (screenHeight * percentageOfScreenHeight).toInt()

//        // Adjust the height of the bottom sheet content view
//        val layoutParams = bottomSheetContent.layoutParams
//        layoutParams.height = desiredHeight
//        bottomSheetContent.layoutParams = layoutParams
            if(bottomSheetBehavior == null)
                Log.d("MainBottomSheet  bottomSheet is null","Main Bottom Sheet")
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
                            dismiss()
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
        val bottomSheetFragment = AddUPIID()
        bottomSheetFragment.show(parentFragmentManager, "AddUPIBottomSheet")
    }

    private fun openAddCardBottomSheet() {
        val bottomSheetFragment =
            AddCardBottomSheet()
        bottomSheetFragment.show(parentFragmentManager, "AddCardBottomSheet")
    }

    private fun openNetBankingBottomSheet() {
        val bottomSheetFragment = NetBankingBottomSheet()
        bottomSheetFragment.show(parentFragmentManager, "NetBankingBottomSheet")
    }

    private fun openWalletBottomSheet() {
        val bottomSheetFragment = WalletBottomSheet()
        bottomSheetFragment.show(parentFragmentManager, "WalletBottomSheet")
    }

    private fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body Main Bottom Sheet", jsonStr)
    }

    private fun logJsonObjectUPIIntent(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("upiIntent call for url", jsonStr)
    }

    private fun getAndSetOrderDetails() {

        val url = "${Base_Session_API_URL}${token}"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObjectAll = JsonObjectRequest(Request.Method.GET, url, null, { response ->

            try {


                val paymentDetailsObject = response.getJSONObject("paymentDetails")
                val totalAmount = paymentDetailsObject.getJSONObject("money").getString("amount")
                val orderObject = paymentDetailsObject.getJSONObject("order")
                val originalAmount = orderObject.getString("originalAmount")
                val shippingCharges = orderObject.getString("shippingAmount")
                val taxes = orderObject.getString("taxAmount")

                transactionAmount = totalAmount
                val itemsArray = orderObject.getJSONArray("items")
                var totalQuantity = 0
                for (i in 0 until itemsArray.length()) {
                    val itemObject = itemsArray.getJSONObject(i)
                    items.add(itemObject.getString("itemName"))
                    prices.add(itemObject.getString("amountWithoutTaxLocale"))
                    val quantity = itemObject.getInt("quantity")
                    totalQuantity += quantity
                }


                Log.d("order details subtotal", originalAmount)
                Log.d("order details taxes",taxes.toString())
                Log.d("order details shipping charges",shippingCharges.toString())
                Log.d("order details subtotal",originalAmount.toString())
                transactionAmount = totalAmount.toString()

                binding.unopenedTotalValue.text = "₹${totalAmount}"
                if (totalQuantity == 1)
                    binding.numberOfItems.text = "${totalQuantity} item"
                else
                    binding.numberOfItems.text = "${totalQuantity} items"
                binding.ItemsPrice.text = "₹${totalAmount}"



                if(originalAmount != totalAmount) {
                    binding.subtotalTextView.text = "₹${originalAmount}"
                    binding.subTotalRelativeLayout.visibility = View.VISIBLE
                }

                if(taxes != "null"){
                    binding.taxTextView.text = "₹${taxes}"
                    binding.taxesRelativeLayout.visibility = View.VISIBLE
                }

                if(shippingCharges != "null"){
                    binding.shippingChargesTextView.text = "₹${shippingCharges}"
                    binding.shippingChargesRelativeLayout.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.d("Error Occurred in MainBottomSheet", e.toString())
                e.printStackTrace()
            }

        }, { error ->

            Log.e("Error", "Error occurred: ${error.message}")
            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                val errorResponse = String(error.networkResponse.data)
                Log.e("Error", " fetching wallets error response: $errorResponse")
            }
        })
        queue.add(jsonObjectAll)
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences =
            requireContext().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token", "empty")
        Log.d("data fetched from sharedPreferences", token.toString())
        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
        Log.d(
            "success screen path fetched from sharedPreferences",
            successScreenFullReferencePath.toString()
        )
    }


    private fun updateTransactionAmountInSharedPreferences(transactionAmountArgs: String) {
        val sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("transactionAmount", transactionAmountArgs)
        editor.apply()
    }


    //To enable proceed button in check activity
    private fun callFunctionInActivity() {
        val activity = activity
        if (activity is Check) {
            activity.removeLoadingAndEnabledProceedButton()
        }
    }


    companion object {

    }
}