package com.boxpay.checkout.sdk.dataclasses

data class SavedCard(
    val cardHolderName:String?,
    val cardIcon: String?,
    val cardNumber : String?,
    val instrumentationRef: String?
)

data class SavedRecommended(
    val logoUrl: String?,
    val displayValue : String?,
    val instrumentationRef: String?,
    val type: String?
)