package com.example.tray.ViewModels

import android.util.Log
import com.example.tray.paymentResult.PaymentResultObject

class CallbackForDismissMainSheet(val dismissFunction : () -> Unit) {
    fun dismissFunctionPrivate(){
        dismissFunction()
    }
}