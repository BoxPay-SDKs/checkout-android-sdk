package com.example.tray

import android.animation.ObjectAnimator
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.adapters.NetbankingBanksAdapter
import com.example.tray.databinding.FragmentNetBankingBottomSheetBinding
import com.example.tray.dataclasses.NetbankingDataClass
import com.example.tray.dataclasses.WalletDataClass
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.GsonBuilder
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale


class NetBankingBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentNetBankingBottomSheetBinding
    private lateinit var allBanksAdapter : NetbankingBanksAdapter
    private var banksDetailsOriginal: ArrayList<NetbankingDataClass> = ArrayList()
    private var banksDetailsFiltered : ArrayList<NetbankingDataClass> = ArrayList()
    private var token: String? = null
    private var proceedButtonIsEnabled = MutableLiveData<Boolean>()
    private val Base_Session_API_URL = "https://test-apis.boxpay.tech/v0/checkout/sessions/"
    private var checkedPosition : Int ?= null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            token = it.getString("token")
        }

    }
    private fun fetchBanksDetails(){
        val url = "https://test-apis.boxpay.tech/v0/checkout/sessions/${token}"
        val queue: RequestQueue = Volley.newRequestQueue(requireContext())
        val jsonObjectAll = JsonObjectRequest(Request.Method.GET, url, null, { response ->

            try {
                val jsonObject = response

                // Get the payment methods array
                val paymentMethodsArray = jsonObject.getJSONObject("configs").getJSONArray("paymentMethods")

                // Filter payment methods based on type equal to "Wallet"
                for (i in 0 until paymentMethodsArray.length()) {
                    val paymentMethod = paymentMethodsArray.getJSONObject(i)
                    if (paymentMethod.getString("type") == "NetBanking") {
                        val bankName = paymentMethod.getString("title")
                        val bankImage = R.drawable.wallet_sample_logo
                        val bankBrand = paymentMethod.getString("brand")
                        val bankInstrumentTypeValue = paymentMethod.getString("instrumentTypeValue")
                        banksDetailsOriginal.add(NetbankingDataClass(bankName,bankImage,bankBrand,bankInstrumentTypeValue))
                    }
                }

                // Print the filtered wallet payment methods


                val delayMillis = 5000L
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    showAllBanks()
                }, delayMillis)

            } catch (e: Exception) {
                Log.d("Error Occured", e.toString())
                e.printStackTrace()
            }

        }, { error ->

            Log.e("error here", "RESPONSE IS $error")
            Toast.makeText(requireContext(), "Fail to get response", Toast.LENGTH_SHORT)
                .show()
        })
        queue.add(jsonObjectAll)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Inflate the layout for this fragment
        binding = FragmentNetBankingBottomSheetBinding.inflate(layoutInflater,container,false)
        banksDetailsOriginal = arrayListOf()
        allBanksAdapter = NetbankingBanksAdapter(banksDetailsFiltered,binding.banksRecyclerView)
        binding.banksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.banksRecyclerView.adapter = allBanksAdapter
        fetchBanksDetails()
        hideLoadingInButton()
        var enabled = false
        binding.checkingTextView.setOnClickListener() {
            if (!enabled)
                enableProceedButton()
            else
                disableProceedButton()

            enabled = !enabled
        }
        proceedButtonIsEnabled.observe(this, Observer { enableProceedButton ->
            if (enableProceedButton) {
                enableProceedButton()
            } else {
                disableProceedButton()
            }
        })
        allBanksAdapter.checkPositionLiveData.observe(this, Observer { checkPositionObserved ->
            if(checkPositionObserved == null){
                disableProceedButton()
            }else{
                enableProceedButton()
                checkedPosition = checkPositionObserved
            }
        })





        binding.searchView.setOnQueryTextListener(/*listener (comment) */ object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String): Boolean {
                if(query.isEmpty()){
                    removeRecyclerViewFromBelowEditText()
                }else {
                    makeRecyclerViewJustBelowEditText()
                }
                filterBanks(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if(newText.isEmpty()){
                    removeRecyclerViewFromBelowEditText()
                }else {
                    makeRecyclerViewJustBelowEditText()
                }
                filterBanks(newText)
                return true
            }
        })

        binding.imageView2.setOnClickListener(){
            dismiss()
        }
        binding.proceedButton.setOnClickListener(){
            val bankInstrumentTypeValue = banksDetailsFiltered[checkedPosition!!].bankInstrumentTypeValue
            Log.d("Selected bank is : ",bankInstrumentTypeValue)
            showLoadingInButton()
            postRequest(requireContext(),bankInstrumentTypeValue)
        }

        return binding.root
    }

    private fun filterBanks(query: String?) {
        banksDetailsFiltered.clear()
        for(bank in banksDetailsOriginal){
            if(query.toString().isBlank() || query.toString().isBlank()){
                showAllBanks()
            }
            else if(bank.bankName.contains(query.toString(), ignoreCase = true)){
                banksDetailsFiltered.add(NetbankingDataClass(bank.bankName,bank.bankImage,bank.bankBrand, bank.bankInstrumentTypeValue))
            }
        }
        allBanksAdapter.notifyDataSetChanged()
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Remove the overlay from the first BottomSheet when the second BottomSheet is dismissed
        (parentFragment as? MainBottomSheet)?.removeOverlayFromCurrentBottomSheet()
        super.onDismiss(dialog)
    }
    fun showAllBanks(){
        banksDetailsFiltered.clear()
        for(bank in banksDetailsOriginal){
            banksDetailsFiltered.add(bank)
        }
        allBanksAdapter.notifyDataSetChanged()
    }

    fun makeRecyclerViewJustBelowEditText(){
        binding.textView19.visibility = View.GONE
        binding.textView24.visibility = View.GONE
        binding.linearLayout2.visibility = View.GONE
    }

    fun removeRecyclerViewFromBelowEditText(){
        binding.textView19.visibility = View.VISIBLE
        binding.textView24.visibility = View.VISIBLE
        binding.linearLayout2.visibility = View.VISIBLE

    }
    fun postRequest(context: Context, bankInstrumentTypeValue : String) {
        Log.d("postRequestCalled", System.currentTimeMillis().toString())
        val requestQueue = Volley.newRequestQueue(context)


        // Constructing the request body
        val requestBody = JSONObject().apply {
            // Billing Address
            val billingAddressObject = JSONObject().apply {
                put("address1", "delivery address for the delivery")
                put("address2", "delivery")
                put("address3", JSONObject.NULL)
                put("city", "Saharanpur")
                put("countryCode", "IN")
                put("countryName", "India")
                put("postalCode", "247554")
                put("state", "Uttar Pradesh")
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
                put("ipAddress", "121.12.23.44")
                put("colorDepth", 24) // Example value
                put("javaEnabled", true) // Example value
                put("timeZoneOffSet", 330) // Example value
            }
            put("browserData", browserData)

            // Instrument Details
            val instrumentDetailsObject = JSONObject().apply {
                put("type", bankInstrumentTypeValue)
            }
            put("instrumentDetails", instrumentDetailsObject)
            // Shopper
            val shopperObject = JSONObject().apply {
                val deliveryAddressObject = JSONObject().apply {
                    put("address1", "delivery address for the delivery")
                    put("address2", "delivery")
                    put("address3", JSONObject.NULL)
                    put("city", "Saharanpur")
                    put("countryCode", "IN")
                    put("countryName", "India")
                    put("postalCode", "247554")
                    put("state", "Uttar Pradesh")
                }
                put("deliveryAddress", deliveryAddressObject)
                put("email", "test123@gmail.com")
                put("firstName", "test")
                put("gender", JSONObject.NULL)
                put("lastName", "last")
                put("phoneNumber", "919656262256")
                put("uniqueReference", "x123y")
            }
            put("shopper", shopperObject)
        }

        // Request a JSONObject response from the provided URL
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, Base_Session_API_URL + token, requestBody,
            Response.Listener { response ->
                // Handle response
                logJsonObject(response)
                hideLoadingInButton()

                try {
                    // Parse the JSON response
                    val jsonObject = response

                    // Retrieve the "actions" array
                    val actionsArray = jsonObject.getJSONArray("actions")
                    var url = ""
                    // Loop through the actions array to find the URL
                    for (i in 0 until actionsArray.length()) {
                        val actionObject = actionsArray.getJSONObject(i)
                        url = actionObject.getString("url")
                        // Do something with the URL
                        Log.d("URL", url)
                    }

                    val intent = Intent(requireContext(),OTPScreenWebView :: class.java)
                    intent.putExtra("url", url)
                    startActivity(intent)

                } catch (e: JSONException) {
                    binding.errorField.visibility = View.VISIBLE
                    binding.textView4.text = e.toString()
                    e.printStackTrace()
                }

            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("Error", "Detailed error response: $errorResponse")
                    binding.errorField.visibility = View.VISIBLE
                    binding.textView4.text = extractMessageFromErrorResponse(errorResponse)
                    hideLoadingInButton()
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
    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body", jsonStr)
    }
    private fun enableProceedButton() {
        binding.proceedButton.isEnabled = true
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.button_bg)
        binding.proceedButton.setBackgroundResource(R.drawable.button_bg)
        binding.textView6.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
            )
        )
    }

    private fun disableProceedButton() {
        binding.textView6.visibility = View.VISIBLE
        binding.proceedButton.isEnabled = false
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
        binding.proceedButton.setBackgroundResource(R.drawable.disable_button)
        binding.textView6.setTextColor(Color.parseColor("#ADACB0"))
    }
    fun hideLoadingInButton() {
        binding.progressBar.visibility = View.INVISIBLE
        binding.textView6.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
            )
        )
        binding.textView6.visibility = View.VISIBLE
        binding.proceedButtonRelativeLayout.setBackgroundResource(R.drawable.disable_button)
        binding.proceedButton.setBackgroundResource(R.drawable.disable_button)
        binding.textView6.setTextColor(Color.parseColor("#ADACB0"))
    }

    fun showLoadingInButton() {
        binding.textView6.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.VISIBLE
        val rotateAnimation = ObjectAnimator.ofFloat(binding.progressBar, "rotation", 0f, 360f)
        rotateAnimation.duration = 3000
        rotateAnimation.repeatCount = ObjectAnimator.INFINITE
        binding.proceedButton.isEnabled = false
        rotateAnimation.start()
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

    companion object {
        fun newInstance(data: String?): NetBankingBottomSheet {
            val fragment = NetBankingBottomSheet()
            val args = Bundle()
            args.putString("token", data)
            fragment.arguments = args
            return fragment
        }
    }
}