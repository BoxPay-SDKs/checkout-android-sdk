package com.example.tray

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.tray.databinding.ActivityCheckBinding
import com.example.tray.paymentResult.PaymentResultObject
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
//        val bottomSheet = QuickPayBottomSheet()
//        bottomSheet.show(supportFragmentManager,"QuickPayTesting")

        makePaymentRequest(this)

        binding.textView6.text = "Generating Token Please wait..."
        successScreenFullReferencePath = "com.example.AndroidCheckOutSDK.SuccessScreen"
        tokenLiveData.observe(this, Observer { tokenInObserve ->
            // Handle the response after the token has been updated
            if(tokenInObserve != null) {
                handleResponseWithToken()
                binding.textView6.text = "Opening"
                binding.openButton.isEnabled = false
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
    fun removeLoadingAndEnabledProceedButton(){
        binding.openButton.isEnabled = true
        binding.progressBar.visibility = View.GONE
        Log.d("text will be updated here","here")
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
        if(tokenFetchedAndOpen)
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


    fun onPaymentResultCallback(result : PaymentResultObject) {
        Log.d("Result for the activity", "Payment result received: onpr ${result.status} onpr ${result.transactionId}  onpr ${result.operationId}")
    }


    private fun makePaymentRequest(context: Context){
        val queue = Volley.newRequestQueue(context)
        val url = "https://sandbox-apis.boxpay.tech/v0/merchants/kAqHOFwT4s/sessions"
        val jsonData = JSONObject(""" {
  "context": {
    "countryCode": "IN",
    "legalEntity": {"code": "demo"},
    "orderId": "test12"
  },
  "paymentType": "S",
  "money": {"amount": "1", "currencyCode": "INR"},
  "descriptor": {"line1": "Some descriptor"},
  "shopper": {
    "firstName": "Piyush",
    "lastName": "Sharma",
    "email":"piyush141001@gmail.com",
    "uniqueReference": "x123y",
    "phoneNumber": "919711668479",
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
                val tokenFetched = response.getString("token")
                Log.d("token fetched", tokenFetched)
                tokenLiveData.value = tokenFetched
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
                headers["Authorization"] =  "Bearer hHS5VOOxvbaHxWlGXvGp42tgDgIWBqR7Gj8V3vQknDs8o7OUqurrHZAi7YfilgfBgO5I52Bi0iFjUuu2iFmbWk"
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