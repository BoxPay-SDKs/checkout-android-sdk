package com.boxpay.checkout.sdk

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.databinding.FragmentUpiQRBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random


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

        token = sharedPreferences.getString("token", "empty")
        val environmentFetched = sharedPreferences.getString("environment","null")
        Base_Session_API_URL = "https://${environmentFetched}apis.boxpay.tech/v0/checkout/sessions/"
        postRequest(requireContext())
        return binding.root
    }
    private fun postRequest(context: Context) {
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
                put("panNumber", sharedPreferences.getString("panNumber", null))
                put("dateOfBirth", sharedPreferences.getString("dateOfBirth", null))
            }

            put("shopper", shopperObject)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->


                transactionId = response.getString("transactionId").toString()
                updateTransactionIDInSharedPreferences(transactionId!!)
                val valuesObject = response.getJSONArray("actions").getJSONObject(0)
                val urlBase64 = valuesObject.getString("content")

                val decodedBytes: ByteArray = Base64.decode(urlBase64, Base64.DEFAULT)

                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                val imageView: ImageView = binding.qrCode
                imageView.setImageBitmap(bitmap)
            },
            Response.ErrorListener { _ ->
                // no op
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["X-Request-Id"] = generateRandomAlphanumericString(10)
                headers["X-Client-Connector-Name"] =  "Android SDK"
                headers["X-Client-Connector-Version"] =  BuildConfig.SDK_VERSION
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

    fun generateRandomAlphanumericString(length: Int): String {
        val charPool : List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}