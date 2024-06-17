package com.example.tray.ViewModels

import android.util.Log
import com.example.tray.paymentResult.PaymentResultObject

class CallBackFunctions(val onPaymentResult : (PaymentResultObject) -> Unit) {
    fun onPaymentResultPrivate(result : PaymentResultObject){
        Log.d("result for callback","checkingPurpose")
        onPaymentResult(result)
    }
}