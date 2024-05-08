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
import com.example.tray.databinding.ActivityOtpscreenWebViewBinding
import com.example.tray.interfaces.OnWebViewCloseListener
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

    val permissionReceive = Manifest.permission.RECEIVE_SMS
    val permissionRead = Manifest.permission.READ_SMS
    val smsVerificationReceiver = SmsReceiver()
    private var webViewCloseListener: OnWebViewCloseListener? = null
    private var job: Job? = null
    private var jobForFetchingSMS : Job? = null
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
        Log.d("cancel confirmation bottom sheet", "explicit dismiss called")
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



        if (ContextCompat.checkSelfPermission(this, permissionReceive) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, register the receiver
            Log.d("Permission given","For Receive SMS")

        } else {
            // Permission is not granted, request it from the user
            ActivityCompat.requestPermissions(this, arrayOf(permissionReceive), 101)
        }

        if (ContextCompat.checkSelfPermission(this, permissionRead) == PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission given","For Read SMS")
            // Permission is granted, register the receiver
        } else {
            // Permission is not granted, request it from the user
            ActivityCompat.requestPermissions(this, arrayOf(permissionRead), 101)
        }

        if(ContextCompat.checkSelfPermission(this, permissionRead) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, permissionReceive) == PackageManager.PERMISSION_GRANTED){
            val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
            registerReceiver(
                smsVerificationReceiver,
                intentFilter,
                RECEIVER_VISIBLE_TO_INSTANT_APPS
            )
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(permissionRead,permissionReceive), 101)
        }


        jobForFetchingSMS = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                Log.d("Read Sms","Called")
                readSms()
                // Delay for 5 seconds
                delay(1000)
            }
        }

//        initAutoFill()



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
        val environmentFetched = sharedPreferences.getString("environment", "null")
        Log.d("environment is $environmentFetched", "Add UPI ID")
        Base_Session_API_URL = "https://${environmentFetched}apis.boxpay.tech/v0/checkout/sessions/"

        requestQueue = Volley.newRequestQueue(this)
        val receivedUrl = intent.getStringExtra("url")
        Log.d("url", receivedUrl.toString())
        binding.webViewForOtpValidation.loadUrl(receivedUrl.toString())
        binding.webViewForOtpValidation.settings.domStorageEnabled = true
        binding.webViewForOtpValidation.settings.javaScriptEnabled = true
        startFunctionCalls()
        fetchTransactionDetailsFromSharedPreferences()


        binding.webViewForOtpValidation.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Page finished loading, you can perform any necessary actions here
                Log.d("page finished loading", url.toString())
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
                // Handle errors here
                Log.d("page failed loading", error.toString())
            }
        }





//        val mainHandler = Handler(Looper.getMainLooper())
//        // Define a Runnable task to be executed after the delay
//        val delayedTask = Runnable {
//            println("Calling delayedFunction after delay...")
//            fetchAndInjectOtp()
//        }
//
////         Post the delayed task to the message queue of the main thread
//        mainHandler.postDelayed(delayedTask, 10000)
    }

    private fun initAutoFill() {
        SmsRetriever.getClient(this)
            .startSmsUserConsent(null)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ADD Card listening", "here")
                } else {
                    Log.d("ADD Card listening failed", "here")
                }
            }
    }

    fun hasValidOTP(input: String): Boolean {
        val regex = Regex("(?:your|use this) (verification code|passcode|OTP)\\s*(\\d{6})(?<!\\d)", RegexOption.IGNORE_CASE)
        Log.d("Message Received",regex.matches(input).toString())
        return regex.matches(input)
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
                    Log.d("Message Received", "Address: $address, Body: $body")
                    val extractedOTP = extractOTPFromMessage(body)
                    if(extractedOTP != null){
                        Log.d("Extracted OTP",extractedOTP)
                        otpFetched = extractedOTP
                        jobForFetchingSMS?.cancel()
                    }else{
                        Log.d("Extracted OTP","null")
                    }
                    // Process SMS message here
                } else {
                    // No SMS found
                    Log.d("Message Received", "No SMS found")
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
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readSms()
            }else{
//                val permission = Manifest.permission.RECEIVE_SMS
//                ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
            }
        }
    }


    private fun fetchAndInjectOtp() {

        if (otpFetched == null) {
            Log.d("otp fetched", "null")
            return
        }

        val otpNum = otpFetched!!.toInt()


//var submitButton = document.querySelector('button[type="submit"]');
        binding.webViewForOtpValidation.addJavascriptInterface(WebAppInterface(this), "Android")


        Log.d("OTP Validation", otpFetched.toString())
        val jsCode = """
            var proceedButtonIDFC = document.querySelector('.btn-idfc-maroon');
            var inputFieldWithPassword = document.querySelector('input[type="password"]');
            var inputFieldWithAutoComplete = document.querySelector('input[autocomplete="one-time-code"]'); 
    var inputField = document.querySelector('input'); // Assuming this is your OTP input field
var submitButton = document.querySelector('button[type="submit"]');
var submitButtonMainButton = document.querySelector('td.mainbutton a#submitOTP');
var makePaymentButton = document.querySelector('button[type="button"]');
var amexContinueButton = document.querySelector('#otcContinueBtn');

if(inputFieldWithAutoComplete){
    inputFieldWithAutoComplete.type = "text";
 inputFieldWithAutoComplete.value = "$otpFetched";
    setTimeout(function() {
    if(submitButtonMainButton){
        submitButtonMainButton.disabled = false;
                setTimeout(function() {
                    submitButtonMainButton.click(); // Click the submit button after a delay
                }, 2000);
        }
        else if (submitButton) {
            if (submitButton.disabled) {
                // If the submit button is disabled, enable it
                submitButton.disabled = false;
                setTimeout(function() {
                    submitButton.click(); // Click the submit button after a delay
                }, 2000); // Adjust the delay time as needed
            } else {
                setTimeout(function() {
                    submitButton.click(); // Click the submit button after a delay
                }, 2000); // Adjust the delay time as needed
            }
        }
        // Change back to password after a de   lay
        
       }, 1000);
    }
else if(inputFieldWithPassword){
inputFieldWithPassword.value = "$otpFetched";
    setTimeout(function() {
    if(amexContinueButton){
                setTimeout(function() {
                    amexContinueButton.click(); // Click the submit button after a delay
                }, 2000);
    }
    else if(proceedButtonIDFC){
    proceedButtonIDFC.disabled = false;
                setTimeout(function() {
                    proceedButtonIDFC.click(); // Click the submit button after a delay
                }, 2000);
    }
    else if(submitButtonMainButton){
        submitButtonMainButton.disabled = false;
                setTimeout(function() {
                    submitButtonMainButton.click(); // Click the submit button after a delay
                }, 2000);
        }
        else if (submitButton) {
            if (submitButton.disabled) {
                // If the submit button is disabled, enable it
                submitButton.disabled = false;
                setTimeout(function() {
                    submitButton.click(); // Click the submit button after a delay
                }, 2000); // Adjust the delay time as needed
            } else {
                setTimeout(function() {
                    submitButton.click(); // Click the submit button after a delay
                }, 2000); // Adjust the delay time as needed
            }
        }else if(makePaymentButton){
        if (makePaymentButton.disabled) {
                // If the submit button is disabled, enable it
                makePaymentButton.disabled = false;
                setTimeout(function() {
                    makePaymentButton.click(); // Click the submit button after a delay
                }, 2000); // Adjust the delay time as needed
            } else {
                setTimeout(function() {
                    makePaymentButton.click(); // Click the submit button after a delay
                }, 2000); // Adjust the delay time as needed
            }
        }
        
        // Change back to password after a delay
        
    }, 1000); 
}
else if (inputField) {
    inputField.value = "$otpFetched";
    setTimeout(function() {
    if(submitButtonMainButton){
        submitButtonMainButton.disabled = false;
       
                setTimeout(function() {
                    submitButtonMainButton.click(); // Click the submit button after a delay
                }, 2000);
        }
        else if (submitButton) {
       
        
            if (submitButton.disabled) {
                // If the submit button is disabled, enable it
                submitButton.disabled = false;
      
                setTimeout(function() {
                    submitButton.click(); // Click the submit button after a delay
                }, 2000); // Adjust the delay time as needed
            } else {
                setTimeout(function() {
                    submitButton.click(); // Click the submit button after a delay
                }, 2000); // Adjust the delay time as needed
            }
        }
      
        // Change back to password after a delay
      
    }, 1000); // Set the OTP value in the input field after a delay
} else {
    // Handle the case where the input field is not found
}
""".trimIndent()

// Execute JavaScript code immediately
        binding.webViewForOtpValidation.evaluateJavascript(jsCode) {value ->
            // Check for JavaScript errors
            if (value != null) {
                ""
                if (value.startsWith("throw")) {
                    Log.e("JavaScript Error", value)
                } else {
                    otpFetched = null
                    Log.d("JavaScript Result", value)
                }
            }
        }
    }

    private val runnable = object : Runnable {
        override fun run() {
            Log.d("otp fetched", "runnable $otpFetched")
            // Call the function
            fetchAndInjectOtp()
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

        Log.d("otp fetched", "extract OTP FROM MESSAGE null")
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
                Log.d("message fetched", otpFetched.toString())
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
        fetchAndInjectOtp()
        Log.d("otp fetched", "start fetching otp intervals")
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

    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body Fetch Status", jsonStr)
    }


    private fun fetchStatusAndReason(url: String) {
        Log.d("fetching function called correctly", "Fine")
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    logJsonObject(response)
                    val status = response.getString("status")
                    val statusReason = response.getString("statusReason")

                    // Do something with status and statusReason
                    // For example, log them
                    Log.d("WebView Status", status)
                    Log.d("Status Reason", statusReason)

                    // Check if status is success, if yes, dismiss the bottom sheet
                    if (status.contains(
                            "Approved",
                            ignoreCase = true
                        ) || statusReason.contains(
                            "Received by BoxPay for processing",
                            ignoreCase = true
                        ) || statusReason.contains(
                            "Approved by PSP",
                            ignoreCase = true
                        ) || status.contains("PAID", ignoreCase = true)
                    ) {
                        job?.cancel()
                        val sharedPreferences =
                            this.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
//                       val successScreenFullReferencePath = sharedPreferences.getString("successScreenFullReferencePath","empty")
//
//                        openActivity(successScreenFullReferencePath.toString(),this)
                        val callback = SingletonClass.getInstance().getYourObject()
                        if (callback == null) {
                            Log.d("call back is null", "Success")
                        } else {
                            job?.cancel()
                            callback.onPaymentResult(PaymentResultObject("Success"))
                            finish()
                        }

                    } else if (status.contains("PENDING", ignoreCase = true)) {
//                        val bottomSheet = PaymentFailureScreen()
//                        bottomSheet.show(supportFragmentManager,"PaymentFailureBottomSheet")
//                        finish()
                    } else if (status.contains("EXPIRED", ignoreCase = true)) {
                        job?.cancel()

//                        val bottomSheet = PaymentFailureScreen()
//                        bottomSheet.show(supportFragmentManager,"PaymentFailureBottomSheet")
//                        finish()
                    } else if (status.contains("PROCESSING", ignoreCase = true)) {

                    } else if (status.contains("FAILED", ignoreCase = true)) {
                        job?.cancel()
//                        val bottomSheet = PaymentFailureScreen()
                        val callback =
                            FailureScreenCallBackSingletonClass.getInstance().getYourObject()
                        if (callback == null) {
                            Log.d("callback is null", "PaymentFailed")
                        } else {
                            callback.openFailureScreen()
                        }
                        finish()
//                        bottomSheet.show(supportFragmentManager,"PaymentFailureBottomSheet")
                        Log.d("Failure Screen View Model", "OTP Screen $status")
////                        sharedViewModelForFailureScreen.openFailureScreen()
//                        FailureScreenFunctionObject.failureScreenFunction?.invoke()
//                        finish()

                    } else if (status.contains("Rejected", ignoreCase = true)) {
                        Log.d("Failure Screen View Model", "OTP Screen $status")
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
        Log.d("data fetched from sharedPreferences", token.toString())
        successScreenFullReferencePath =
            sharedPreferences.getString("successScreenFullReferencePath", "empty")
        Log.d(
            "success screen path fetched from sharedPreferences",
            successScreenFullReferencePath.toString()
        )
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
            Log.d("OTP Validation", message)
        }
    }
}