package com.example.tray.ViewModels

import com.example.tray.paymentResult.PaymentResultObject

class CallBackFunctions(val onPaymentResult : (PaymentResultObject) -> Unit) {
    fun onPaymentResultPrivate(result : PaymentResultObject){
        onPaymentResult(result)
    }
}