package com.example.tray

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.ViewModels.SharedViewModel
import com.example.tray.ViewModels.SingletonForDismissMainSheet
import com.example.tray.databinding.ActivityOtpscreenWebViewBinding
import com.example.tray.interfaces.OnWebViewCloseListener
import com.example.tray.interfaces.UpdateMainBottomSheetInterface
import com.example.tray.paymentResult.PaymentResultObject
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask
import java.util.regex.Pattern
import kotlin.reflect.typeOf


internal class OTPScreenWebView() : AppCompatActivity() {
    private val binding by lazy {
        ActivityOtpscreenWebViewBinding.inflate(layoutInflater)
    }

    private var callbackForDismissingMainSheet: UpdateMainBottomSheetInterface? = null
    val permissionReceive = Manifest.permission.RECEIVE_SMS
    val permissionRead = Manifest.permission.READ_SMS
    val smsVerificationReceiver = SmsReceiver()
    private var webViewCloseListener: OnWebViewCloseListener? = null
    private var job: Job? = null
    private var jobForFetchingSMS: Job? = null
    var isBottomSheetShown = false
    private var token: String? = null
    private lateinit var requestQueue: RequestQueue
    private var successScreenFullReferencePath: String? = null
    private var previousBottomSheet: Context? = null
    private lateinit var Base_Session_API_URL: String
    private lateinit var sharedViewModel: SharedViewModel
    private val handler = Handler()
    private val delayMillis = 4000L
    private val SMS_CONSENT_REQUEST = 1010
    private var otpFetched: String? = null
    private var startedCallsForOTPInject = false


    fun explicitDismiss() {
        finish()
    }

    fun setWebViewCloseListener(listener: OnWebViewCloseListener) {
        webViewCloseListener = listener
    }

    // Call this method when you close the webView
    private fun notifyWebViewClosed() {
        webViewCloseListener?.onWebViewClosed()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        sharedViewModel.dismissBottomSheetEvent.observe(this) { dismissed ->
            if (dismissed) {
                explicitDismiss()
                sharedViewModel.bottomSheetDismissed()
            }
            isBottomSheetShown = false
        }


        val sharedPreferences =
            this.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val baseUrl = sharedPreferences.getString("baseUrl", "null")
        Base_Session_API_URL = "https://${baseUrl}/v0/checkout/sessions/"

        requestQueue = Volley.newRequestQueue(this)
        val receivedUrl = intent.getStringExtra("url")
        binding.webViewForOtpValidation.loadUrl(receivedUrl.toString())
        binding.webViewForOtpValidation.settings.domStorageEnabled = true
        binding.webViewForOtpValidation.settings.javaScriptEnabled = true
        startFunctionCalls()
        fetchTransactionDetailsFromSharedPreferences()

        Handler(Looper.getMainLooper()).postDelayed({
            if (ContextCompat.checkSelfPermission(
                    this,
                    permissionReceive
                ) == PackageManager.PERMISSION_GRANTED
            ) {

            } else {
                // Permission is not granted, request it from the user
                ActivityCompat.requestPermissions(this, arrayOf(permissionReceive), 101)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    permissionRead
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is granted, register the receiver
            } else {
                // Permission is not granted, request it from the user
                ActivityCompat.requestPermissions(this, arrayOf(permissionRead), 101)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    permissionRead
                ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this,
                    permissionReceive
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
                registerReceiver(
                    smsVerificationReceiver,
                    intentFilter,
                    RECEIVER_VISIBLE_TO_INSTANT_APPS
                )

                readSms()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permissionRead, permissionReceive),
                    101
                )
            }


            jobForFetchingSMS = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    readSms()
                    // Delay for 5 seconds
                    delay(1000)
                }
            }


        }, 5000) // 5000 milliseconds = 5 seconds


        binding.webViewForOtpValidation.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (!startedCallsForOTPInject) {
                    startedCallsForOTPInject = true
                    startFetchingOtpAtIntervals()
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
            }
        }
    }

    private fun readSms() {
        try {
            val contentResolver: ContentResolver = this.contentResolver
            val cursor: Cursor? = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                Telephony.Sms.DEFAULT_SORT_ORDER + " LIMIT 1"
            )

            cursor?.use { // Ensures the cursor is closed after use
                if (it.moveToFirst()) {
                    val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                    val extractedOTP = extractOTPFromMessage(body)
                    if (extractedOTP != null) {
                        otpFetched = extractedOTP
                        jobForFetchingSMS?.cancel()
                    } else {

                    }
                    // Process SMS message here
                } else {
                    // No SMS found

                }
            }
        } catch (e: SecurityException) {
            // Handle permission denial
            Log.e("Error", "Permission Denied: ${e.message}")


            val permission = Manifest.permission.RECEIVE_SMS

            ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
        } catch (e: Exception) {
            // Handle other exceptions
            Log.e("Error", "Error reading SMS: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readSms()
            } else {
//                val permission = Manifest.permission.RECEIVE_SMS
//                ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
            }
        }
    }




    private val runnable = object : Runnable {
        override fun run() {
            // Call the function
            handler.postDelayed(this, delayMillis) // Schedule next execution after delay
        }
    }

    fun extractOTPFromMessage(message: String): String? {
        val regex = "\\b\\d{4}\\b|\\b\\d{6}\\b" // Double backslashes to escape within Kotlin string
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(message)

        if (matcher.find()) {
            return matcher.group() // Return the matched OTP
        }


        return null // Return null if no OTP is found
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1010) {
            // Result from SMS consent activity
            if (resultCode == Activity.RESULT_OK && data != null) {
                // User granted consent
                val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                otpFetched = extractOTPFromMessage(message.toString())
                // Handle OTP

            } else {
                // User denied consent
                // Handle denial
            }
        }
    }

    private fun unregisterReceiver() {
        try {
            this.unregisterReceiver(smsVerificationReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered, do nothing
        }
    }

    fun stopTimer() {
        handler.removeCallbacks(runnable)
    }

    // Start scheduling the function to run initially and then at intervals
    private fun startFetchingOtpAtIntervals() {
        handler.postDelayed(runnable, delayMillis)
    }

    override fun onBackPressed() {
        if (!isBottomSheetShown) {
            val bottomSheet = CancelConfirmationBottomSheet()
            bottomSheet.show(supportFragmentManager, "CancelConfirmationBottomSheet")
//            isBottomSheetShown = true
        } else {
//            isBottomSheetShown = false
            super.onBackPressed()
        }
    }

    // Method to set the previous bottom sheet reference
    fun setPreviousBottomSheet(bottomSheet: Context?) {
        previousBottomSheet = bottomSheet
    }


    private fun fetchStatusAndReason(url: String) {

        val sharedPreferences =
            this.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val status = response.getString("status")
                    val statusReason = response.getString("statusReason")
                    val transactionId = response.getString("transactionId")

                    // Check if status is success, if yes, dismiss the bottom sheet
                    if (status.contains(
                            "Approved",
                            ignoreCase = true
                        ) || status.contains("PAID", ignoreCase = true)
                    ) {

                        editor.putString("status","Success")
                        editor.apply()

                        job?.cancel()
                        val callback = SingletonClass.getInstance().getYourObject()
                        val callbackForDismissing = SingletonForDismissMainSheet.getInstance().getYourObject()
                        if(callback!= null){
                            callback.onPaymentResult(PaymentResultObject("Success",transactionId,transactionId))
                        }

                        if(callbackForDismissing != null){
                            callbackForDismissing.dismissFunction()
                        }else{

                        }

                        finish()
                    } else if (status.contains("RequiresAction", ignoreCase = true)) {
                        editor.putString("status","RequiresAction")
                        editor.apply()
                    } else if (status.contains("Processing", ignoreCase = true)) {
                        editor.putString("status","Posted")
                        editor.apply()
                    } else if (status.contains("FAILED", ignoreCase = true)) {

                        editor.putString("status","Failed")
                        editor.apply()

                        job?.cancel()
                        val callback =
                            FailureScreenCallBackSingletonClass.getInstance().getYourObject()
                        if (callback == null) {

                        } else {
                            callback.openFailureScreen()
                        }


                        finish()
                        
                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }) { error ->
            Log.e("Error", "Error occurred: ${error.message}")
            if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                val errorResponse = String(error.networkResponse.data)
                Log.e("Error", "Detailed error response: $errorResponse")
            }
            // Handle errors here
        }
        // Add the request to the RequestQueue.
        requestQueue.add(jsonObjectRequest)
    }
//    fun killOTPWeViewActivity(){
//        val endAllTheBottomSheets = Runnable {
//            finish()
//        }
//        val handler = Handler()
//        // Delay execution by 1000 milliseconds (1 second)
//        handler.postDelayed(endAllTheBottomSheets, 2000)
//    }

    private fun startFunctionCalls() {
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                fetchStatusAndReason("${Base_Session_API_URL}${token}/status")
                // Delay for 5 seconds
                delay(2000)
            }
        }
    }

    private fun fetchTransactionDetailsFromSharedPreferences() {
        val sharedPreferences =
            this.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token", "empty")
        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the coroutine when the activity is destroyed
        job?.cancel()
    }

    companion object {

    }

    private fun openActivity(activityPath: String, context: Context) {
        if (context is AppCompatActivity) {
            try {
                // Get the class object for the activity using reflection
                val activityClass = Class.forName(activityPath)
                // Create an instance of the activity using Kotlin reflection
                val activityInstance =
                    activityClass.getDeclaredConstructor().newInstance() as AppCompatActivity

                // Check if the activity is a subclass of AppCompatActivity
                if (activityInstance is AppCompatActivity) {
                    // Start the activity
                    context.startActivity(Intent(context, activityClass))
                } else {
                    // Log an error or handle the case where the activity is not a subclass of AppCompatActivity
                }
            } catch (e: ClassNotFoundException) {
                // Log an error or handle the case where the activity class cannot be found
            }
        } else {
            // Log an error or handle the case where the context is not an AppCompatActivity
        }
    }

    class WebAppInterface(private val mContext: Context) {
        @JavascriptInterface
        fun showToast(message: String) {

            if (message == "Success") {


            } else {
                Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show()
            }
        }


        @JavascriptInterface
        fun logStatement(message: String) {

        }
    }

}