package com.boxpay.checkout.sdk

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.boxpay.checkout.sdk.databinding.ActivityCheckBinding
import org.json.JSONObject

class Check : AppCompatActivity() {
    val tokenLiveData = MutableLiveData<String>()
    var customerShopperToken: String? = null
    private var successScreenFullReferencePath: String? = null
    private var tokenFetchedAndOpen = false
    private lateinit var hyperServiceHolder: HyperServiceHolder


    private val binding: ActivityCheckBinding by lazy {
        ActivityCheckBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        makePaymentRequest(this)

        binding.textView6.text = "Generating Token Please wait..."
        hyperServiceHolder = HyperServiceHolder(this)
        hyperServiceHolder.setCallback(createHyperPaymentsCallbackAdapter())
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
        val orderJson = JSONObject(
            """
                {
    "action": "paymentPage",
    "merchantId": "railyatri_sandbox",
    "clientId": "railyatripp",
    "orderId": "1725133659jpp1988719986",
    "amount": "2180",
    "toolbarSecondLine": "NZM - BDTS | 2024-08-21",
    "customerId": "cth_udBM2B34PMgSZXjX",
    "customerEmail": "avinash.singh@railyatri.in",
    "customerMobile": "9555681381",
    "orderDetails": "{\"metadata.webhook_url\":\"https://payment-test.railyatri.in/api/juspay/update-payment\",\"udf1\":\"30313407\",\"udf2\":\"4\",\"return_url\":\"https://payment-test.railyatri.in/payment/juspay/process\",\"order_id\":\"1725133659jpp1988719986\",\"merchant_id\":\"railyatri_sandbox\",\"amount\":\"2180.0\",\"timestamp\":\"1725133662\",\"customer_id\":\"cth_udBM2B34PMgSZXjX\",\"currency\":\"INR\",\"customer_email\":\"avinash.singh@railyatri.in\",\"customer_phone\":\"9555681381\",\"metadata.merchant_container_list\":\"[{\\\"payment_method\\\":\\\"RY_CASH\\\",\\\"payment_method_type\\\":\\\"MERCHANT_CONTAINER\\\",\\\"display_name\\\":\\\"RY CASH\\\",\\\"eligible_amount\\\":10,\\\"balance_amount\\\":400,\\\"walletIconURL\\\":\\\"https://cdn-icons-png.flaticon.com/512/216/216490.png\\\"},{\\\"payment_method\\\":\\\"RY_CASH_PLUS\\\",\\\"payment_method_type\\\":\\\"MERCHANT_CONTAINER\\\",\\\"display_name\\\":\\\"RY CASH PLUS\\\",\\\"eligible_amount\\\":400,\\\"balance_amount\\\":400,\\\"walletIconURL\\\":\\\"https://cdn-icons-png.flaticon.com/512/216/216490.png\\\"}]\"}",
    "product_summary": "[[{\"type\":\"text\",\"text\":\"Updated Status:\",\"textSize\":14,\"color\":\"#000000\"},{\"type\":\"text\",\"text\":\"50 WAITLIST\",\"textSize\":16,\"fontType\":\"Bold\",\"color\":\"#00B829\"},{\"type\":\"linegap\",\"gap\":0}],[{\"type\":\"linegap\",\"gap\":4},{\"type\":\"text\",\"text\":\"High confirmation chance tickets are likely to get confirmed!\",\"textSize\":12,\"color\":\"#333333\"}],[{\"type\":\"divider\",\"thickness\":1,\"color\":\"#F0F7FD\"}],[{\"type\":\"text\",\"text\":\"Review Ticket\",\"textSize\":15,\"fontType\":\"Bold\",\"color\":\"#000000\"},{\"type\":\"space\",\"width\":10,\"weight\":1},{\"type\":\"text\",\"text\":\"Booking ID: 30313407\",\"textSize\":11,\"fontType\":\"SemiBold\",\"color\":\"#333333\"}],[{\"type\":\"divider\",\"thickness\":2,\"color\":\"#ffffff\"}],[{\"type\":\"divider\",\"thickness\":1,\"color\":\"#F0F7FD\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"linegap\",\"gap\":0},{\"type\":\"text\",\"text\":\"12910 - Garib Rath Express\",\"textSize\":14,\"fontType\":\"Bold\",\"color\":\"#333333\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"linegap\",\"gap\":4},{\"type\":\"text\",\"text\":\"2 Travellers| Class- 3A| Quota- GN\",\"textSize\":9,\"fontType\":\"Bold\",\"color\":\"#333333\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"linegap\",\"gap\":4},{\"type\":\"text\",\"text\":\"Saturday, 30 Sep\",\"textSize\":10,\"color\":\"#333333\"},{\"type\":\"space\",\"width\":50,\"weight\":1},{\"type\":\"text\",\"text\":\"Saturday, 30 Sep\",\"textSize\":10,\"color\":\"#333333\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"linegap\",\"gap\":0},{\"type\":\"text\",\"text\":\"NZM , 16:30\",\"textSize\":18,\"fontType\":\"Bold\",\"color\":\"#333333\"},{\"type\":\"space\",\"width\":10,\"weight\":1},{\"type\":\"image\",\"url\":\"https://images.railyatri.in/ry_images_prod/train-icon-1715841210.png\",\"size\":30},{\"type\":\"space\",\"width\":10,\"weight\":1},{\"type\":\"text\",\"text\":\"09:15, BDTS \",\"textSize\":18,\"fontType\":\"Bold\",\"color\":\"#333333\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"linegap\",\"gap\":0},{\"type\":\"text\",\"text\":\"DELHI HAZRAT NIZAMUDDIN\",\"textSize\":10,\"color\":\"#333333\"},{\"type\":\"space\",\"width\":10,\"weight\":1},{\"type\":\"text\",\"text\":\"---- 16:45 h ----\",\"textSize\":10,\"color\":\"#888888\"},{\"type\":\"space\",\"width\":10,\"weight\":1},{\"type\":\"text\",\"text\":\" MUMBAI BANDRA TERMINUS\",\"textSize\":10,\"color\":\"#333333\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"divider\",\"thickness\":1,\"color\":\"#F0F7FD\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"accordion\",\"limit\":1,\"content\":[[{\"type\":\"text\",\"text\":\"Passenger Details & Fare Breakup\",\"textSize\":15,\"fontType\":\"Bold\",\"color\":\"#333333\"},{\"type\":\"space\",\"width\":10,\"weight\":1},{\"type\":\"toggleImage\",\"openIcon\":\"https://assets.juspay.in/hyper/images/internalPP/ic_arrow_down.png\",\"closeIcon\":\"https://assets.juspay.in/hyper/images/internalPP/ic_arrow_up.png\",\"size\":20}],[{\"type\":\"text\",\"text\":\"1. Test (Male)\",\"textSize\":14,\"color\":\"#333333\"},{\"type\":\"text\",\"text\":\"25 \",\"textSize\":12,\"color\":\"#888888\"},{\"type\":\"text\",\"text\":\"| Lower Berth | \",\"textSize\":12,\"color\":\"#333333\"},{\"type\":\"text\",\"text\":\"2. Test (Male)\",\"textSize\":14,\"color\":\"#333333\"},{\"type\":\"text\",\"text\":\"30 \",\"textSize\":12,\"color\":\"#888888\"},{\"type\":\"text\",\"text\":\"| Lower Berth | \",\"textSize\":12,\"color\":\"#333333\"}],[{\"type\":\"divider\",\"thickness\":1,\"color\":\"#E5E5E5\"}],[{\"type\":\"text\",\"text\":\"Fare Breakup\",\"textSize\":14,\"fontType\":\"Bold\",\"color\":\"#000000\"},{\"type\":\"space\",\"width\":10,\"weight\":1}],[{\"type\":\"text\",\"text\":\"Ticket Base Fare\",\"textSize\":12,\"color\":\"#333333\"},{\"type\":\"space\",\"width\":50,\"weight\":1},{\"type\":\"text\",\"text\":\"₹2180.0\",\"textSize\":12,\"color\":\"#333333\"}],[{\"type\":\"text\",\"text\":\"Agent Service Charge\",\"textSize\":12,\"color\":\"#333333\"},{\"type\":\"space\",\"width\":50,\"weight\":1},{\"type\":\"text\",\"text\":\"₹40.0\",\"textSize\":12,\"color\":\"#333333\"}],[{\"type\":\"text\",\"text\":\"IRCTC Conv. Fee\",\"textSize\":12,\"color\":\"#333333\"},{\"type\":\"space\",\"width\":50,\"weight\":1},{\"type\":\"text\",\"text\":\"₹35.4\",\"textSize\":12,\"color\":\"#333333\"}],[{\"type\":\"text\",\"text\":\"Net Amount payable\",\"textSize\":14,\"fontType\":\"Bold\",\"color\":\"#000000\"},{\"type\":\"space\",\"width\":50,\"weight\":1},{\"type\":\"text\",\"text\":\"₹2304\",\"textSize\":14,\"fontType\":\"Bold\",\"color\":\"#000000\"},{\"type\":\"background\",\"color\":\"#F0F7FD\"}],[{\"type\":\"space\",\"width\":1,\"weight\":2}]]}]]",
    "signature": "aSUaLK6cvlPgMptvU7QqF+Asdg4p5+ZuYekFCriW98ijgsXNrYwWG19aQEwOJ3lhZFGmGdcUR9jV27fVdGktzH2N0eAO88yxMp/tmopbEbObJOypXdvPtlRUW6FNS0YAfz6RYWZJ7DB0ZNciuQ83Zei8s7d4UzPB41kKy4lwi9DqtQHAQOQUl9eSEqhY80VAcAd40X0/Tuf2p8X4/vhi6r0oaie1w2acJWCnxOrErLk3Z2W1vuw2B28aurBEQPS9qDOhn9Q49DSwIwg7nAP1XkE2szh1ybvcrUpRzDgdNU7ozLxSAyVpEyH15C9rNYsIuxEyiZbHFk9dr0gLbPqlPw==",
    "merchantKeyId": "13031",
    "language": "English"
}
               """
        )
        hyperServiceHolder.setBoxPayTextEnv(true, false)
        hyperServiceHolder.process(orderJson, tokenLiveData.value ?: "", customerShopperToken ?: "")
    }


    private fun createHyperPaymentsCallbackAdapter(): HyperPaymentsCallbackAdapter {
        return object : HyperPaymentsCallbackAdapter {
            override fun onEvent(jsonObject: JSONObject) {
                println("jsonObject>>> $jsonObject")
                try {
                    val event = jsonObject.getString("event")
                    if (event == "hide_loader") {
                        // Hide Loader
                    } else if (event == "process_result") {
                        val error = jsonObject.optBoolean("error")
                        val innerPayload = jsonObject.optJSONObject("payload")
                        val status = innerPayload.optString("status")
//                        val redirect = Intent(context, ResponsePage::class.java)

                        if (!error) {
                            when (status) {
                                "charged" -> {
//                                    redirect.putExtra("status", "OrderSuccess")
//                                    context.startActivity(redirect)
                                }

                                "cod_initiated" -> {
//                                    redirect.putExtra("status", "CODInitiated")
//                                    context.startActivity(redirect)
                                }
                            }
                        } else {
                            when (status) {
                                "backpressed" -> {

                                }

                                "user_aborted" -> {
//                                    redirect.putExtra("status", "UserAborted")
//                                    context.startActivity(redirect)
                                }

                                "pending_vbv" -> {
//                                    redirect.putExtra("status", "PendingVBV")
//                                    context.startActivity(redirect)
                                }

                                "authorizing" -> {
//                                    redirect.putExtra("status", "Authorizing")
//                                    context.startActivity(redirect)
                                }

                                "authorization_failed" -> {
//                                    redirect.putExtra("status", "AuthorizationFailed")
//                                    context.startActivity(redirect)
                                }

                                "authentication_failed" -> {
//                                    redirect.putExtra("status", "AuthenticationFailed")
//                                    context.startActivity(redirect)
                                }

                                "api_failure" -> {
//                                    redirect.putExtra("status", "APIFailure")
//                                    context.startActivity(redirect)
                                }

                                else -> {
//                                    redirect.putExtra("status", "APIFailure")
//                                    context.startActivity(redirect)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {

                }
            }
        }
    }


    private fun makePaymentRequest(context: Context) {
        val queue = Volley.newRequestQueue(context)
        val url = "https://test-apis.boxpay.tech/v0/merchants/lGfqzNSKKA/sessions"
        val jsonData = JSONObject(
            """ {
  "context": {
    "countryCode": "IN",
    "legalEntity": {"code": "razorpay"},
    "orderId": "test12"
  },
  "paymentType": "S",
  "money": {"amount": "100", "currencyCode": "INR"},
  "descriptor": {"line1": "Some descriptor"},
  "shopper": {
        "firstName": "Ishika cnsjbc cnbhsbc jbcydsbc bcydbc",
        "lastName": "Bansal",
        "email": "ishika.bansal@boxpay.tech",
        "uniqueReference": "x123y",
        "phoneNumber": "919876543210",
        "deliveryAddress": {
            "address1": "first line",
            "address2": "second line",
            "city": "New Delhi",
            "state": "Delhi",
            "countryCode": "IN",
            "postalCode": "147147"
        }
    },
  "order": {
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
      },
      {
        "id": "test",
        "itemName": "item no 2",
        "description": "testProduct",
        "quantity": 3,
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
  "frontendBackUrl": "https://www.boxpay.tech",
  "createShopperToken":true
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
            Response.ErrorListener { /* no response handling */ }) {
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
}