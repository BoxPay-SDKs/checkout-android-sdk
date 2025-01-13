package com.boxpay.checkout.sdk

import android.content.Context
import android.widget.Toast
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.boxpay.checkout.sdk.utils.ConfigurationOptions
import com.boxpay.checkout.sdk.utils.handleException

class BoxPayElements(
    val token: String,
    val onPaymentResult: ((PaymentResultObject) -> Unit),
    val configurationOptions: Map<ConfigurationOptions, Any>? = null
) {
    private var testEnv:Boolean = true
    private var context: Context? = null
    private var proceedButtonVisibility: Boolean = false
    private var handleCardValidity: ((Boolean) -> Unit)? = null
    private var upiLayout : Int? = null
    private var cardLayout : Int? = null
    private lateinit var boxPayUpiComponent: BoxPayUpiComponent
    private lateinit var boxPayCardComponent: BoxPayCardComponent

    fun setContext(context: Context) {
        this.context = context
    }

    fun setProceedButtonVisibility(visible: Boolean) {
        this.proceedButtonVisibility = visible
    }

    fun setCardValidityCallback(handleCardValidityCallback:(Boolean) -> Unit) {
        this.handleCardValidity = handleCardValidityCallback
    }

    fun setUPILayoutId(layout: Int) {
        this.upiLayout = layout
    }

    fun setCardLayoutId(layout: Int) {
        this.cardLayout = layout
    }

    fun initiateUpiPayment() {
        if (configurationOptions?.get(ConfigurationOptions.SHOW_UPI_METHOD_ALONE) == true) {
            boxPayUpiComponent.onProceedPayment()
        } else {
            Toast.makeText(
                context,
                "Please enable UPI payment method",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun initiateCardPayment() {
        if (configurationOptions?.get(ConfigurationOptions.SHOW_CARD_METHOD_ALONE) == true) {
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

            if (configurationOptions?.get(ConfigurationOptions.SHOW_UPI_METHOD_ALONE) == true) {
                boxPayUpiComponent = BoxPayUpiComponent(token, configurationOptions?.get(ConfigurationOptions.ENABLE_SANDBOX_ENV) == true, onPaymentResult)
                boxPayUpiComponent.setContext(context!!)
                boxPayUpiComponent.setProceedButtonVisibility(proceedButtonVisibility)
                boxPayUpiComponent.displayUpiComponent(sessionUrl, upiLayout!!)
            }

            if (configurationOptions?.get(ConfigurationOptions.SHOW_CARD_METHOD_ALONE) == true) {
                boxPayCardComponent = BoxPayCardComponent(token, configurationOptions?.get(ConfigurationOptions.ENABLE_SANDBOX_ENV) == true, onPaymentResult)
                boxPayCardComponent.setContext(context!!)
                boxPayCardComponent.setProceedButtonVisibility(proceedButtonVisibility, handleCardValidity)
                boxPayCardComponent.displayCardComponent(sessionUrl, cardLayout!!)
            }
        } catch (e: Exception) {
            handleException(context!!, e.message ?: "", token ?: "",sessionUrl , "Boxpayelements")
        }
    }
}