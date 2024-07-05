package com.example.tray

import SingletonClass
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.ViewModels.CallBackFunctions
import com.example.tray.paymentResult.PaymentResultObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

class BoxPayCheckout(private val context: Context, private val token: String, val onPaymentResult: ((PaymentResultObject) -> Unit)?, private val sandboxEnabled: Boolean = false){
    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
    private var editor: SharedPreferences.Editor = sharedPreferences.edit()

    private var BASE_URL : String ?= null
    init {
        CoroutineScope(Dispatchers.Main).launch {
            val latestVersion =
                getLatestVersionFromJitPack("com.github.BoxPay-SDKs", "checkout-android-sdk")
            val currentVersion = BuildConfig.SDK_VERSION
            if (latestVersion != currentVersion) {
                enqueueSdkDownload(context, latestVersion)
            }
        }

        val prefs = context.getSharedPreferences("sdk_prefs", Context.MODE_PRIVATE)
        val newSdkAvailable = prefs.getBoolean("newSdkAvailable", false)
        if (newSdkAvailable) {
            // Update the SDK
            updateSdk()
            with(prefs.edit()) {
                putBoolean("newSdkAvailable", false)
                apply()
            }
        }
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
                    e.printStackTrace()
                }

            },
            Response.ErrorListener { error ->
                // Handle error
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    val errorMessage = extractMessageFromErrorResponse(errorResponse).toString()
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

    fun updateSdk() {
        val sdkDirectory = File(context.filesDir, "boxpay_sdk")
        val newSdkDirectory = File(context.filesDir, "boxpay_sdk_new")

        if (!newSdkDirectory.exists()) {
            return
        }

        // Delete old SDK files
        if (sdkDirectory.exists()) {
            sdkDirectory.deleteRecursively()
        }
    }


    suspend fun getLatestVersionFromJitPack(groupId: String, artifactId: String): String {
        val client = OkHttpClient.Builder()
            .build()

        val request = Request.Builder()
            .url("https://jitpack.io/api/builds/$groupId/$artifactId/")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }

                val responseBody = response.body?.string()
                val json = JSONObject(responseBody)
                val versionsObject = json.getJSONObject("com.github.BoxPay-SDKs").getJSONObject("checkout-android-sdk")
                val versionsMap = versionsObject.toMap()
                val lastVersionWithoutVAndOkStatus = versionsMap.filter { (version, status) ->
                    !version.contains("v") &&  !version.contains("beta") && status == "ok"
                }.keys.maxOrNull()

                println("latestversion $lastVersionWithoutVAndOkStatus")

                return@withContext lastVersionWithoutVAndOkStatus ?: BuildConfig.SDK_VERSION
            } catch (e: IOException) {
                e.printStackTrace()
                "Unknown"
            }
        }
    }

    private fun enqueueSdkDownload(context: Context, latestVersion: String) {
        val workManager = WorkManager.getInstance(context)
        val downloadRequest = OneTimeWorkRequestBuilder<SdkDownloadWorker>()
            .setInputData(workDataOf("latestVersion" to latestVersion))
            .build()
        workManager.enqueue(downloadRequest)
    }

    fun JSONObject.toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = getString(key)
        }
        return map
    }
}

class SdkDownloadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val latestVersion = inputData.getString("latestVersion") ?: return Result.failure()
        val sdkUrl = "https://jitpack.io/com/github/BoxPay-SDKs/checkout-android-sdk/$latestVersion/checkout-android-sdk-$latestVersion.aar"

        // Implement the logic to download the SDK and save it locally
        val client = OkHttpClient()
        val request = Request.Builder().url(sdkUrl).build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure()
            }

            val body: ResponseBody = response.body ?: return Result.failure()
            val newSdkFile = File(applicationContext.filesDir, "boxpay_sdk_new/checkout-android-sdk-$latestVersion.aar")
            newSdkFile.parentFile?.mkdirs()

            withContext(Dispatchers.IO) {
                FileOutputStream(newSdkFile).use { outputStream ->
                    outputStream.write(body.bytes())
                }
            }


        } catch (e: IOException) {
            e.printStackTrace()
            return Result.failure()
        }

        // Notify that a new SDK is available
        val sharedPreferences =
            applicationContext.getSharedPreferences("sdk_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("newSdkAvailable", true)
            apply()
        }

        return Result.success()
    }
}