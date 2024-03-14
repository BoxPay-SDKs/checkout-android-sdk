package com.example.tray

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.databinding.ActivityOtpscreenWebViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONException


internal class OTPScreenWebView : AppCompatActivity() {
    private val binding by lazy {
        ActivityOtpscreenWebViewBinding.inflate(layoutInflater)
    }

    private var job: Job? = null
    private var token: String? = null
    private lateinit var requestQueue: RequestQueue
    private var successScreenFullReferencePath: String? = null
    private var previousBottomSheet: Context ?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        requestQueue = Volley.newRequestQueue(this)
        val receivedUrl = intent.getStringExtra("url")
        Log.d("url", receivedUrl.toString())
        binding.webViewForOtpValidation.loadUrl(receivedUrl.toString())
        binding.webViewForOtpValidation.settings.domStorageEnabled = true
        binding.webViewForOtpValidation.settings.javaScriptEnabled = true
        startFunctionCalls()
        fetchTransactionDetailsFromSharedPreferences()


        binding.webViewForOtpValidation.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Page finished loading, you can perform any necessary actions here
                Log.d("page finished loading",url.toString())
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                // Handle errors here
                Log.d("page failed loading",error.toString())
            }
        }
    }

    // Method to set the previous bottom sheet reference
    fun setPreviousBottomSheet(bottomSheet: Context?) {
        previousBottomSheet = bottomSheet
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
                    Log.d("WebView Status", status)
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
                        val sharedPreferences = this.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
                       val successScreenFullReferencePath = sharedPreferences.getString("successScreenFullReferencePath","empty")

                        openActivity(successScreenFullReferencePath.toString(),this)
                    } else if (status.contains("PENDING", ignoreCase = true)) {
//                        val bottomSheet = PaymentFailureScreen()
//                        bottomSheet.show(supportFragmentManager,"PaymentFailureBottomSheet")
//                        finish()
                    } else if (status.contains("EXPIRED", ignoreCase = true)) {
                        val bottomSheet = PaymentFailureScreen()
                        bottomSheet.show(supportFragmentManager,"PaymentFailureBottomSheet")
                        finish()
                    } else if (status.contains("PROCESSING", ignoreCase = true)) {

                    } else if (status.contains("FAILED", ignoreCase = true)) {
                        val bottomSheet = PaymentFailureScreen()
                        bottomSheet.show(supportFragmentManager,"PaymentFailureBottomSheet")
                        finish()
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
        requestQueue.add(jsonObjectRequest)
    }
//    fun killOTPWeViewActivity(){
//        val endAllTheBottomSheets = Runnable {
//            finish()
//        }
//        val handler = Handler()
//        // Delay execution by 1000 milliseconds (1 second)
//        handler.postDelayed(endAllTheBottomSheets, 2000)
//    }


    private fun startFunctionCalls() {
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                fetchStatusAndReason("https://test-apis.boxpay.tech/v0/checkout/sessions/${token}/status")
                // Delay for 5 seconds
                delay(4000)
            }
        }
    }
    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences = this.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token","empty")
        Log.d("data fetched from sharedPreferences",token.toString())
        successScreenFullReferencePath = sharedPreferences.getString("successScreenFullReferencePath","empty")
        Log.d("success screen path fetched from sharedPreferences",successScreenFullReferencePath.toString())
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the coroutine when the activity is destroyed
        job?.cancel()
    }
    companion object{

    }
    private fun openActivity(activityPath: String, context: Context) {
        if (context is AppCompatActivity) {
            try {
                // Get the class object for the activity using reflection
                val activityClass = Class.forName(activityPath)
                // Create an instance of the activity using Kotlin reflection
                val activityInstance = activityClass.getDeclaredConstructor().newInstance() as AppCompatActivity

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
}