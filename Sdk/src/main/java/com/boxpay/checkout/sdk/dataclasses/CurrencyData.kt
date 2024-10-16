package com.boxpay.checkout.sdk.dataclasses

data class CurrencyData(
    val currencyCode: String,
    val currencyName: String,
    val countryCode: String,
    val countryName: String,
    val flag: String
)
