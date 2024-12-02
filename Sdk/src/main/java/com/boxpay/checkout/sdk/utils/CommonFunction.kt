package com.boxpay.checkout.sdk.utils

import android.content.Context
import android.webkit.WebSettings
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.Locale

fun handleException(
    context: Context,
    message: String = "An error occurred",
    token: String,
    baseUrl: String
) {
    callUIAnalytics(context, token, baseUrl, message)
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

private fun callUIAnalytics(context: Context, token: String, baseUrl: String, message: String) {
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
            put("errorMessage",message)
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

    // Add the request to the RequestQueue.
    requestQueue.add(jsonObjectRequest)

}
