package com.example.tray.paymentResult

class PaymentResultObject(private val resultFetched : String,private val transactionIdFetched : String, private val operationIdFetched : String) {
    var status : String ?= null
    var transactionId : String ?= null
    var operationId : String ?= null
    init {
        this.status = resultFetched
        this.transactionId = transactionIdFetched
        this.operationId = operationIdFetched
    }
}