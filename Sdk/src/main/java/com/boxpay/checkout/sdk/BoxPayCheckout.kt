package com.boxpay.checkout.sdk

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.ViewModels.CallBackFunctions
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

class BoxPayCheckout(
    private val context: Context,
    private val token: String,
    val onPaymentResult: ((PaymentResultObject) -> Unit)?,
    private val sandboxEnabled: Boolean
) {
    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
    private var editor: SharedPreferences.Editor = sharedPreferences.edit()
    private var sdkUpdater: SdkUpdater = SdkUpdater(context)

    private var BASE_URL: String? = null

    init {
        CoroutineScope(Dispatchers.Main).launch {
            val prefs = context.getSharedPreferences("sdk_prefs", Context.MODE_PRIVATE)
            val newSdkAvailable = prefs.getBoolean("newSdkAvailable", false)
            sdkUpdater.isUpdateAvailable { isUpdateAvailable, latestVersion ->
                if (isUpdateAvailable) {
                    sdkUpdater.downloadUpdate(
                        latestVersion = latestVersion
                    ) { sdkFile ->
                        if (sdkFile != null) {
                            sdkUpdater.installUpdate(sdkFile) {classLoader ->
                                if (newSdkAvailable) {
                                    sdkUpdater.reinitializeSdk(classLoader, token)
                                }
                            }
                        } else {
                            println("failed to update the file")
                        }
                    }
                    with(prefs.edit()) {
                        putBoolean("newSdkAvailable", false)
                        apply()
                    }
                }
            }

            if (!newSdkAvailable) {
                openBottomSheet()
            }
        }
        if (sandboxEnabled == true) {
            editor.putString("baseUrl", "sandbox-apis.boxpay.tech")
            this.BASE_URL = "sandbox-apis.boxpay.tech"
        } else if (sandboxEnabled == false){
            editor.putString("baseUrl", "apis.boxpay.in")
            this.BASE_URL = "apis.boxpay.in"
        } else {
            editor.putString("baseUrl", "test-apis.boxpay.tech")
            this.BASE_URL = "test-apis.boxpay.tech"
        }
        editor.apply()
    }

    fun display() {
        if (context is Activity) {
            callUIAnalytics(context)
            putTransactionDetailsInSharedPreferences()
        }
    }

    private fun callUIAnalytics(context: Context) {
        val requestQueue = Volley.newRequestQueue(context)
        val userAgentHeader = WebSettings.getDefaultUserAgent(context)
        val browserLanguage = Locale.getDefault().toString()

        // Constructing the request body
        val requestBody = JSONObject().apply {
            put("callerToken", token)
            put("uiEvent", "CHECKOUT_LOADED")

            // Create browserData JSON object
            val browserData = JSONObject().apply {
                put("userAgentHeader", userAgentHeader)
                put("browserLanguage", browserLanguage)
            }

            put("browserData", browserData)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, "${BASE_URL}/v0/ui-analytics", requestBody,
            Response.Listener { _ ->
            },
            Response.ErrorListener { _ ->
            }) {

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

    fun openBottomSheet() {
        initializingCallBackFunctions()

        if (context is Activity) {
            val activity =
                context as AppCompatActivity // or FragmentActivity, depending on your activity type
            val fragmentManager = activity.supportFragmentManager
            // Now you can use fragmentManager
            val bottomSheet = MainBottomSheet()
            bottomSheet.show(fragmentManager, "MainBottomSheet")
        }
    }

    private fun initializingCallBackFunctions() {
        val callBackFunctions = onPaymentResult?.let { CallBackFunctions(it) }
        SingletonClass.getInstance().callBackFunctions = callBackFunctions
    }


    private fun putTransactionDetailsInSharedPreferences() {
        editor.putString("token", token)
        editor.apply()
    }
}