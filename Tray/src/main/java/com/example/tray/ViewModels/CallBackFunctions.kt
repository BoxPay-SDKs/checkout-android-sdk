package com.example.tray.ViewModels

import android.util.Log

class CallBackFunctions(val onPaymentResult : (String) -> Unit) {
    fun onPaymentResultPrivate(result : String){
        Log.d("result for callback","checkingPurpose")
       onPaymentResult(result)
    }
}