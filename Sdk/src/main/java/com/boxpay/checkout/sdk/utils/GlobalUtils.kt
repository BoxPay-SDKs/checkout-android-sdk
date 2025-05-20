package com.boxpay.checkout.sdk.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.webkit.WebSettings
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.OTPScreenWebView
import com.boxpay.checkout.sdk.UPITimerBottomSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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

fun showWebOrTimerScreen(fragment: Fragment,response: JSONObject, displayUserId : String , startFetchStatusCall : ()-> Unit) {
    val actionObject = response.optJSONArray("actions")
        ?.takeIf { it.length() > 0 }
        ?.getJSONObject(0)

    val actionType = actionObject?.optString("type", null)

    when (actionType) {
        "html", "url" -> {
            openWebView(fragment,response)
            startFetchStatusCall()
        }
        "timer" -> {
            val expirySec = actionObject?.optInt("expirySec", DEFAULT_UPI_TIMER_IN_SEC) ?: DEFAULT_UPI_TIMER_IN_SEC
            openUPITimerBottomSheet(displayUserId, expirySec, fragment)
        }
        else -> {
            openUPITimerBottomSheet(displayUserId, DEFAULT_UPI_TIMER_IN_SEC, fragment) // fallback
        }
    }
}

private fun openUPITimerBottomSheet(displayName : String,timerInSec: Int,fragment: Fragment) {
    val bottomSheetFragment = UPITimerBottomSheet.newInstance(displayName, timerInSec)
    fragment.childFragmentManager.beginTransaction()
        .add(bottomSheetFragment, "UPITimerBottomSheet")
        .commitAllowingStateLoss()
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatToISO8601WithCurrentTime(dateString: String): String {
    // Define a formatter to parse the input date string with time
    val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
    val dateFormatter = DateTimeFormatter.ISO_DATE

    // Try to parse the input as LocalDateTime
    val date = try {
        LocalDateTime.parse(dateString, dateTimeFormatter).toLocalDate()
    } catch (e: Exception) {
        // If parsing as LocalDateTime fails, try parsing as LocalDate
        LocalDate.parse(dateString, dateFormatter)
    }

    // Create a LocalDateTime with the fixed time set to "00:00:00"
    val dateTime = LocalDateTime.of(date, LocalTime.MIDNIGHT)

    // Convert LocalDateTime to ZonedDateTime in UTC
    val zonedDateTime = dateTime.atZone(ZoneOffset.UTC)

    // Format to ISO 8601 with "T00:00:00Z"
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    return zonedDateTime.format(formatter)
}

fun SharedPreferences.getEffectiveString(
    chosenKey: String,
    storedKey: String,
    validator: (String) -> Boolean = { it.isNotBlank() && it != "null" },
    formatter: (String) -> String = { it }
): String? {
    return listOf(chosenKey, storedKey)
        .mapNotNull { getString(it, null) }
        .firstOrNull { validator(it) }
        ?.let { formatter(it) }
}


const val DEFAULT_UPI_TIMER_IN_SEC = 300



