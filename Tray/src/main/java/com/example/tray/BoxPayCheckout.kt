package com.example.tray

import SingletonClass
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.ViewModels.CallBackFunctions
import com.example.tray.paymentResult.PaymentResultObject
import com.google.gson.GsonBuilder
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

class BoxPayCheckout(private val context: Context, private val token: String, val onPaymentResult: ((PaymentResultObject) -> Unit)?, private val sandboxEnabled: Boolean = false){
    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
    private var editor: SharedPreferences.Editor = sharedPreferences.edit()

    private var BASE_URL : String ?= null
    init {
        if(sandboxEnabled){
            editor.putString("baseUrl", "sandbox-apis.boxpay.tech")
            this.BASE_URL = "sandbox-apis.boxpay.tech"
        }else{
            editor.putString("baseUrl","apis.boxpay.in")
            this.BASE_URL = "apis.boxpay.in"
        }

        editor.apply()
    }
    fun display() {
        if (context is Activity) {
            val activity = context as AppCompatActivity // or FragmentActivity, depending on your activity type
            callUIAnalytics(context,"CHECKOUT_LOADED")
            putTransactionDetailsInSharedPreferences()
            openBottomSheet()
        }
    }

    private fun callUIAnalytics(context: Context, event: String) {
        val requestQueue = Volley.newRequestQueue(context)
        val userAgentHeader = WebSettings.getDefaultUserAgent(context)
        val browserLanguage = Locale.getDefault().toString()

        // Constructing the request body
        val requestBody = JSONObject().apply {
            put("callerToken", token)
            put("uiEvent", event)

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
            Response.Listener { response ->

                try {

                } catch (e: JSONException) {
                    Log.d("status check error", e.toString())
                }

            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("Error", "Detailed error response: $errorResponse")
                    val errorMessage = extractMessageFromErrorResponse(errorResponse).toString()
                    Log.d("Error message", errorMessage)
                }

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
    fun extractMessageFromErrorResponse(response: String): String? {
        try {
            // Parse the JSON string
            val jsonObject = JSONObject(response)
            // Retrieve the value associated with the "message" key
            return jsonObject.getString("message")
        } catch (e: Exception) {
            // Handle JSON parsing exception
            e.printStackTrace()
        }
        return null
    }

    private fun openBottomSheet(){
        initializingCallBackFunctions()

        if (context is Activity) {
            val activity = context as AppCompatActivity // or FragmentActivity, depending on your activity type
            val fragmentManager = activity.supportFragmentManager
            // Now you can use fragmentManager
            val bottomSheet = MainBottomSheet()
            bottomSheet.show(fragmentManager, "MainBottomSheet")
        }
    }
    fun initializingCallBackFunctions(){
        val callBackFunctions = onPaymentResult?.let { CallBackFunctions(it) }
        SingletonClass.getInstance().callBackFunctions = callBackFunctions
    }




    private fun putTransactionDetailsInSharedPreferences() {
        editor.putString("token", token)
        editor.apply()
    }
}