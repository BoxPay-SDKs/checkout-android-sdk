package com.example.AndroidCheckOutSDK

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
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
    private var selectedEnvironment: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        ArrayAdapter.createFromResource(
            this,
            com.example.AndroidCheckOutSDK.R.array.environment_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            binding.environmentSpinner.adapter = adapter
        }

        binding.environmentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                // Handle the selection
                selectedEnvironment = parent?.getItemAtPosition(position).toString()
                Log.d("Environment Item",selectedEnvironment.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
                selectedEnvironment = null
            }
        }

        binding.button.setOnClickListener(){
//            makePaymentRequest(this,binding.editTextText.text.toString())

            if(selectedEnvironment == null){
                Toast.makeText(this, "Select the environment", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val token = binding.editTextText.text.toString()
            Log.d("token fetched from merchant details screen",token)
            binding.button.isEnabled = false
            binding.button.text = "Please Wait"
            Log.d("selectedEnvironment",selectedEnvironment.toString())
            val checkout = BoxPayCheckout(this,token,::onPaymentResult,true)
            checkout.display()
        }
    }

    fun onPaymentResult(result : PaymentResultObject){
        if(result.status == "Success"){
            Log.d("Payment Result = ",result.status.toString())
            binding.button.setText("Payment has been Completed. please use another token")
        }else{
            Log.d("Payment Result else condition = ",result.status.toString())
            binding.button.isEnabled = true
            binding.button.text = "Proceed"
        }
    }

//    private fun makePaymentRequest(context: Context,token : String){
//        val queue = Volley.newRequestQueue(context)
//        val url = "https://test-apis.boxpay.tech/v0/merchants/gZOlwkSlVe/sessions"
//        val jsonData = JSONObject("""{
//    "context": {
//        "countryCode": "IN",
//        "legalEntity": {
//            "code": "demo_merchant"
//        },
//        "orderId": "test12"
//    },
//    "paymentType": "S",
//    "money": {
//        "amount": "30",
//        "currencyCode": "INR"
//    },
//    "descriptor": {
//        "line1": "Some descriptor"
//    },
//    "billingAddress": {
//        "address1": "first address line",
//            "address2": "second address line",
//            "city": "Faridabad",
//            "state": "Haryana",
//            "countryCode": "IN",
//            "postalCode": "121004"
//    },
//    "shopper": {
//        "firstName": "test",
//        "lastName": "last",
//        "email": "test123@gmail.com",
//        "uniqueReference": "x123y",
//        "phoneNumber": "911234567890",
//        "deliveryAddress": {
//            "address1": "first line",
//        "address2": "second line",
//        "city": "Mumbai",
//        "state": "Maharashtra",
//        "countryCode": "IN",
//        "postalCode": "123456"
//        }
//    },
//    "order": {
//        "originalAmount":10,
//        "shippingAmount": 10,
//        "voucherCode": "VOUCHER",
//        "taxAmount": 10,
//        "totalAmountWithoutTax": 20,
//        "items": [
//            {
//                "id":"test",
//               "itemName":"test_name",
//               "description":"testProduct",
//               "quantity":1,
//               "manufacturer":null,
//               "brand":null,
//               "color":null,
//               "productUrl":null,
//               "imageUrl":"https://test-merchant.boxpay.tech/boxpay%20logo.svg",
//               "categories":null,
//               "amountWithoutTax":10,
//               "taxAmount":10,
//               "taxPercentage":null,
//               "discountedAmount":null,
//               "amountWithoutTaxLocale":"10",
//               "amountWithoutTaxLocaleFull":"10"
//            }
//        ]
//    },
//    "statusNotifyUrl": "https://www.boxpay.tech",
//    "frontendReturnUrl": "https://www.boxpay.tech",
//    "frontendBackUrl": "https://www.boxpay.tech"
//}""")
//        val request = object : JsonObjectRequest(Method.POST, url, jsonData,
//            { response ->
//                logJsonObject(response)
//                val tokenFetched = response.getString("token")
//                Log.d("token fetched", tokenFetched)
//                // Call a function that depends on the token
//
//                val checkout = BoxPayCheckout(this,tokenFetched,::onPaymentResult)
//                checkout.display()
//
//                binding.button.isEnabled = true
//                binding.button.setBackgroundResource(com.example.tray.R.drawable.button_bg)
//            },
//            Response.ErrorListener { error ->
//                // Handle error
//                Log.e("Error", "Error occurred: ${error.toString()}")
//                if (error is VolleyError && error.networkResponse != null && error.networkResponse.data != null) {
//                    val errorResponse = String(error.networkResponse.data)
//                    Log.e("Error", "Detailed error response: $errorResponse")
//                    Log.d("","")
//                }
//            }) {
//            override fun getHeaders(): Map<String, String> {
//                val headers = HashMap<String, String>()
//                headers["Content-Type"] = "application/json"
//                headers["Authorization"] =  "Bearer XyUQOoLDgHlgxAojYhY22ev4P6icr94XIMkxrISZFQnAZIOueM4WbFAWGDc0Q6jPcWBkCXfXWpvRlHoQ5fl20d"
//                return headers
//            }
//        }
//        queue.add(request)
//    }

    fun logJsonObject(jsonObject: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonStr = gson.toJson(jsonObject)
        Log.d("Request Body Check", jsonStr)
    }
}