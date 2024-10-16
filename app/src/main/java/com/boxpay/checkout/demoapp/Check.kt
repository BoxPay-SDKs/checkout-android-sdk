package com.boxpay.checkout.demoapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.BoxPayCheckout
import com.boxpay.checkout.sdk.BuildConfig
import com.boxpay.checkout.sdk.databinding.ActivityCheckBinding
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import org.json.JSONObject

class Check : AppCompatActivity() {
    val tokenLiveData = MutableLiveData<String>()
    var customerShopperToken: String? = null
    private var successScreenFullReferencePath: String? = null
    private var tokenFetchedAndOpen = false


    private val binding: ActivityCheckBinding by lazy {
        ActivityCheckBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        makePaymentRequest(this)

        binding.textView6.text = "Generating Token Please wait..."
        successScreenFullReferencePath = "com.example.AndroidCheckOutSDK.SuccessScreen"
        tokenLiveData.observe(this, Observer { tokenInObserve ->
            // Handle the response after the token has been updated
            if (tokenInObserve != null) {
                handleResponseWithToken()
                binding.textView6.text = "Opening"
                binding.openButton.isEnabled = false
            }
        })

        var actionInProgress = false
        binding.openButton.setOnClickListener() {

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
                actionInProgress = false
                binding.openButton.isEnabled = true
            }
        }
    }

    private fun handleResponseWithToken() {
        if (tokenFetchedAndOpen)
            return
        showBottomSheetWithOverlay()
        tokenFetchedAndOpen = true
    }

    private fun showBottomSheetWithOverlay() {
        val boxPayCheckout =
            BoxPayCheckout(
                this,
                tokenLiveData.value ?: "",
                ::onPaymentResultCallback,
                false,
                customerShopperToken = customerShopperToken ?: ""
            )
        boxPayCheckout.testEnv = true
        boxPayCheckout.display()
    }


    fun onPaymentResultCallback(result: PaymentResultObject) {
        Toast.makeText(this, result.status, Toast.LENGTH_SHORT).show()
    }


    private fun makePaymentRequest(context: Context) {
        val queue = Volley.newRequestQueue(context)
        val url = "https://test-apis.boxpay.tech/v0/merchants/oh3mnorsME/sessions"
        val jsonData = JSONObject(
            """ {
    "context": {
        "countryCode": "US",
        "legalEntity": {
            "code": "dcc_usd"
        },
        "orderId": "test12"
    },
    "paymentType": "S",
    "money": {
        "amount": "1234",
        "currencyCode": "USD"
    },
    "descriptor": {
        "line1": "Some descriptor"
    },
    "shopper": {
        "firstName": "Ankush",
        "lastName": "Kashyap",
        "email": "ankush.kashyap@boxpay.tech",
        "uniqueReference": "x123y",
        "phoneNumber": "917986361129",
        "deliveryAddress": {
            "address1": "first line",
            "address2": "second line",
            "city": "Chandigarh",
            "state": "Chandigarh",
            "countryCode": "IN",
            "postalCode": "160002"
        },
         "dateOfBirth": "2023-07-17T12:34:56Z",
         "panNumber": "CTGPA2222D"
    
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
                "imageUrl": "https://www.kasandbox.org/programming-images/avatars/old-spice-man.png",
                "categories": null,
                "amountWithoutTax": 423.73,
                "taxAmount": 76.27,
                "taxPercentage": null,
                "discountedAmount": null,
                "amountWithoutTaxLocale": "10",
                "amountWithoutTaxLocaleFull": "10"
            },
            {
                "id": "test",
                "itemName": "Sample Item",
                "description": "testProduct",
                "quantity": 1,
                "manufacturer": null,
                "brand": null,
                "color": null,
                "productUrl": null,
                "imageUrl": "https://www.kasandbox.org/programming-images/avatars/old-spice-man.png",
                "categories": null,
                "amountWithoutTax": 423.73,
                "taxAmount": 76.27,
                "taxPercentage": null,
                "discountedAmount": null,
                "amountWithoutTaxLocale": "10",
                "amountWithoutTaxLocaleFull": "10"
            },
            {
                "id": "test",
                "itemName": "Sample Item",
                "description": "testProduct",
                "quantity": 1,
                "manufacturer": null,
                "brand": null,
                "color": null,
                "productUrl": null,
                "imageUrl": "https://www.kasandbox.org/programming-images/avatars/old-spice-man.png",
                "categories": null,
                "amountWithoutTax": 423.73,
                "taxAmount": 76.27,
                "taxPercentage": null,
                "discountedAmount": null,
                "amountWithoutTaxLocale": "10",
                "amountWithoutTaxLocaleFull": "10"
            },
            {
                "id": "test",
                "itemName": "Sample Item",
                "description": "testProduct",
                "quantity": 1,
                "manufacturer": null,
                "brand": null,
                "color": null,
                "productUrl": null,
                "imageUrl": "https://www.kasandbox.org/programming-images/avatars/old-spice-man.png",
                "categories": null,
                "amountWithoutTax": 423.73,
                "taxAmount": 76.27,
                "taxPercentage": null,
                "discountedAmount": null,
                "amountWithoutTaxLocale": "10",
                "amountWithoutTaxLocaleFull": "10"
            },
            {
                "id": "test",
                "itemName": "Sample Item",
                "description": "testProduct",
                "quantity": 1,
                "manufacturer": null,
                "brand": null,
                "color": null,
                "productUrl": null,
                "imageUrl": "https://www.kasandbox.org/programming-images/avatars/old-spice-man.png",
                "categories": null,
                "amountWithoutTax": 423.73,
                "taxAmount": 76.27,
                "taxPercentage": null,
                "discountedAmount": null,
                "amountWithoutTaxLocale": "10",
                "amountWithoutTaxLocaleFull": "10"
            },
            {
                "id": "test",
                "itemName": "Sample Item",
                "description": "testProduct",
                "quantity": 1,
                "manufacturer": null,
                "brand": null,
                "color": null,
                "productUrl": null,
                "imageUrl": "https://www.kasandbox.org/programming-images/avatars/old-spice-man.png",
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
    "frontendBackUrl": "https://www.boxpay.tech",
    "createShopperToken":false,
    "expiryDurationSec":900
}"""
        )

        val request = object : JsonObjectRequest(Method.POST, url, jsonData,
            { response ->
                val sharedPreferences =
                    this.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
                val editor: SharedPreferences.Editor = sharedPreferences.edit()
                val tokenFetched = response.getString("token")
                val payload = response.optJSONObject("payload")
                customerShopperToken = payload?.optString("shopper_token", "")
                println("======customerShopperToken $tokenFetched")
                tokenLiveData.value = tokenFetched
                editor.putString("baseUrl", "test-apis.boxpay.tech")
                editor.putString("token", tokenLiveData.value)
                editor.apply()
                // Call a function that depends on the token
            },
            Response.ErrorListener { error ->
                println("====error $error")
                /* no response handling */
            }) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] =
                    "Bearer i8zuZD3mR9SYvT29z3p4DHRigXBcL5Cu5H2Lpl5M9w1LP7BVqj79YE09vhrskbXTbJjtZ5HsLFfivNjtdCZZZk"
                headers["X-Client-Connector-Name"] = "Android SDK"
                headers["X-Client-Connector-Version"] = BuildConfig.SDK_VERSION
                return headers
            }
        }
        queue.add(request)
    }
}
