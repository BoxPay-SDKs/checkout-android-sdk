package com.boxpay.checkout.sdk.dataclasses

data class FetchPaymentMethodPostOffer(
    val id : String?,
    val type : String?,
    val brand : String?,
    val title : String?,
    val typeTitle : String?,
    val logoUrl : String?,
    val instrumentTypeValue : String?,
    val applicableOffers : List<GetInstantOffersResponse>?
)