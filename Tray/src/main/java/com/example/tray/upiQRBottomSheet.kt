package com.example.tray

import android.R
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.databinding.FragmentUpiQRBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale


class upiQRBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding : FragmentUpiQRBottomSheetBinding
    private lateinit var sharedPreferences : SharedPreferences
    private lateinit var editor : SharedPreferences.Editor
    private lateinit var Base_Session_API_URL : String
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
        binding = FragmentUpiQRBottomSheetBinding.inflate(layoutInflater,container,false)

        sharedPreferences =
            requireActivity().getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()


        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        token = sharedPreferences.getString("token", "empty")
        val environmentFetched = sharedPreferences.getString("environment","null")
        Log.d("environment is $environmentFetched","Add UPI ID")
        Base_Session_API_URL = "https://${environmentFetched}apis.boxpay.tech/v0/checkout/sessions/"
        postRequest(requireContext())
        return binding.root
    }
    private fun postRequest(context: Context) {
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
                put("type", "upi/qr")
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
                val valuesObject = response.getJSONArray("actions").getJSONObject(0)
                val urlBase64 = valuesObject.getString("content")
                Log.d("urlBase64",urlBase64)

                val decodedBytes: ByteArray = Base64.decode(urlBase64, Base64.DEFAULT)

                // Convert byte array to Bitmap

                // Convert byte array to Bitmap
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                // Display the Bitmap in an ImageView

                // Display the Bitmap in an ImageView
                val imageView: ImageView = binding.qrCode
                imageView.setImageBitmap(bitmap)
                logJsonObject(response)
            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("Error", "Detailed error response: $errorResponse")
                }
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = token.toString()
                return headers
            }
        }.apply {
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

    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body UPI QR", jsonStr)
    }
    fun urlToBase64(base64String: String): String {

        return try {
            // Decode Base64 string to byte array
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)

            // Convert byte array to string
            val decodedString = String(decodedBytes, StandardCharsets.UTF_8)

            // Decode URL
            URLDecoder.decode(decodedString, "UTF-8")
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    companion object {

    }
}