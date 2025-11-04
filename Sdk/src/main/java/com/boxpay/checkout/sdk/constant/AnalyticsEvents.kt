package com.boxpay.checkout.sdk.constant

object AnalyticsEvents {

    // Event attributes
    const val PAYMENT_TYPE  = "paymentType"
    const val PAYMENT_SUB_TYPE = "paymentSubType"
    const val CALLER_TOKEN= "callerToken"
    const val UI_EVENT ="uiEvent"
    const val UPI_INTENT_ERROR="upiIntentError"

    // uiEvent
    const val ADDRESS_UPDATED = "ADDRESS_UPDATED"
    const val PAYMENT_CATEGORY_SELECTED = "PAYMENT_CATEGORY_SELECTED"
    const val PAYMENT_METHOD_SELECTED = "PAYMENT_METHOD_SELECTED"
    const val PAYMENT_INITIATED = "PAYMENT_INITIATED"
    const val CHECKOUT_LOADED = "CHECKOUT_LOADED"
    const val PAYMENT_INSTRUMENT_PROVIDED = "PAYMENT_INSTRUMENT_PROVIDED"
    const val UPI_APP_NOT_FOUND = "UPI_APP_NOT_FOUND"
    const val FAILED_TO_LAUNCH_UPI_INTENT = "FAILED_TO_LAUNCH_UPI_INTENT"
    const val ERROR_GETTING_UPI_URL = "ERROR_GETTING_UPI_URL"
    const val SDK_CRASH = "SDK_CRASH"
    const val PAYMENT_RESULT_SCREEN_DISPLAYED = "PAYMENT_RESULT_SCREEN_DISPLAYED"
}