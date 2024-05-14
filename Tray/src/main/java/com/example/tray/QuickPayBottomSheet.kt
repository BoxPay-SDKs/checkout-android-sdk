package com.example.tray

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.SeekBar
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.databinding.FragmentQuickPayBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import org.json.JSONObject
import java.util.Locale

class QuickPayBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentQuickPayBottomSheetBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor : SharedPreferences.Editor
    private lateinit var Base_Session_API_URL: String
    private var transactionId : String ?= null
    private var token: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding = FragmentQuickPayBottomSheetBinding.inflate(layoutInflater,container,false)
//        val seekBar = binding.sliderButton
//
//        val maxProgress = 100
//        val desiredMinProgress = (maxProgress * 0.15).toInt() // 15% progress
//        val desiredMaxProgress = (maxProgress * 0.85).toInt() // 85% progress
//
//        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                // Check if progress exceeds desired progress
//                if (progress > desiredMaxProgress) {
//                    seekBar?.progress = desiredMaxProgress
//                }
//                if(progress < desiredMinProgress) {
//                    seekBar?.progress = desiredMinProgress
//                }
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {
//                // Not needed for this implementation
//            }
//
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//                // Not needed for this implementation
//                var progress = 0
//                if(seekBar == null){
//                    progress = 0
//                }else{
//                    progress = seekBar.progress
//                }
//
//                if(progress < 80){
//                    seekBar?.progress = desiredMinProgress
//                }else{
//                    seekBar?.progress = desiredMaxProgress
//                }
//            }
//        })


        sharedPreferences = requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        val environmentFetched = sharedPreferences.getString("environment", "null")

        token = sharedPreferences.getString("token", "empty")

        Base_Session_API_URL = "https://${environmentFetched}apis.boxpay.tech/v0/checkout/sessions/"



        fetchLastUsedPaymentMethods()
        return binding.root
    }

    private fun fetchLastUsedPaymentMethods(){
        val sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        val requestQueue = Volley.newRequestQueue(context)
        val environmentFetched = sharedPreferences.getString("environment","null")
        val url = "https://${environmentFetched}apis.boxpay.tech/v0/shoppers/+919818198330/instruments"
        val jsonArrayRequest = object : JsonArrayRequest(
            Method.GET, url,null,
            Response.Listener { response ->
                logJsonObject(response.getJSONObject(0))
            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("Detailed error response:", "Detailed error response: $errorResponse")

                    val errorMessage = extractMessageFromErrorResponse(errorResponse).toString()
                    Log.d("Error message", errorMessage)
                    if (errorMessage.contains(
                            "Session is no longer accepting the payment as payment is already completed",
                            ignoreCase = true
                        )
                    ) {

                    } else {

                    }
                }


            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Session 82168892-b4a6-4b44-8a66-120fe5c6329f"
                return headers
            }
        }.apply {
            // Set retry policy
            val timeoutMs = 100000 // Timeout in milliseconds
            val maxRetries = 0 // Max retry attempts
            val backoffMultiplier = 1.0f // Backoff multiplier
            retryPolicy = DefaultRetryPolicy(timeoutMs, maxRetries, backoffMultiplier)
        }

        // Add the request to the RequestQueue.
        requestQueue.add(jsonArrayRequest)
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


    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body QuickPay", jsonStr)
    }

    private fun postRequestForUPICollect(context: Context, userVPA: String) {
        Log.d("postRequestCalled", System.currentTimeMillis().toString())
        val requestQueue = Volley.newRequestQueue(context)


        // Constructing the request body
        val requestBody = JSONObject().apply {
            // Billing Address
            val billingAddressObject = JSONObject().apply {
                put("address1", sharedPreferences.getString("address1", "null"))
                put("address2", sharedPreferences.getString("address2", "null"))
                put("address3", sharedPreferences.getString("address3", "null"))
                put("city", sharedPreferences.getString("city", "null"))
                put("countryCode", sharedPreferences.getString("countryCode", "null"))
                put("countryName", sharedPreferences.getString("countryName", "null"))
                put("postalCode", sharedPreferences.getString("postalCode", "null"))
                put("state", sharedPreferences.getString("state", "null"))
            }
            put("billingAddress", billingAddressObject)

            // Browser Data

            // Get the IP address

            // Create the browserData JSON object
            val browserData = JSONObject().apply {

                val webView = WebView(requireContext())

                // Get the default User-Agent string
                val userAgentHeader = WebSettings.getDefaultUserAgent(requireContext())

                // Get the screen height and width
                val displayMetrics = resources.displayMetrics
                put("screenHeight", displayMetrics.heightPixels.toString())
                put("screenWidth", displayMetrics.widthPixels.toString())
                put("acceptHeader", "application/json")
                put("userAgentHeader", userAgentHeader)
                put("browserLanguage", Locale.getDefault().toString())
                put("ipAddress", sharedPreferences.getString("ipAddress","null"))
                put("colorDepth", 24) // Example value
                put("javaEnabled", true) // Example value
                put("timeZoneOffSet", 330) // Example value
            }
            put("browserData", browserData)

            // Instrument Details
            val instrumentDetailsObject = JSONObject().apply {
                put("type", "upi/collect")

                val upiObject = JSONObject().apply {
                    put("shopperVpa", userVPA)
                }
                put("upi", upiObject)
            }
            put("instrumentDetails", instrumentDetailsObject)
            // Shopper
            val shopperObject = JSONObject().apply {
                val deliveryAddressObject = JSONObject().apply {

                    put("address1", sharedPreferences.getString("address1","null"))
                    put("address2", sharedPreferences.getString("address2","null"))
                    put("address3", sharedPreferences.getString("address3","null"))
                    put("city", sharedPreferences.getString("city","null"))
                    put("countryCode", sharedPreferences.getString("countryCode","null"))
                    put("countryName", sharedPreferences.getString("countryName","null"))
                    put("postalCode", sharedPreferences.getString("postalCode","null"))
                    put("state", sharedPreferences.getString("state","null"))
                }


                put("deliveryAddress", deliveryAddressObject)
                put("email", sharedPreferences.getString("email","null"))
                put("firstName", sharedPreferences.getString("firstName","null"))
                if(sharedPreferences.getString("gender","null") == "null")
                    put("gender", JSONObject.NULL)
                else
                    put("gender",sharedPreferences.getString("gender","null"))
                put("lastName", sharedPreferences.getString("lastName","null"))
                put("phoneNumber", sharedPreferences.getString("phoneNumber","null"))
                put("uniqueReference", sharedPreferences.getString("uniqueReference","null"))
            }

            logJsonObject(shopperObject)


            put("shopper", shopperObject)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->
                // Handle response
                // Log.d("Response of Successful Post API call", response.toString())

                transactionId = response.getString("transactionId").toString()
                updateTransactionIDInSharedPreferences(transactionId!!)
                logJsonObject(response)
            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("Error", "Detailed error response: $errorResponse")
                    val errorMessage = extractMessageFromErrorResponse(errorResponse).toString()
                    Log.d("Error message", errorMessage)
                    if (errorMessage.contains(
                            "Session is no longer accepting the payment as payment is already completed",
                            ignoreCase = true
                        )
                    ) {

                    } else {

                    }

                }


            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = token.toString()
                return headers
            }
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
    private fun updateTransactionIDInSharedPreferences(transactionIdArg : String) {
        editor.putString("transactionId", transactionIdArg)
        editor.apply()
    }

    companion object {

    }
}