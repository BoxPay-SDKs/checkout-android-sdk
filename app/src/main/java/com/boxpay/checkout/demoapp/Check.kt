package com.boxpay.checkout.demoapp

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
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
import com.boxpay.checkout.sdk.BoxPayElements
import com.boxpay.checkout.sdk.BuildConfig
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.boxpay.checkout.sdk.utils.ConfigurationOptions
import org.json.JSONObject

class Check : AppCompatActivity() {
    val tokenLiveData = MutableLiveData<String>()
    var customerShopperToken: String? = null
    private var successScreenFullReferencePath: String? = null
    private var tokenFetchedAndOpen = false
    private var isUpiAlone: Boolean = false
    private var isCardAlone : Boolean = false


    private val binding: ActivityCheckBinding by lazy {
        ActivityCheckBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        makePaymentRequest(this)
        val bundle = intent.extras
        isUpiAlone = bundle?.getBoolean("isUpiAlone", false) ?: false
        isCardAlone = bundle?.getBoolean("isCardAlone",false) ?: false

        binding.textView6.text = "Generating Token Please wait..."
        successScreenFullReferencePath = "com.example.AndroidCheckOutSDK.SuccessScreen"
        tokenLiveData.observe(this, Observer { tokenInObserve ->
            // Handle the response after the token has been updated
            if (tokenInObserve != null) {
                handleResponseWithToken()
                binding.textView6.text = "Opening"
            }
        })
    }

    private fun handleResponseWithToken() {
        if (tokenFetchedAndOpen) return
        showBottomSheetWithOverlay()
        tokenFetchedAndOpen = true
    }

    private fun showBottomSheetWithOverlay() {
        if (isUpiAlone || isCardAlone) {
            val paymentMethod = if (isUpiAlone && isCardAlone) {
                listOf("upi","card")
            } else if (isUpiAlone) {
                listOf("upi")
            } else {
                listOf("card")
            }
            val boxPayElements = BoxPayElements(
                tokenLiveData.value ?: "",
                ::onPaymentResultCallback,
                paymentMethod,
            )
            boxPayElements.setContext(this)
            boxPayElements.setTestEnv(true)
            boxPayElements.setUPILayoutId(R.id.upiOpenButon)
            boxPayElements.setCardLayoutId(R.id.cardOpenButton)
            binding.cardOpenButton.removeAllViews()
            binding.upiOpenButon.removeAllViews()
            boxPayElements.setProceedButtonVisibility(false)
            boxPayElements.setCardValidityCallback(::handleCardValidity)
            boxPayElements.setUpiValidityCallback(::handleUpiValidity)
            boxPayElements.showPaymentMethods()
            disableProceedButton()

            binding.proceedButtonBottom.visibility = View.VISIBLE
            binding.proceedButtonBottom.setOnClickListener {
                boxPayElements.initiatePayment()
            }
        } else {
            val boxPayCheckout =
                BoxPayCheckout(
                    context = this,
                    token = tokenLiveData.value ?: "",
                    onPaymentResult = ::onPaymentResultCallback,
                    customerShopperToken = customerShopperToken ?: "",
                    configurationOptions = mapOf(
                        ConfigurationOptions.SHOW_UPI_QR_ON_LOAD to false,
                        ConfigurationOptions.ENABLE_SANDBOX_ENV to false,
                        ConfigurationOptions.SHOW_BOXPAY_SUCCESS_SCREEN to true
                    )
                )
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
    "amount" : "65000",
    "currencyCode" : "INR"
  },
  "descriptor" : {
    "line1" : "Some descriptor"
  },
  "shopper": {
            "firstName": "Ankush",
            "lastName": "Kashyap",
            "gender": null,
            "phoneNumber": "+917777777777",
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
                "labelName": "test",
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
  "createShopperToken" : true,
  "expiryDurationSec" : 900
}"""
        )

        val request = object : JsonObjectRequest(Method.POST, url, jsonData, { response ->
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
        }, Response.ErrorListener {}) {
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

    private fun enableProceedButton() {
        binding.bottomProceedButtonLayout.isEnabled = true
        binding.proceedButtonBottom.isEnabled = true
        binding.bottomProceedButtonLayout.setBackgroundResource(com.boxpay.checkout.sdk.R.drawable.button_bg)
        binding.bottomProceedButtonText.setTextColor(
            Color.parseColor(
                "#FFFFFF"
            )
        )
    }

    private fun disableProceedButton() {
        binding.bottomProceedButtonText.visibility = View.VISIBLE
        binding.proceedButtonBottom.isEnabled = false
        binding.bottomProceedButtonLayout.setBackgroundResource(com.boxpay.checkout.sdk.R.drawable.disable_button)
        binding.proceedButtonBottom.setBackgroundResource(com.boxpay.checkout.sdk.R.drawable.disable_button)
        binding.bottomProceedButtonText.setTextColor(Color.parseColor("#ADACB0"))
    }

    private fun handleUpiValidity(valid: Boolean) {
        if (valid) {
            enableProceedButton()
        } else {
            disableProceedButton()
        }
    }

    private fun handleCardValidity(valid: Boolean) {
        if (valid) {
            enableProceedButton()
        } else {
            disableProceedButton()
        }
    }
}