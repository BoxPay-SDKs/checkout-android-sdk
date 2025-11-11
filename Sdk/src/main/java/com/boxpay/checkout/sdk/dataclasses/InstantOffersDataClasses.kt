package com.boxpay.checkout.sdk.dataclasses

data class GetInstantOffersRequest(
    val type : String,
    val currency : String,
    val minAmount : Int,
    val maxAmount : Int
)

data class GetInstantOffersResponse(
    val title : String?,
    val description: String?,
    val terms : String?,
    val type : String?,
    val code : String?,
    val discount : OfferDiscount?,
    val enabled : Boolean?,
    val visibleOnCheckout : Boolean?,
    val criteria : Criteria?
)

data class OfferDiscount(
    val percentage : Int?,
    val amount : Int?,
    val type : String?,
    val maxAmount : Int?
)

data class Criteria(
    val startDate : String?,
    val endDate : String?,
    val minMoney : MinMoney?,
    val applicableTo : Applicable?
)

data class MinMoney(
    val amount : Int?,
    val currencyCode : String?
)

data class Applicable(
    val paymentMethods : List<OfferPaymentMethod>?
)

data class OfferPaymentMethod(
    val type : String?,
    val issuer : String?
)


data class ApplyInstantOfferRequest(
    val offerSearchRequest : OfferSearchRequest
)

data class OfferSearchRequest(
    val currency : String,
    val minAmount : Int,
    val maxAmount: Int,
    val offers : List<String>
)


data class ApplyInstantOfferResponse(
    val originalAmount : Int?,
    val currency : String?,
    val evaluatedOffers : List<EvaluatedOffers>?,
    val finalAmount : Int?
)

data class EvaluatedOffers(
    val title : String?,
    val description : String?,
    val terms : String?,
    val type : String?,
    val code : String?,
    val discount : OfferDiscount?,
    val appliedDiscountAmount : Int?
)