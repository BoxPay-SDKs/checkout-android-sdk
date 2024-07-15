package com.boxpay.checkout.sdk.ViewModels

import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject

class CallBackFunctions(val onPaymentResult : (PaymentResultObject) -> Unit) {
    fun onPaymentResultPrivate(result : PaymentResultObject){
        onPaymentResult(result)
    }
}