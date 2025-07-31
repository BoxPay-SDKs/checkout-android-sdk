package com.boxpay.checkout.sdk.dataclasses

import android.widget.EditText
import android.widget.TextView

data class DeliveryAddressErrorHandlingData(
    val errorTextView: TextView,
    val editTextField: EditText,
    val defaultMessage: String
)