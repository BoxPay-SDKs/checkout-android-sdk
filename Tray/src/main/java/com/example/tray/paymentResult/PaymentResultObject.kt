package com.example.tray.paymentResult

class PaymentResultObject(private val resultFetched : String) {
    var result : String ?= null
    init {
        this.result = resultFetched
    }
}