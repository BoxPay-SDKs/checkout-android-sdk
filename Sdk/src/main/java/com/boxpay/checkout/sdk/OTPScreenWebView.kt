package com.boxpay.checkout.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
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
    private var webViewCloseListener: OnWebViewCloseListener? = null
    private var job: Job? = null
    private var jobForFetchingSMS: Job? = null
    var isBottomSheetShown = false
    private var token: String? = null
    private lateinit var requestQueue: RequestQueue
    private var successScreenFullReferencePath: String? = null
    private var previousBottomSheet: Context? = null
    private lateinit var Base_Session_API_URL: String
    private var captureOnly: Boolean = false
    private var captureAndSubmitOnly: Boolean = false
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
            registerReceiver(smsConsentReceiver, IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
                RECEIVER_EXPORTED)

            startSmsRetriever()

        }, 1000) // 5000 milliseconds = 5 seconds
        fetchOtpStatus()

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

    private val runnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, delayMillis) // Schedule next execution after delay
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
        unregisterReceiver(smsConsentReceiver)
        job?.cancel()
    }

    fun generateRandomAlphanumericString(length: Int): String {
        val charPool: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun startSmsRetriever() {
        val client = SmsRetriever.getClient(this)
        client.startSmsUserConsent(null)
    }

    private val smsConsentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
                val extras = intent.extras
                val consentIntent = extras?.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)

                try {
                    // Start the consent dialog to prompt the user for SMS reading permission
                    if (consentIntent != null) {
                        startActivityForResult(consentIntent, SMS_CONSENT_REQUEST)
                    }
                } catch (e: ActivityNotFoundException) {
                    // Handle error
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SMS_CONSENT_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                println("======message $message")

                if (message != null) {
                    otpFetched = extractOtpFromMessage(message) // Extract the OTP from the message
                    jobForFetchingSMS?.cancel()

                    // Inject OTP into the WebView or any web page
                    if (otpFetched != null && captureOnly) {
                        captureOnly()
                    } else if (otpFetched != null && captureAndSubmitOnly) {
                        captureAndSubmitOnly()
                    }
                }
            } else {
                // The user denied consent or the dialog was cancelled
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun extractOtpFromMessage(message: String): String? {
        val otpPattern = Pattern.compile("\\d{4,6}") // Adjust based on OTP length
        val matcher = otpPattern.matcher(message)
        return if (matcher.find()) {
            matcher.group(0) // Return the first match (OTP)
        } else null
    }

    private fun fetchOtpStatus() {
        val url = "${Base_Session_API_URL}${token}/setup-configs"
        println("=====url $url")
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.GET, url, null,
            Response.Listener{ response ->
                try {
                    val otpAutoCaptureMode = response.optString("otpAutoCaptureMode")
                    print("=======otpAuto$otpAutoCaptureMode")
                    if (!otpAutoCaptureMode.isNullOrEmpty() && otpAutoCaptureMode.equals("Disabled",true)) {
                        captureOnly = false
                        captureAndSubmitOnly = false
                    } else if (!otpAutoCaptureMode.isNullOrEmpty() && otpAutoCaptureMode.equals("Capture_Only",true)) {
                        captureOnly = true
                        captureAndSubmitOnly = false
                    } else {
                        captureOnly = false
                        captureAndSubmitOnly = true
                    }
                } catch (e: JSONException) {

                }
            },
            Response.ErrorListener {error ->
                println("=====errror ${String(error.networkResponse.data)}")
                println("=====error listener ${error.message}")
                // no op
            }) {}
        requestQueue.add(jsonObjectRequest)
    }

    private fun captureOnly() {
        val jsCode = """
    (function() {
       var otpInput = document.querySelector('input'); // Find the OTP input field
        if (otpInput) {
            otpInput.value = '$otpFetched'; // Set the OTP value
        }
         var submitButton = document.querySelector('button[type="submit"], input[type="submit"]');
         if(submitButton.disabled) {
         submitButton.disabled = false
         }
    })();
"""
        binding.webViewForOtpValidation.evaluateJavascript(jsCode, null)
    }

    private fun captureAndSubmitOnly() {
        val jsCode = """
    (function() {
        // Try to find an input field where the class name includes 'otp'
        var otpInput = document.querySelector('input[type="text"].otp, input[type="number"].otp, input[class*="otp"]');
        
        if (otpInput) {
            otpInput.value = '$otpFetched'; // Set the OTP value
        }
        
        var submitButton = document.querySelector('button[type="submit"], input[type="submit"]'); // Find the submit button
        if (submitButton) {
            submitButton.click(); // Click the submit button to proceed
        }
    })();
"""
        binding.webViewForOtpValidation.evaluateJavascript(jsCode, null) // Inject JavaScript into the WebView

    }

}