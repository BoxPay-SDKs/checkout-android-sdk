package com.example.tray

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject


class Check : AppCompatActivity() {
    val tokenLiveData = MutableLiveData<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check)

        makePaymentRequest(this)
        val openBottomSheet = findViewById<Button>(R.id.openButton)


        tokenLiveData.observe(this, Observer { token ->
            // Handle the response after the token has been updated
            if(!token.isEmpty()) {
                handleResponseWithToken()
            }else{
                Log.d("token is empty","waiting")
            }
        })

        openBottomSheet.setOnClickListener {
//            val bottomSheet = MainBottomSheet()
            showBottomSheetWithOverlay()
        }
    }

    fun showBottomSheetWithOverlay() {
        // Show the BottomSheetDialogFragment
        val bottomSheetFragment = MainBottomSheet.newInstance(tokenLiveData.value)
        bottomSheetFragment.show(supportFragmentManager, "YourBottomSheetTag")
    }
    fun makePaymentRequest(context: Context){
        val queue = Volley.newRequestQueue(context)
        val url = "https://test-apis.boxpay.tech/v0/merchants/gZztXPf8Ag/sessions"
        Log.d("makePaymentRequest","success")
        val jsonData = JSONObject("""{
        "context": {
            "countryCode": "IN",
            "legalEntity": {
                "code": "demo_merchant"
            },
            "orderId": "test12"
        },
        "paymentType": "A",
        "money": {
            "amount": "1",
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
            "phoneNumber": "",
            "deliveryAddress": {
                "address1": "first address line",
                "address2": "second address line",
                "city": "Faridabad",
                "state": "Haryana",
                "countryCode": "IN",
                "postalCode": "121004"
            }
        },
        "order": {
            "originalAmount" : 1697,
            "shippingAmount" : 500,
            "voucherCode" : "VOUCHER",
            "items": [
                {
                    "id": "test",
                    "itemName" : "test_name",
                    "description": "testProduct",
                    "quantity": 1,
                    "imageUrl" : "https://test-merchant.boxpay.tech/boxpay logo.svg",
                    "amountWithoutTax" : 699.00
                },
                {
                    "id": "test2",
                    "itemName" : "test_name2",
                    "description": "testProduct2",
                    "quantity": 2,
                    "imageUrl" : "https://test-merchant.boxpay.tech/boxpay logo.svg",
                    "amountWithoutTax" : 499.00
                }
            ]
        },
        "frontendReturnUrl": "https://www.boxpay.tech",
        "frontendBackUrl": "https://www.boxpay.tech",
        "expiryDurationSec": 7200
    }""")


        var token = ""

//        val credentials = "your_username:your_password"
//        val base64Credentials = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
//        val headers = HashMap<String, String>()
//        headers["Content-Type"] = "application/json"
//        headers["Authorization"] = "Basic $base64Credentials"

        val request = object : JsonObjectRequest(Method.POST, url, jsonData,
            { response ->
                val tokenFetched = response.getString("token")
                Log.d("token fetched", token)
                tokenLiveData.value = tokenFetched
                // Call a function that depends on the token

                // Use the token as needed
            },
            { error ->
                Log.e("Error occurredabc", error.toString())
                if (error.networkResponse != null) {
                    Log.d("Status code", error.networkResponse.statusCode.toString())
                    Log.d("Response data", error.networkResponse.data.toString(Charsets.UTF_8))
                }
            }) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] =  "Bearer 6Zioq6AqRt0gdF6PVr08ynfQWmWIDshSRmNGnzKuSgqXEj9xPwoDFGjF895CGBv4FFt3z740l4CY4uZ2xv9HR"
                return headers
            }
        }
        queue.add(request)


    }
    fun handleResponseWithToken() {
        // Use the token as needed
        // For example:
        Log.d("Token", "Token has been updated. Using token: ${tokenLiveData.value}")
        showBottomSheetWithOverlay()
    }
}
