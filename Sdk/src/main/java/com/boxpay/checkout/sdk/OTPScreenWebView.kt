package com.boxpay.checkout.sdk

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
import com.google.android.gms.auth.api.phone.SmsRetriever
import kotlinx.coroutines.Job
import org.json.JSONException
import java.util.regex.Pattern
import kotlin.random.Random


internal class OTPScreenWebView() : AppCompatActivity() {
    private val binding by lazy {
        ActivityOtpscreenWebViewBinding.inflate(layoutInflater)
    }

    private var jobForFetchingSMS: Job? = null
    var isBottomSheetShown = false
    private var token: String? = null
    private lateinit var requestQueue: RequestQueue
    private var successScreenFullReferencePath: String? = null
    private lateinit var Base_Session_API_URL: String
    private var captureOnly: Boolean = false
    private var captureAndSubmitOnly: Boolean = false
    private lateinit var sharedViewModel: SharedViewModel
    private val SMS_CONSENT_REQUEST = 1010
    private var otpFetched: String? = null

    private fun explicitDismiss() {
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

            binding.webViewForOtpValidation.loadDataWithBaseURL(
                null,
                htmlUrl,
                "text/html",
                "UTF-8",
                null
            )
        } else {
            binding.webViewForOtpValidation.loadUrl(receivedUrl.toString())
        }

        binding.webViewForOtpValidation.settings.domStorageEnabled = true
        binding.webViewForOtpValidation.settings.javaScriptEnabled = true

        fetchTransactionDetailsFromSharedPreferences()

        Handler(Looper.getMainLooper()).postDelayed({
            registerReceiver(
                smsConsentReceiver, IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
                RECEIVER_EXPORTED
            )
            startSmsRetriever()
        }, 1000)
        fetchOtpStatus()

        binding.webViewForOtpValidation.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
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

    override fun onBackPressed() {
        if (!isBottomSheetShown) {
            val bottomSheet = CancelConfirmationBottomSheet()
            bottomSheet.show(supportFragmentManager, "CancelConfirmationBottomSheet")
        } else {
            super.onBackPressed()
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
                    if (consentIntent != null) {
                        startActivityForResult(consentIntent, SMS_CONSENT_REQUEST)
                    }
                } catch (_: ActivityNotFoundException) {
                    // Handle error
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SMS_CONSENT_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                if (message != null) {
                    otpFetched = extractOtpFromMessage(message) // Extract the OTP from the message
                    jobForFetchingSMS?.cancel()

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
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.GET, url, null,
            Response.Listener { response ->
                try {
                    val otpAutoCaptureMode = response.optString("otpAutoCaptureMode")
                    if (!otpAutoCaptureMode.isNullOrEmpty() && otpAutoCaptureMode.equals(
                            "Disabled",
                            true
                        )
                    ) {
                        captureOnly = false
                        captureAndSubmitOnly = false
                    } else if (!otpAutoCaptureMode.isNullOrEmpty() && otpAutoCaptureMode.equals(
                            "Capture_Only",
                            true
                        )
                    ) {
                        captureOnly = true
                        captureAndSubmitOnly = false
                    } else {
                        captureOnly = false
                        captureAndSubmitOnly = true
                    }
                } catch (_: JSONException) {

                }
            },
            Response.ErrorListener { }) {}
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
        var otpInput = document.querySelector('input:placeholder-shown');
        
        if (otpInput) {
            otpInput.value = '$otpFetched'; // Set the OTP value
        }
        
        var submitButton = document.querySelector('button[type="submit"], input[type="submit"]'); // Find the submit button
        if (submitButton) {
        if(submitButton.disabled) {
         submitButton.disabled = false
         }
            submitButton.click(); // Click the submit button to proceed
        }
    })();
"""
        binding.webViewForOtpValidation.evaluateJavascript(
            jsCode,
            null
        )
    }
}