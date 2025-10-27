package com.boxpay.checkout.sdk.utils

import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebSettings
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.enum.AnalyticsEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

fun callUIAnalytics(
    context: Context,
    message: String,
    screenName: String,
    uiEvent: String
) {
    val queue: RequestQueue = Volley.newRequestQueue(context)
    val token = getSessionToken(context)

    CoroutineScope(Dispatchers.IO).launch {
        val userAgentHeader = WebSettings.getDefaultUserAgent(context)
        val browserLanguage = Locale.getDefault().toString()

        // Construct the request body
        val requestBody = JSONObject().apply {
            put(AnalyticsEvents.CALLER_TOKEN, token.ifEmpty { "${getAppName(context)} app name" })
            put(AnalyticsEvents.UI_EVENT, uiEvent)

            val browserData = JSONObject().apply {
                put("userAgentHeader", userAgentHeader)
                put("browserLanguage", browserLanguage)
            }

            val eventAttrs = JSONObject().apply {
                put("errorMessage", if(token.isEmpty()) "$message extra message - token is '$token' which is not valid " else message)
                put("screenName", screenName)
            }

            put("eventAttrs", eventAttrs)
            put("browserData", browserData)
        }

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST,
            getAnalyticsUrl(context),
            requestBody,
            Response.Listener { _ ->
            },
            Response.ErrorListener { _ ->
            }
        ) {}.apply {
            retryPolicy = DefaultRetryPolicy(
                100_000, // timeout in milliseconds
                0,       // no retries
                1.0f     // backoff multiplier
            )
        }

        // Enqueue the request
        queue.add(jsonObjectRequest)
    }
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