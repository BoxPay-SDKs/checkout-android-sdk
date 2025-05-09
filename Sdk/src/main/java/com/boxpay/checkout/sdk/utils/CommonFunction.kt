package com.boxpay.checkout.sdk.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.webkit.WebSettings
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.OTPScreenWebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random

fun handleException(
    context: Context,
    message: String = "An error occurred",
    token: String,
    baseUrl: String,
    screenName: String
) {
    callUIAnalytics(context, token, baseUrl, message, screenName)
}

private fun callUIAnalytics(context: Context, token: String, baseUrl: String, message: String, screenName: String) {
    CoroutineScope(Dispatchers.IO).launch {
        val requestQueue = Volley.newRequestQueue(context)
        val userAgentHeader = WebSettings.getDefaultUserAgent(context)
        val browserLanguage = Locale.getDefault().toString()

        // Constructing the request body
        val requestBody = JSONObject().apply {
            put("callerToken", token)
            put("uiEvent", "SDK_CRASH")

            // Create browserData JSON object
            val browserData = JSONObject().apply {
                put("userAgentHeader", userAgentHeader)
                put("browserLanguage", browserLanguage)
            }
            val eventAttrs = JSONObject().apply {
                put("errorMessage", message)
                put("screenName", screenName)
            }
            put("eventAttrs", eventAttrs)

            put("browserData", browserData)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, "https://${baseUrl}/v0/ui-analytics", requestBody,
            Response.Listener { /*no response handling */ },
            Response.ErrorListener { /*no response handling */ }) {}.apply {
            // Set retry policy
            val timeoutMs = 100000 // Timeout in milliseconds
            val maxRetries = 0 // Max retry attempts
            val backoffMultiplier = 1.0f // Backoff multiplier
            retryPolicy = DefaultRetryPolicy(timeoutMs, maxRetries, backoffMultiplier)
        }
        requestQueue.add(jsonObjectRequest)
    }
}

fun openWebView(fragment: Fragment, response: JSONObject) {
    val type = response.getJSONArray("actions").getJSONObject(0).getString("type")
    val url = if (type.contains("html", true)) {
        response.getJSONArray("actions").getJSONObject(0).getString("htmlPageString")
    } else {
        response.getJSONArray("actions").getJSONObject(0).getString("url")
    }

    val intent = Intent(fragment.context, OTPScreenWebView::class.java)
    intent.putExtra("url", url)
    intent.putExtra("type", type)
    fragment.startActivityForResult(intent, 333)
}

fun fetchStatusAndReason(context: Context, url: String, editor: SharedPreferences.Editor, callback: (Boolean, String?) -> Unit) {
    val requestQueue = Volley.newRequestQueue(context)

    val jsonObjectRequest = object : JsonObjectRequest(
        Method.GET, url, null,
        Response.Listener { response ->
            try {
                val status = response.getString("status")
                val transactionId = response.getString("transactionId").toString()
                val amount = response.getString("amount").toString()

                when {
                    status.contains("Approved", true) || status.contains("PAID", true) -> {
                        editor.putString("status", "Success")
                        editor.putString("amount", amount)
                        editor.putString("transactionId", transactionId)
                        editor.apply()
                        callback(true, null)
                    }
                    status.contains("RequiresAction", true) -> {
                        editor.putString("status", "RequiresAction").apply()
                        callback(false, "RequiresAction")
                    }
                    status.contains("Processing", true) -> {
                        editor.putString("status", "Posted").apply()
                        callback(false, "Processing")
                    }
                    status.contains("FAILED", true) -> {
                        editor.putString("status", "Failed").apply()
                        callback(false, "Failed")
                    }
                    status.contains("Pending", true) -> {
                        editor.putString("status", "Failed").apply()
                        callback(false, "Pending")
                    }
                }
            } catch (_: JSONException) {
            }
        },
        Response.ErrorListener {
            callback(false, "Error")
        }) {
        override fun getHeaders(): MutableMap<String, String> {
            return hashMapOf("X-Request-Id" to generateRandomAlphanumericString(10))
        }
    }

    requestQueue.add(jsonObjectRequest)
}

fun generateRandomAlphanumericString(length: Int): String {
    val charPool: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}

const val DEFAULT_UPI_TIMER_IN_SEC = 300



