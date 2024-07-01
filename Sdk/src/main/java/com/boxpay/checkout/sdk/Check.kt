package com.boxpay.checkout.sdk

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.databinding.ActivityCheckBinding
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Check : AppCompatActivity() {
    val tokenLiveData = MutableLiveData<String>()
    private var successScreenFullReferencePath: String? = null
    private var tokenFetchedAndOpen = false


    private val binding: ActivityCheckBinding by lazy {
        ActivityCheckBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
//        val bottomSheet = QuickPayBottomSheet()
//        bottomSheet.show(supportFragmentManager,"QuickPayTesting")
        CoroutineScope(Dispatchers.Main).launch {
            val latestVersion =
                getLatestVersionFromJitPack("com.github.BoxPay-SDKs", "checkout-android-sdk")
            val currentVersion = BuildConfig.SDK_VERSION
            if (latestVersion != currentVersion) {
                enqueueSdkDownload(applicationContext, latestVersion)
            }
        }

        val prefs = applicationContext.getSharedPreferences("sdk_prefs", Context.MODE_PRIVATE)
        val newSdkAvailable = prefs.getBoolean("newSdkAvailable", false)
        if (newSdkAvailable) {
            // Update the SDK
            updateSdk()
            with(prefs.edit()) {
                putBoolean("newSdkAvailable", false)
                apply()
            }
        }

        makePaymentRequest(this)

        binding.textView6.text = "Generating Token Please wait..."
        successScreenFullReferencePath = "com.example.AndroidCheckOutSDK.SuccessScreen"
        tokenLiveData.observe(this, Observer { tokenInObserve ->
            // Handle the response after the token has been updated
            if (tokenInObserve != null) {
                handleResponseWithToken()
                binding.textView6.text = "Opening"
                binding.openButton.isEnabled = false
            } else {
                Log.d("token is empty", "waiting")
            }
        })

        var actionInProgress = false
        binding.openButton.setOnClickListener() {

            // Disable the button
            if (actionInProgress) {
                return@setOnClickListener
            }


            actionInProgress = true

            // Disable the button
            binding.openButton.isEnabled = false
            binding.openButton.visibility = View.GONE
            binding.pleaseWaitTextView.visibility = View.VISIBLE

            if (!(tokenLiveData.value.isNullOrEmpty())) {
                showBottomSheetWithOverlay()
                // Enable the button after the action is completed
                // You can remove this if you want to enable the button after a certain delay
                actionInProgress = false
                binding.openButton.isEnabled = true
            }
        }
    }

    fun removeLoadingAndEnabledProceedButton() {
        binding.openButton.isEnabled = true
        binding.progressBar.visibility = View.GONE
        Log.d("text will be updated here", "here")
        binding.textView6.text = "Open Bottom Sheet"
        binding.textView6.visibility = View.VISIBLE
        binding.openButton.visibility = View.VISIBLE
        binding.pleaseWaitTextView.visibility = View.GONE
    }

    fun showLoadingInButton() {
        binding.textView6.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.VISIBLE
        val rotateAnimation = ObjectAnimator.ofFloat(binding.progressBar, "rotation", 0f, 360f)
        rotateAnimation.duration = 3000
        rotateAnimation.repeatCount = ObjectAnimator.INFINITE
        binding.openButton.isEnabled = false
        rotateAnimation.start()
    }

    private fun handleResponseWithToken() {
        if (tokenFetchedAndOpen)
            return
        Log.d("Token", "Token has been updated. Using token: ${tokenLiveData.value}")
        showBottomSheetWithOverlay()
        tokenFetchedAndOpen = true
    }

    private fun showBottomSheetWithOverlay() {

        //tokenLiveData.value.toString()
//         tokenLiveData.value.toString()
        val boxPayCheckout = BoxPayCheckout(this, tokenLiveData.value ?: "",:: onPaymentResultCallback,true)
        boxPayCheckout.display()
//         QuickPayBottomSheet().show(supportFragmentManager,"QuickPayTesting")
    }


    fun onPaymentResultCallback(result: PaymentResultObject) {
        Log.d(
            "Result for the activity",
            "Payment result received: onpr ${result.status} onpr ${result.transactionId}  onpr ${result.operationId}"
        )
    }


    private fun makePaymentRequest(context: Context){
        val queue = Volley.newRequestQueue(context)
        val url = "https://sandbox-apis.boxpay.tech/v0/merchants/lGhJZ2Fxv2/sessions"
        val jsonData = JSONObject(""" {
  "context": {
    "countryCode": "IN",
    "legalEntity": {"code": "boxpay_test"},
    "orderId": "test12"
  },
  "paymentType": "S",
  "money": {"amount": "1", "currencyCode": "INR"},
  "descriptor": {"line1": "Some descriptor"},
  "shopper": {
    "firstName": "Ishika",
    "lastName": "Bansal",
    "email":"ishika.bansal@boxpay.tech",
    "uniqueReference": "x123y",
    "phoneNumber": "919876543210",
    "deliveryAddress": {
      "address1": "first line",
      "address2": "second line",
      "city": "New Delhi",
      "state": "Delhi",
      "countryCode": "IN",
      "postalCode": "147147"
    }
  },
  "order": {
    "originalAmount": 423.73,
    "shippingAmount": 50,
    "voucherCode": "VOUCHER",
    "taxAmount": 76.27,
    "totalAmountWithoutTax": 423.73,
    "items": [
      {
        "id": "test",
        "itemName": "Sample Item",
        "description": "testProduct",
        "quantity": 1,
        "manufacturer": null,
        "brand": null,
        "color": null,
        "productUrl": null,
        "imageUrl":
            "https://www.kasandbox.org/programming-images/avatars/old-spice-man.png",
        "categories": null,
        "amountWithoutTax": 423.73,
        "taxAmount": 76.27,
        "taxPercentage": null,
        "discountedAmount": null,
        "amountWithoutTaxLocale": "10",
        "amountWithoutTaxLocaleFull": "10"
      }
    ]
  },
  "statusNotifyUrl": "https://www.boxpay.tech",
  "frontendReturnUrl": "https://www.boxpay.tech",
  "frontendBackUrl": "https://www.boxpay.tech"
}""")

        val request = object : JsonObjectRequest(Method.POST, url, jsonData,
            { response ->
                logJsonObject(response)
                val sharedPreferences =
                    this.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
                val editor: SharedPreferences.Editor = sharedPreferences.edit()
                val tokenFetched = response.getString("token")
                Log.d("token fetched", tokenFetched)
                tokenLiveData.value = tokenFetched
                editor.putString("token",tokenLiveData.value)
                editor.apply()
                // Call a function that depends on the token
            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.toString()}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("Error", "Detailed error response: $errorResponse")
                    Log.d("","")
                }
            }) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] =  "Bearer OvxrLXMibYlA4Tn6NjMQuUnUOqUE36OOk7N3oUrGqfy6hDWWgfJnFIKqtCxWJ1vTEhIn6wMHsUmOMlvm7aUQ4e"
                headers["X-Client-Connector-Name"] =  "Android SDK"
                headers["X-Client-Connector-Version"] =  BuildConfig.SDK_VERSION
                return headers
            }
        }
        queue.add(request)
    }

    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body Check", jsonStr)
    }

    fun updateSdk() {
        val sdkDirectory = File(applicationContext.filesDir, "sdk")
        val newSdkDirectory = File(applicationContext.filesDir, "sdk_new")

        if (!newSdkDirectory.exists()) {
            Log.e("updateSdk", "New SDK directory does not exist.")
            return
        }

        // Delete old SDK files
        if (sdkDirectory.exists()) {
            sdkDirectory.deleteRecursively()
        }

        // Rename the new SDK directory to the old one
        if (newSdkDirectory.renameTo(sdkDirectory)) {
            Log.i("updateSdk", "SDK updated successfully.")
        } else {
            Log.e("updateSdk", "Failed to update SDK.")
        }
    }


    suspend fun getLatestVersionFromJitPack(groupId: String, artifactId: String): String {
        val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                // Log your message here (you can log to Logcat)
                Log.d("HttpLoggingInterceptor", message)
            }
        }).apply {
            level = HttpLoggingInterceptor.Level.BODY  // Set logging level
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)  // Add logging interceptor
            .build()

        val request = Request.Builder()
            .url("https://jitpack.io/api/builds/$groupId/$artifactId/latest/")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }

                val responseBody = response.body?.string()
                val json = JSONObject(responseBody)
                json.getString("version") // Return the version from JSON
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

        showUpdateSnackbar()
    }

    private fun showUpdateSnackbar() {
        val rootView = findViewById<View>(android.R.id.content)
        val snackbar =
            Snackbar.make(rootView, "A new version is available.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Update") {
                    // Call the update function
                    updateSdk()
                    // Restart the app
                    restartApp(applicationContext)
                }

        // Show the snackbar for a custom duration (e.g., 10 seconds)
        snackbar.show()
        Handler(Looper.getMainLooper()).postDelayed({
            snackbar.dismiss()
        }, 10000) // 10 seconds
    }


    fun restartApp(context: Context) {
        val packageManager: PackageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
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
            val newSdkFile = File(applicationContext.filesDir, "sdk_new/checkout-android-sdk-$latestVersion.aar")
            newSdkFile.parentFile?.mkdirs()

            withContext(Dispatchers.IO) {
                FileOutputStream(newSdkFile).use { outputStream ->
                    outputStream.write(body.bytes())
                }
            }

            Log.d("new version", "a new Version is downloaded")


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