package com.boxpay.checkout.sdk.ViewModels

class CallbackForDismissMainSheet(val dismissFunction : () -> Unit) {
    fun dismissFunctionPrivate(){
        dismissFunction()
    }
}