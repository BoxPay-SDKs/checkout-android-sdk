package com.example.tray.paymentResult

class PaymentResultObject(private val resultFetched : String) {
    var status : String ?= null
    init {
        this.status = resultFetched
    }
}