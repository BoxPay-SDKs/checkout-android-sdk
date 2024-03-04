package com.example.tray

import android.animation.ObjectAnimator
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.databinding.ActivityCheckBinding
import com.example.tray.databinding.CustomRadioButtonLayoutBinding
import com.google.gson.GsonBuilder
import org.json.JSONObject


class Check : AppCompatActivity() {
    val tokenLiveData = MutableLiveData<String>()
    private var successScreenFullReferencePath : String ?= null
    private var tokenFetchedAndOpen = false
    private val binding : ActivityCheckBinding by lazy {
        ActivityCheckBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        makePaymentRequest(this)

        binding.textView6.text = "Generating Token Please wait..."

        successScreenFullReferencePath = "com.example.AndroidCheckOutSDK.SuccessScreen"

        tokenLiveData.observe(this, Observer { token ->
            // Handle the response after the token has been updated
            if(!(tokenLiveData.value == null)) {
                handleResponseWithToken()
                binding.textView6.text = "Open"
                binding.openButton.isEnabled = true
            }else{
                Log.d("token is empty","waiting")
            }
        })

        var actionInProgress = false
        binding.openButton.setOnClickListener(){

            // Disable the button
            if (actionInProgress) {
                return@setOnClickListener
            }


            actionInProgress = true

            // Disable the button
            binding.openButton.isEnabled = false
            binding.openButton.visibility = View.GONE

            if (!(tokenLiveData.value.isNullOrEmpty())) {
                showBottomSheetWithOverlay()
                // Enable the button after the action is completed
                // You can remove this if you want to enable the button after a certain delay
                actionInProgress = false
                binding.openButton.isEnabled = true
            }
        }
    }
    fun removeLoadingAndEnabledProceedButton(){
        binding.openButton.isEnabled = true
        binding.progressBar.visibility = View.GONE
        Log.d("text will be updated here","here")
        binding.textView6.text = "Open Bottom Sheet"
        binding.textView6.visibility = View.VISIBLE
        binding.openButton.visibility = View.VISIBLE
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
    fun handleResponseWithToken() {
        if(tokenFetchedAndOpen)
            return
        Log.d("Token", "Token has been updated. Using token: ${tokenLiveData.value}")
        showBottomSheetWithOverlay()
        tokenFetchedAndOpen = true
    }


    fun showBottomSheetWithOverlay() {
        val bottomSheetFragment = MainBottomSheet.newInstance(tokenLiveData.value,successScreenFullReferencePath)
        bottomSheetFragment.show(supportFragmentManager, "MainBottomSheet")
    }
    fun makePaymentRequest(context: Context){
        val queue = Volley.newRequestQueue(context)
        val url = "https://test-apis.boxpay.tech/v0/merchants/hK3JrVc6ys/sessions"

        val jsonData = JSONObject("""{
    "context": {
        "countryCode": "SG",
        "legalEntity": {
            "code": "demo_merchant"
        },
        "orderId": "test12"
    },
    "paymentType": "S",
    "money": {
        "amount": "2197",
        "currencyCode": "EUR"
    },
    "descriptor": {
        "line1": "Some descriptor"
    },
    "billingAddress": {
        "address1": "first address line",
            "address2": "second address line",
            "city": "Faridabad",
            "state": "Haryana",
            "countryCode": "IN",
            "postalCode": "121004"
    },
    "shopper": {
        "firstName": "test",
        "lastName": "last",
        "email": "test123@gmail.com",
        "uniqueReference": "x123y",
        "phoneNumber": "911234567890",
        "deliveryAddress": {
            "address1": "first line",
        "address2": "second line",
        "city": "Mumbai",
        "state": "Maharashtra",
        "countryCode": "IN",
        "postalCode": "123456"
        }
    },
    "order": {
        "originalAmount": 1697,
        "shippingAmount": 500,
        "voucherCode": "VOUCHER",
        "totalAmountWithoutTax": 699.00,
        "items": [
            {
                "id": "test",
                "itemName": "test_name",
                "description": "testProduct",
                "quantity": 1,
                "imageUrl": "https://test-merchant.boxpay.tech/boxpay%20logo.svg",
                "amountWithoutTax": 699.00
            },
            {
                "id": "test2",
                "itemName": "test_name2",
                "description": "testProduct2",
                "quantity": 2,
                "imageUrl": "https://test-merchant.boxpay.tech/boxpay%20logo.svg",
                "amountWithoutTax": 499.00
            }
        ]
    },
    "statusNotifyUrl": "https://www.boxpay.tech",
    "frontendReturnUrl": "https://www.boxpay.tech",
    "frontendBackUrl": "https://www.boxpay.tech"
}""")
        var token = ""
        val request = object : JsonObjectRequest(Method.POST, url, jsonData,
            { response ->
                logJsonObject(response)
                val tokenFetched = response.getString("token")
                Log.d("token fetched", token)
                tokenLiveData.value = tokenFetched
                // Call a function that depends on the token

                // Use the token as needed
            },
            Response.ErrorListener { error ->
                // Handle error
                Log.e("Error", "Error occurred: ${error.message}")
                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
                    val errorResponse = String(error.networkResponse.data)
                    Log.e("Error", "Detailed error response: $errorResponse")
                    Log.d("","")
                }
            }) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] =  "Bearer afcGgCv6mOVIIpnFPWBL44RRciVU8oMteV5ZhC2nwjjjuw8z0obKMjdK8ShcwLOU6uRNjQryLKl1pLAsLAXSI"
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


}