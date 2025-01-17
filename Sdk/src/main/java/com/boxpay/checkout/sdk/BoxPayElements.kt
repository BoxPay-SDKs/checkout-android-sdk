package com.boxpay.checkout.sdk

import android.content.Context
import android.widget.Toast
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.boxpay.checkout.sdk.utils.ConfigurationOptions
import com.boxpay.checkout.sdk.utils.handleException

class BoxPayElements(
    private val token: String,
    private val onPaymentResult: ((PaymentResultObject) -> Unit),
    private val paymentMethods: List<String>,
    private val configurationOptions: Map<ConfigurationOptions, Any>? = null
) {
    private var testEnv:Boolean = false
    private var context: Context? = null
    private var proceedButtonVisibility: Boolean = false
    private var handleCardValidity: ((Boolean) -> Unit)? = null
    private var handleUpiValidity: ((Boolean) -> Unit)? = null
    private var upiLayout : Int? = null
    private var cardLayout : Int? = null
    private lateinit var boxPayUpiComponent: BoxPayUpiComponent
    private var currentlyExpandedComponent: String? = null
    private lateinit var boxPayCardComponent: BoxPayCardComponent

    fun setContext(context: Context) {
        this.context = context
    }

    fun setTestEnv(testEnv: Boolean) {
        this.testEnv = testEnv
    }

    fun setProceedButtonVisibility(visible: Boolean) {
        this.proceedButtonVisibility = visible
    }

    fun setCardValidityCallback(handleCardValidityCallback:(Boolean) -> Unit) {
        this.handleCardValidity = handleCardValidityCallback
    }

    fun setUpiValidityCallback(handleUpiValidityCallback:(Boolean) -> Unit) {
        this.handleUpiValidity = handleUpiValidityCallback
    }

    fun setUPILayoutId(layout: Int) {
        this.upiLayout = layout
    }

    fun setCardLayoutId(layout: Int) {
        this.cardLayout = layout
    }

    private fun initiateUpiPayment() {
        if (paymentMethods.contains("upi")) {
            boxPayUpiComponent.onProceedPayment()
        } else {
            Toast.makeText(
                context,
                "Please enable UPI payment method",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun initiatePayment() {
        if ( currentlyExpandedComponent == "upi") {
            initiateUpiPayment()
        } else {
            initiateCardPayment()
        }
    }

    private fun initiateCardPayment() {
        if (paymentMethods.contains("card")) {
            boxPayCardComponent.onClickProceed()
        } else {
            Toast.makeText(
                context,
                "Please enable card payment method",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    fun showPaymentMethods() {
        var sessionUrl = ""
        try {
            sessionUrl = if (configurationOptions?.get(ConfigurationOptions.ENABLE_SANDBOX_ENV) == true) {
                "sandbox-apis.boxpay.tech"
            } else if (testEnv) {
                "test-apis.boxpay.tech"
            } else {
                "apis.boxpay.in"
            }

            if (paymentMethods.contains("upi")) {
                currentlyExpandedComponent = "upi"
                boxPayUpiComponent = BoxPayUpiComponent(token, onPaymentResult)
                boxPayUpiComponent.setContext(context!!)
                boxPayUpiComponent.setProceedButtonVisibility(proceedButtonVisibility, handleUpiValidity)
                boxPayUpiComponent.displayUpiComponent(sessionUrl, upiLayout!!)
            }

            if (paymentMethods.contains("card")) {
                currentlyExpandedComponent = "card"
                boxPayCardComponent = BoxPayCardComponent(token,onPaymentResult)
                boxPayCardComponent.setContext(context!!)
                boxPayCardComponent.setProceedButtonVisibility(proceedButtonVisibility, handleCardValidity)
                boxPayCardComponent.displayCardComponent(sessionUrl, cardLayout!!)
            }

            if (paymentMethods.contains("upi") && paymentMethods.contains("card")) {
                currentlyExpandedComponent = "upi"
                boxPayUpiComponent.setVisibilityFunction(::onUpiVisibilityCallback)
                boxPayCardComponent.setVisibilityFunction(::onCardVisibilityCallback)
            }

            if (paymentMethods.isEmpty() || paymentMethods.any { it != "upi" && it != "card" }) {
                Toast.makeText(
                    context,
                    "Payment method list is invalid. Please pass only \"upi\" or \"card\" in the list.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {
            handleException(context!!, e.message ?: "", token ,sessionUrl , "Boxpayelements")
        }
    }

    private fun onUpiVisibilityCallback() {
        currentlyExpandedComponent = "upi"
        boxPayCardComponent.onClickUpiComponent()
    }

    private fun onCardVisibilityCallback() {
        currentlyExpandedComponent = "card"
        boxPayUpiComponent.onClickCardComponent()
    }
}