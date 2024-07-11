package com.boxpay.checkout.sdk.ViewModels

import android.util.Log
import com.boxpay.checkout.sdk.paymentResult.PaymentResultObject

class CallBackFunctions(val onPaymentResult : (PaymentResultObject) -> Unit) {
    fun onPaymentResultPrivate(result : PaymentResultObject){
        Log.d("result for callback","checkingPurpose")
        onPaymentResult(result)
    }
}