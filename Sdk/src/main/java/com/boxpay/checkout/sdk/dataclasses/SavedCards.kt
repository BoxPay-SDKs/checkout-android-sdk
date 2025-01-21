package com.boxpay.checkout.sdk.dataclasses

data class SavedCard(
    val cardHolderName:String?,
    val cardIcon: Int?,
    val cardNumber : String?,
    val instrumentationRef: String?
)
