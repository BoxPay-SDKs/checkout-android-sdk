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
import com.boxpay.checkout.demoapp.databinding.ActivityCheckBinding
import com.boxpay.checkout.sdk.BoxPayCheckout
import com.boxpay.checkout.sdk.BoxPayUpiComponent
import com.boxpay.checkout.sdk.BuildConfig
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import org.json.JSONObject

class Check : AppCompatActivity() {
    val tokenLiveData = MutableLiveData<String>()
    var customerShopperToken: String? = null
    private var successScreenFullReferencePath: String? = null
    private var tokenFetchedAndOpen = false
    private var isUpiAlone: Boolean = false


    private val binding: ActivityCheckBinding by lazy {
        ActivityCheckBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        makePaymentRequest(this)
        val bundle = intent.extras
        isUpiAlone = bundle?.getBoolean("isUpiAlone",false) ?: false

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
        if (isUpiAlone) {
            val boxPayUpiComponent = BoxPayUpiComponent(tokenLiveData.value ?: "", false, ::onPaymentResultCallback)
            boxPayUpiComponent.setTestEnv(true)
            boxPayUpiComponent.setContext(this)
            binding.proceedButtonBottom.visibility = View.VISIBLE
            boxPayUpiComponent.setProceedButtonVisibility(false)

            // Replace a container in your activity's layout
            binding.openButton.removeAllViews()
            supportFragmentManager.beginTransaction()
                .replace(R.id.openButton,boxPayUpiComponent)
                .commit()

            binding.proceedButtonBottom.setOnClickListener {
                boxPayUpiComponent.onClickProceed()
                binding.proceedButtonBottom.isEnabled = false
            }
        } else {
            val boxPayCheckout =
                BoxPayCheckout(
                    context = this,
                    token = tokenLiveData.value ?: "",
                    onPaymentResult = ::onPaymentResultCallback,
                    sandboxEnabled = false,
                    customerShopperToken = customerShopperToken ?: "",
                    isSuccessScreenVisible = true,
                    extraParams = hashMapOf("loadQrDirect" to false)
                )
            boxPayCheckout.testEnv = true
            boxPayCheckout.display()
        }
    }


    fun onPaymentResultCallback(result: PaymentResultObject) {
        Toast.makeText(this, result.status, Toast.LENGTH_SHORT).show()
        binding.proceedButtonBottom.isEnabled = true
    }


    private fun makePaymentRequest(context: Context) {
        val queue = Volley.newRequestQueue(context)
        val url = "https://test-apis.boxpay.tech/v0/merchants/lGfqzNSKKA/sessions"
        val jsonData = JSONObject(
            """ {
  "context" : {
    "countryCode" : "IN",
    "legalEntity" : {
      "code" : "razorpay"
    },
    "orderId" : "test12"
  },
  "paymentType" : "S",
  "money" : {
    "amount" : "6500",
    "currencyCode" : "INR"
  },
  "descriptor" : {
    "line1" : "Some descriptor"
  },
  "shopper": {
            "firstName": "Ankush",
            "lastName": "Kashyap",
            "gender": null,
            "phoneNumber": "917777777777",
            "email": "ankush.kashyap@boxpay.tech",
            "uniqueReference": "x123y",
            "deliveryAddress": {
                "address1": "first line",
                "address2": "second line",
                "address3": null,
                "city": "Chandigarh",
                "state": "Chandigarh",
                "countryCode": "IN",
                "postalCode": "160002",
                "shopperRef": null,
                "addressRef": null,
                "labelType": "Other",
                "labelName": null,
                "name": null,
                "email": null,
                "phoneNumber": null
            },
            "dateOfBirth": "2023-07-17T12:34:56Z",
            "panNumber": "CTGPA2222D"
        },
  "order" : {
    "originalAmount" : 500,
    "shippingAmount" : 50,
    "voucherCode" : "VOUCHER",
    "taxAmount" :100,
    "totalAmountWithoutTax" : 550,
    "items" : [ {
      "id" : "test",
      "itemName" : "La Fille Regular Solid Handheld Bag Blue",
      "description" : "testProduct",
      "quantity" : 1,
      "manufacturer" : null,
      "brand" : null,
      "color" : null,
      "productUrl" : null,
      "imageUrl" : "https://assetscdn1.paytm.com/images/catalog/product/B/BA/BAGLAFILLE-BLUEINTO887307A255D05/1563381583133_0..jpg",
      "categories" : null,
      "amountWithoutTax" : 500,
      "taxAmount" : 76.27,
      "taxPercentage" : null,
      "discountedAmount" : null,
      "amountWithoutTaxLocale" : "10",
      "amountWithoutTaxLocaleFull" : "10"
    }]
  },
  "statusNotifyUrl" : "https://www.boxpay.tech",
  "frontendReturnUrl" : "https://www.boxpay.tech",
  "frontendBackUrl" : "https://www.boxpay.tech",
  "createShopperToken" : false,
  "expiryDurationSec" : 900
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
                tokenLiveData.value = tokenFetched
                editor.putString("baseUrl", "test-apis.boxpay.tech")
                editor.putString("token", tokenLiveData.value)
                editor.apply()
                // Call a function that depends on the token
            },
            Response.ErrorListener {
            }) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] =
                    "Bearer 3z3G6PT8vDhxQCKRQzmRsujsO5xtsQAYLUR3zcKrPwVrphfAqfyS20bvvCg2X95APJsT5UeeS5YdD41aHbz6mg"
                headers["X-Client-Connector-Name"] = "Android SDK"
                headers["X-Client-Connector-Version"] = BuildConfig.SDK_VERSION
                return headers
            }
        }
        queue.add(request)
    }

    fun extractMessageFromErrorResponse(response: String): String? {
        try {
            // Parse the JSON string
            val jsonObject = JSONObject(response)
            // Retrieve the value associated with the "message" key
            return jsonObject.getString("message")
        } catch (e: Exception) {
            // Handle JSON parsing exception
        }
        return null
    }
}