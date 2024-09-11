package com.boxpay.checkout.sdk

import android.content.Context
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import org.json.JSONObject

class HyperServiceHolder(private val context: Context) {

    private var callback: HyperPaymentsCallbackAdapter? = null
    private var token: String? = null
    private var customerToken: String? = null
    private lateinit var checkout: BoxPayOrderCheckout
    private var sandbox: Boolean = false
    private var testEnv : Boolean = false

    // Method to set the callback
    fun setCallback(callback: HyperPaymentsCallbackAdapter) {
        this.callback = callback
    }

    fun process(jsonObject: JSONObject,token: String, customerToken: String) {
        this.token = token
        this.customerToken = customerToken
        checkout = BoxPayOrderCheckout(
            context, token, ::handlePaymentResult, sandbox,customerToken, jsonObject
        )
        if (this.testEnv) {
            checkout.testEnv = true
        }
        checkout.display()
    }

    fun setBoxPayTextEnv(test: Boolean, sandbox: Boolean) {
        this.testEnv = test
        this.sandbox = sandbox
    }

    private fun handlePaymentResult(paymentResult: PaymentResultObject) {
        println("Payment Result: $paymentResult")
        callback?.onEvent(JSONObject().apply {
            put("event", "process_result")
            put("payload", JSONObject().apply {
                put("status", paymentResult.status)
                // Add other fields as needed
            })
        })
    }
}
