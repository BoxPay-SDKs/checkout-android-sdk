package com.boxpay.checkout.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.ViewModels.SharedViewModel
import com.boxpay.checkout.sdk.databinding.ActivityOtpscreenWebViewBinding
import com.boxpay.checkout.sdk.interfaces.OnWebViewCloseListener
import com.boxpay.checkout.sdk.interfaces.UpdateMainBottomSheetInterface
import com.google.android.gms.auth.api.phone.SmsRetriever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONException
import java.util.regex.Pattern
import kotlin.random.Random


internal class OTPScreenWebView() : AppCompatActivity() {
    private val binding by lazy {
        ActivityOtpscreenWebViewBinding.inflate(layoutInflater)
    }

    private var callbackForDismissingMainSheet: UpdateMainBottomSheetInterface? = null
    val permissionReceive = Manifest.permission.RECEIVE_SMS
    val permissionRead = Manifest.permission.READ_SMS
    val smsVerificationReceiver = SmsReceiver()
    private var webViewCloseListener: OnWebViewCloseListener? = null
    private var job: Job? = null
    private var jobForFetchingSMS: Job? = null
    var isBottomSheetShown = false
    private var token: String? = null
    private lateinit var requestQueue: RequestQueue
    private var successScreenFullReferencePath: String? = null
    private var previousBottomSheet: Context? = null
    private lateinit var Base_Session_API_URL: String
    private lateinit var sharedViewModel: SharedViewModel
    private var delay = 4000L
    private val handler = Handler()
    private val delayMillis = 4000L
    private val SMS_CONSENT_REQUEST = 1010
    private var otpFetched: String? = null
    private var startedCallsForOTPInject = false


    fun explicitDismiss() {
        val resultIntent = Intent()
        resultIntent.putExtra("closed", "Your Result Data")
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    @SuppressLint("SetJavaScriptEnabled")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        sharedViewModel.dismissBottomSheetEvent.observe(this) { dismissed ->
            if (dismissed) {
                explicitDismiss()
                sharedViewModel.bottomSheetDismissed()
            }
            isBottomSheetShown = false
        }

        val sharedPreferences =
            this.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val baseUrl = sharedPreferences.getString("baseUrl", "null")
        Base_Session_API_URL = "https://${baseUrl}/v0/checkout/sessions/"

        requestQueue = Volley.newRequestQueue(this)
        val receivedType = intent.getStringExtra("type")
        val receivedUrl = intent.getStringExtra("url")

        if (receivedType?.contains("html", true) == true) {
            val htmlUrl = receivedUrl?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                ?.replace("\\n", "\n")
                ?.replace("\\/", "/") ?: ""

            binding.webViewForOtpValidation.loadDataWithBaseURL(null,htmlUrl, "text/html", "UTF-8", null)
        } else {
            binding.webViewForOtpValidation.loadUrl(receivedUrl.toString())
        }

        binding.webViewForOtpValidation.settings.domStorageEnabled = true
        binding.webViewForOtpValidation.settings.javaScriptEnabled = true

        startFunctionCalls()
        fetchTransactionDetailsFromSharedPreferences()

        Handler(Looper.getMainLooper()).postDelayed({
            if (ContextCompat.checkSelfPermission(
                    this,
                    permissionReceive
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(permissionReceive), 101)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    permissionRead
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(permissionRead), 101)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    permissionRead
                ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this,
                    permissionReceive
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
                registerReceiver(
                    smsVerificationReceiver,
                    intentFilter,
                    RECEIVER_VISIBLE_TO_INSTANT_APPS
                )

                readSms()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permissionRead, permissionReceive),
                    101
                )
            }


            jobForFetchingSMS = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    readSms()
                    // Delay for 5 seconds
                    delay(1000)
                }
            }


        }, 5000) // 5000 milliseconds = 5 seconds


        binding.webViewForOtpValidation.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (!startedCallsForOTPInject) {
                    startedCallsForOTPInject = true
                    startFetchingOtpAtIntervals()
                }
                if (url?.contains("boxpay") == true) {
                    finish()
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
            }
        }
    }

    private fun readSms() {
        try {
            val contentResolver: ContentResolver = this.contentResolver
            val cursor: Cursor? = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                Telephony.Sms.DEFAULT_SORT_ORDER + " LIMIT 1"
            )

            cursor?.use { // Ensures the cursor is closed after use
                if (it.moveToFirst()) {
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                    val extractedOTP = extractOTPFromMessage(body)
                    if (extractedOTP != null) {
                        otpFetched = extractedOTP
                        jobForFetchingSMS?.cancel()
                    }
                    // Process SMS message here
                }
            }
        } catch (e: SecurityException) {
            // Handle permission denial

            val permission = Manifest.permission.RECEIVE_SMS

            ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readSms()
            }
        }
    }

    private val runnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, delayMillis) // Schedule next execution after delay
        }
    }

    fun extractOTPFromMessage(message: String): String? {
        val regex = "\\b\\d{4}\\b|\\b\\d{6}\\b" // Double backslashes to escape within Kotlin string
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(message)

        if (matcher.find()) {
            return matcher.group() // Return the matched OTP
        }

        return null // Return null if no OTP is found
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1010) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                otpFetched = extractOTPFromMessage(message.toString())
            }
        }
    }

    private fun startFetchingOtpAtIntervals() {
        handler.postDelayed(runnable, delayMillis)
    }

    override fun onBackPressed() {
        if (!isBottomSheetShown) {
            val bottomSheet = CancelConfirmationBottomSheet()
            bottomSheet.show(supportFragmentManager, "CancelConfirmationBottomSheet")
        } else {
            super.onBackPressed()
        }
    }

    private fun fetchStatusAndReason(url: String) {

        val sharedPreferences =
            this.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.GET, url, null,
            Response.Listener{ response ->
                try {
                    val status = response.getString("status")
                    val transactionId = response.getString("transactionId").toString()
                    delay = 200L

                    if (status.contains(
                            "Approved",
                            ignoreCase = true
                        ) || status.contains("PAID", ignoreCase = true)
                    ) {

                        editor.putString("status","Success")
                        editor.putString("amount", response.getString("amount").toString())
                        editor.putString("transactionId", transactionId)
                        editor.apply()

                        finish()
                    } else if (status.contains("RequiresAction", ignoreCase = true)) {
                        editor.putString("status","RequiresAction")
                        editor.apply()
                    } else if (status.contains("Processing", ignoreCase = true)) {
                        editor.putString("status","Posted")
                        editor.apply()
                    } else if (status.contains("FAILED", ignoreCase = true)) {

                        editor.putString("status","Failed")
                        editor.apply()
                        finish()
                    }

                } catch (e: JSONException) {

                }
            },
            Response.ErrorListener {
                // no op
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
                return headers
            }
        }
        requestQueue.add(jsonObjectRequest)
    }

    private fun startFunctionCalls() {
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(delay)
                fetchStatusAndReason("${Base_Session_API_URL}${token}/status")
                // Delay for 5 seconds
            }
        }
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences =
            this.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token", "empty")
        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    fun generateRandomAlphanumericString(length: Int): String {
        val charPool: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}