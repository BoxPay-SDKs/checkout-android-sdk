package com.boxpay.checkout.sdk

import android.content.Context
import android.webkit.WebSettings
import com.boxpay.checkout.sdk.enum.AnalyticsEvents
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.lang.Thread.UncaughtExceptionHandler
import java.util.*

class SDKCrashHandler(
    private val context: Context,
    private val token: String,
    private val url: String,
    private val defaultHandler: UncaughtExceptionHandler
) : UncaughtExceptionHandler {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor { _ ->
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runBlocking {
            withContext(Dispatchers.IO) {
                reportCrashSync(throwable)
            }
        }
        defaultHandler.uncaughtException(thread, throwable)
    }

    private fun reportCrashSync(throwable: Throwable) {
        try {
            val userAgentHeader = WebSettings.getDefaultUserAgent(context)
            val browserLanguage = Locale.getDefault().toString()

            val payload = JSONObject().apply {
                put(AnalyticsEvents.CALLER_TOKEN, token)
                put(AnalyticsEvents.UI_EVENT, AnalyticsEvents.SDK_CRASH)

                put("browserData", JSONObject().apply {
                    put("userAgentHeader", userAgentHeader)
                    put("browserLanguage", browserLanguage)
                })

                put("eventAttrs", JSONObject().apply {
                    put("errorMessage", throwable.message ?: "No message")
                    put("screenName", "BoxPayCheckout")
                    put("exception", throwable.javaClass.name)
                })
            }

            val body = payload.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute()

        } catch (_: Exception) {
        }
    }
}
