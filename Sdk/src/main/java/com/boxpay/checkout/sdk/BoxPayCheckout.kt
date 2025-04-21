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
import com.boxpay.checkout.sdk.enum.AnalyticsEvents
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.boxpay.checkout.sdk.utils.ConfigurationOptions
import com.boxpay.checkout.sdk.utils.handleException
import org.json.JSONObject
import java.util.Locale

class BoxPayCheckout(
    private val context: Context,
    private val token: String,
    val onPaymentResult: ((PaymentResultObject) -> Unit)?,
    private val customerShopperToken: String = "",
    private val configurationOptions: Map<ConfigurationOptions, Any>? = null
) {
    constructor(
        context: Context,
        token: String,
        onPaymentResult: ((PaymentResultObject) -> Unit)?,
        configurationOptions: Map<ConfigurationOptions, Any>,
        customerShopperToken: String = "",
    ) : this(
        context,
        token,
        onPaymentResult,
        customerShopperToken,
        configurationOptions
    )

    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
    private var editor: SharedPreferences.Editor = sharedPreferences.edit()

    private var BASE_URL: String? = null

    fun display() {
        if(configurationOptions != null){
            if (configurationOptions[ConfigurationOptions.ENABLE_SANDBOX_ENV] == true) {
                editor.putString("baseUrl", "test-apis.boxpay.tech")
                this.BASE_URL = "test-apis.boxpay.tech"
            } else {
                editor.putString("baseUrl", "apis.boxpay.in")
                this.BASE_URL = "apis.boxpay.in"
            }
            editor.putBoolean("isSuccessScreenVisible", configurationOptions[ConfigurationOptions.SHOW_BOXPAY_SUCCESS_SCREEN] == true)
        }else {
            editor.putBoolean("isSuccessScreenVisible", false)
            editor.putString("baseUrl", "apis.boxpay.in")
            this.BASE_URL = "apis.boxpay.in"
        }
        editor.apply()
        try {
            if (!token.isNullOrEmpty()) {
                callUIAnalytics(context)
                putTransactionDetailsInSharedPreferences()
                openBottomSheet()
            } else {
                handleException(context, "Token added is either null or empty", token, this.BASE_URL ?: "", "BoxPayCheckout")
            }
        } catch (e: Exception) {
            handleException(context, e.message ?: "", token, this.BASE_URL ?: "", "BoxPayCheckout")
        }
    }

    private fun callUIAnalytics(context: Context) {
        val requestQueue = Volley.newRequestQueue(context)
        val userAgentHeader = WebSettings.getDefaultUserAgent(context)
        val browserLanguage = Locale.getDefault().toString()

        // Constructing the request body
        val requestBody = JSONObject().apply {
            put(AnalyticsEvents.CALLER_TOKEN, token)
            put(AnalyticsEvents.UI_EVENT, AnalyticsEvents.CHECKOUT_LOADED)

            // Create browserData JSON object
            val browserData = JSONObject().apply {
                put("userAgentHeader", userAgentHeader)
                put("browserLanguage", browserLanguage)
            }

            put("browserData", browserData)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, "https://${BASE_URL}/v0/ui-analytics", requestBody,
            Response.Listener { /*no response handling */ },
            Response.ErrorListener { /*no response handling */ }) {}.apply {
            // Set retry policy
            val timeoutMs = 100000 // Timeout in milliseconds
            val maxRetries = 0 // Max retry attempts
            val backoffMultiplier = 1.0f // Backoff multiplier
            retryPolicy = DefaultRetryPolicy(timeoutMs, maxRetries, backoffMultiplier)
        }

        // Add the request to the RequestQueue.
        requestQueue.add(jsonObjectRequest)

    }


    private fun openBottomSheet() {
        try {
            initializingCallBackFunctions()

            if (context is Activity) {
                val activity =
                    context as AppCompatActivity // or FragmentActivity, depending on your activity type
                val fragmentManager = activity.supportFragmentManager
                // Now you can use fragmentManager
                val bottomSheet = MainBottomSheet()
                bottomSheet.setContext(activity.applicationContext)
                bottomSheet.loadQrDirect(configurationOptions?.get(ConfigurationOptions.SHOW_UPI_QR_ON_LOAD) == true)
                bottomSheet.show(fragmentManager, "MainBottomSheet")
            }
        } catch (e: Exception) {
            handleException(context, e.message ?: "", token, this.BASE_URL ?: "", "BoxpayCheckout")
        }
    }

    fun initializingCallBackFunctions() {
        val callBackFunctions = onPaymentResult?.let { CallBackFunctions(it) }
        SingletonClass.getInstance().callBackFunctions = callBackFunctions
    }


    private fun putTransactionDetailsInSharedPreferences() {
        editor.putString("token", token)
        editor.putString("shopperToken", customerShopperToken)
        editor.apply()
    }
}