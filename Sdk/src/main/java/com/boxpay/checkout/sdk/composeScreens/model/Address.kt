package com.boxpay.checkout.sdk.composeScreens.model

data class Address(
    val address1:  String?,
    val address2: String?,
    val city:  String?,
    val state : String?,
    val countryCode :String?,
    val postalCode : String?,
    val lastUsed: Any?,
    val addressRef: String?,
    val labelType: String?,
    val labelName: String?,
    val name: String?,
    val email: String?,
    val phoneNumber:String?,
    val shopperRef: String?
)