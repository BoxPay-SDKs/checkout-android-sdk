package com.example.tray

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.android.volley.Request
import com.android.volley.RequestQueue
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        requestQueue = Volley.newRequestQueue(this)

        val receivedUrl = intent.getStringExtra("url")
        Log.d("url", receivedUrl.toString())
        token = intent.getStringExtra("token")
        Log.d("token logged from webView", "token")
        binding.webViewForOtpValidation.loadUrl(receivedUrl.toString())
        startFunctionCalls()
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
                    Log.d("Status", status)
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
                        finish()
                    } else if (status.contains("PENDING", ignoreCase = true)) {
                        //do nothing
                    } else if (status.contains("EXPIRED", ignoreCase = true)) {

                    } else if (status.contains("PROCESSING", ignoreCase = true)) {

                    } else if (status.contains("FAILED", ignoreCase = true)) {

                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }) { error ->
            Log.d("Error here", error.toString())
            error.printStackTrace()

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
                delay(5000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the coroutine when the activity is destroyed
        job?.cancel()
    }
}