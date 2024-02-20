package com.example.tray

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
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

class OTPScreenWebView : AppCompatActivity() {
    private val binding by lazy {
        ActivityOtpscreenWebViewBinding.inflate(layoutInflater)
    }

    private var job: Job? = null
    private var token: String? = null
    private lateinit var requestQueue: RequestQueue
    private var successScreenFullReferencePath: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        requestQueue = Volley.newRequestQueue(this)

        val receivedUrl = intent.getStringExtra("url")
        Log.d("url", receivedUrl.toString())
        binding.webViewForOtpValidation.loadUrl(receivedUrl.toString())
        startFunctionCalls()
        fetchTransactionDetailsFromSharedPreferences()
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
                        val bottomSheet = PaymentStatusBottomSheet()
                        bottomSheet.show(supportFragmentManager, "SuccessBottomSheet")
                        val endAllTheBottomSheets = Runnable {
                            finish()
                        }
                        val handler = Handler()
                        // Delay execution by 1000 milliseconds (1 second)
                        handler.postDelayed(endAllTheBottomSheets, 3000)
                    } else if (status.contains("PENDING", ignoreCase = true)) {
                        //do nothing
                    } else if (status.contains("EXPIRED", ignoreCase = true)) {

                    } else if (status.contains("PROCESSING", ignoreCase = true)) {

                    } else if (status.contains("FAILED", ignoreCase = true)) {
                        val bottomSheet = PaymentFailureScreen()
                        bottomSheet.show(supportFragmentManager,"PaymentFailureBottomSheet")
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
}