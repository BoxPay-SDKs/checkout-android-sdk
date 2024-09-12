package com.boxpay.checkout.sdk

import org.json.JSONObject


interface HyperPaymentsCallbackAdapter {
    fun onEvent(jsonObject: JSONObject)
}