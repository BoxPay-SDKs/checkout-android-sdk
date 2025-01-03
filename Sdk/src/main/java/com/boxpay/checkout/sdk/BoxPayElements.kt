package com.boxpay.checkout.sdk

import android.content.Context
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject
import com.boxpay.checkout.sdk.utils.ConfigurationOptions

class BoxPayElements(
    val token: String,
    val onPaymentResult: ((PaymentResultObject) -> Unit),
    val configurationOptions: Map<ConfigurationOptions, Any>? = null
) {
    private var testEnv:Boolean = true
    private var context: Context? = null
    private var proceedButtonVisibility: Boolean = false
    private var handleCardValidity: ((Boolean) -> Unit)? = null

    fun setContext(context: Context) {
        this.context = context
    }

    fun setProceedButtonVisibility(visible: Boolean) {
        this.proceedButtonVisibility = visible
    }

    fun setCardValidityCallback(handleCardValidityCallback:(Boolean) -> Unit) {
        this.handleCardValidity = handleCardValidityCallback
    }

    fun showPaymentMethods() {
        val sessionUrl = if (configurationOptions?.get(ConfigurationOptions.ENABLE_SANDBOX_ENV) == true) {
            "sandbox-apis.boxpay.tech"
        } else if (testEnv) {
            "test-apis.boxpay.tech"
        } else {
            "apis.boxpay.in"
        }

        if (configurationOptions?.get(ConfigurationOptions.SHOW_UPI_METHOD) == true) {
            val boxPayUpiComponent = BoxPayUpiComponent(token, configurationOptions?.get(ConfigurationOptions.ENABLE_SANDBOX_ENV) == true, onPaymentResult)
            boxPayUpiComponent.setContext(context!!)
            boxPayUpiComponent.setProceedButtonVisibility(proceedButtonVisibility)
            boxPayUpiComponent.displayUpiComponent(sessionUrl)
        }

        if (configurationOptions?.get(ConfigurationOptions.SHOW_CARD_METHOD) == true) {
            val boxPayCardComponent = BoxPayCardComponent(token, configurationOptions?.get(ConfigurationOptions.ENABLE_SANDBOX_ENV) == true, onPaymentResult)
            boxPayCardComponent.setContext(context!!)
            boxPayCardComponent.setProceedButtonVisibility(proceedButtonVisibility, handleCardValidity)
            boxPayCardComponent.displayCardComponent(sessionUrl)
        }
    }
}