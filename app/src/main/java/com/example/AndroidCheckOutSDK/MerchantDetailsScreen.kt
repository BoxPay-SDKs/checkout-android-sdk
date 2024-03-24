package com.example.AndroidCheckOutSDK

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.AndroidCheckOutSDK.databinding.ActivityMerchantDetailsScreenBinding
import com.example.tray.BoxPayCheckout
import com.example.tray.paymentResult.PaymentResultObject
import com.google.gson.GsonBuilder
import org.json.JSONObject

class MerchantDetailsScreen : AppCompatActivity() {
    private val binding : ActivityMerchantDetailsScreenBinding by lazy {
        ActivityMerchantDetailsScreenBinding.inflate(layoutInflater)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.button.setOnClickListener(){
            binding.button.isEnabled = false
            binding.button.setBackgroundColor(Color.parseColor("#787676"))
            makePaymentRequest(this,binding.editTextText.text.toString(),binding.editTextText2.text.toString())
        }
    }
    private fun makePaymentRequest(context: Context,merchantId : String, apiKey : String){
        val queue = Volley.newRequestQueue(context)
        val url = "https://test-apis.boxpay.tech/v0/merchants/gZOlwkSlVe/sessions"
        val jsonData = JSONObject("""{
    "context": {
        "countryCode": "IN",
        "legalEntity": {
            "code": "demo_merchant"
        },
        "orderId": "test12"
    },
    "paymentType": "S",
    "money": {
        "amount": "30",
        "currencyCode": "INR"
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
        "originalAmount":10,
        "shippingAmount": 10,
        "voucherCode": "VOUCHER",
        "taxAmount": 10,
        "totalAmountWithoutTax": 20,
        "items": [
            {
                "id":"test",
               "itemName":"test_name",
               "description":"testProduct",
               "quantity":1,
               "manufacturer":null,
               "brand":null,
               "color":null,
               "productUrl":null,
               "imageUrl":"https://test-merchant.boxpay.tech/boxpay%20logo.svg",
               "categories":null,
               "amountWithoutTax":10,
               "taxAmount":10,
               "taxPercentage":null,
               "discountedAmount":null,
               "amountWithoutTaxLocale":"10",
               "amountWithoutTaxLocaleFull":"10"
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
                val tokenFetched = response.getString("token")
                Log.d("token fetched", tokenFetched)
                // Call a function that depends on the token

                val checkout = BoxPayCheckout(this,tokenFetched,::onPaymentResult)
                checkout.display()

                binding.button.isEnabled = true
                binding.button.setBackgroundResource(com.example.tray.R.drawable.button_bg)
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
                headers["Authorization"] =  "Bearer XyUQOoLDgHlgxAojYhY22ev4P6icr94XIMkxrISZFQnAZIOueM4WbFAWGDc0Q6jPcWBkCXfXWpvRlHoQ5fl20d"
                return headers
            }
        }
        queue.add(request)
    }
    fun onPaymentResult(result : PaymentResultObject){
        Log.d("onPaymentResult",result.result.toString())
    }
    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body Check", jsonStr)
    }
}