package com.boxpay.checkout.sdk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.boxpay.checkout.sdk.databinding.ActivityCheckBinding
import org.json.JSONObject

class CheckoutActivity : AppCompatActivity() {
    private lateinit var hyperServiceHolder: HyperServiceHolder

    private val binding: ActivityCheckBinding by lazy {
        ActivityCheckBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        hyperServiceHolder = HyperServiceHolder(this)
        hyperServiceHolder.setCallback(createHyperPaymentsCallbackAdapter())
       binding.proceedButtonRelativeLayout.setOnClickListener {
           val jsonData = JSONObject(
               """
                {
      "requestId": "037dc2f3-0df7-4450-9397-07ead52d84d2",
      "service": "in.juspay.hyperpay",
      "payload": {
          "clientId": "yourClientId",
          "amount": "1.0",
          "merchantId": "yourMerchantId",
          "clientAuthToken": null,
          "clientAuthTokenExpiry": "2022-03-12T20:29:23Z",
          "environment": "production",
          "options.getUpiDeepLinks": "true",
          "lastName": "wick",
          "action": "paymentPage",
          "customerId": "testing-customer-one",
          "returnUrl": "https://shop.merchant.com",
          "currency": "INR",
          "firstName": "John",
          "customerPhone": "9876543210",
          "customerEmail": "test@mail.com",
          "orderId": "testing-order-one",
          "description": "Complete your payment"
      }
  }
               """
           )
           hyperServiceHolder.setBoxPayTextEnv(true, false)
           hyperServiceHolder.process(jsonData)
       }
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
}