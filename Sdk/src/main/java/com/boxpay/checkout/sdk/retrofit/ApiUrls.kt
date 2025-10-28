package com.boxpay.checkout.sdk.retrofit

object ApiUrls {
    const val DCC = "v0/checkout/sessions/{token}/dcc/quotations"
    const val CREATE_SESSION = "v0/checkout/sessions/{token}"
    const val GET_INSTANT_OFFERS = "v0/checkout/sessions/{token}/offers/search"
    const val APPLY_INSTANT_OFFER = "v0/checkout/sessions/{token}/offers/evaluate"
    const val GET_SURCHARGE = "v0/checkout/sessions/{token}/surcharges/evaluate"
}